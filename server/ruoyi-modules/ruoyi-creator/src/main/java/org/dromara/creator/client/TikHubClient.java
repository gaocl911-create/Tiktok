package org.dromara.creator.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.creator.config.TikHubProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal TikHub API client for Douyin creator/content monitoring.
 */
public class TikHubClient {

    public static final String SEC_USER_ID_ENDPOINT = "/api/v1/douyin/web/get_sec_user_id";
    public static final String PROFILE_ENDPOINT = "/api/v1/douyin/web/handler_user_profile";
    public static final String USER_POSTS_ENDPOINT = "/api/v1/douyin/app/v3/fetch_user_post_videos";
    public static final String ONE_VIDEO_ENDPOINT = "/api/v1/douyin/app/v3/fetch_one_video_v3";
    public static final String ONE_VIDEO_BY_SHARE_URL_ENDPOINT = "/api/v1/douyin/app/v3/fetch_one_video_by_share_url";
    public static final String WEB_ONE_VIDEO_BY_SHARE_URL_ENDPOINT = "/api/v1/douyin/web/fetch_one_video_by_share_url";

    private final TikHubProperties properties;
    private final BigDecimal spentTodayUsd;
    private final List<TikHubCallLogDraft> callLogs = new ArrayList<>();

    @Getter
    private int requestCount;
    @Getter
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;

    public TikHubClient(TikHubProperties properties, BigDecimal spentTodayUsd) {
        this.properties = properties;
        this.spentTodayUsd = spentTodayUsd == null ? BigDecimal.ZERO : spentTodayUsd;
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            throw new ServiceException("TikHub is disabled. Set TIKHUB_ENABLED=true first.");
        }
        if (StringUtils.isBlank(properties.getApiToken())) {
            throw new ServiceException("TIKHUB_API_TOKEN is not configured.");
        }
    }

    public JsonNode getSecUserId(String profileUrl) {
        return get(SEC_USER_ID_ENDPOINT, Map.of("url", profileUrl));
    }

    public JsonNode getProfile(String secUserId) {
        return get(PROFILE_ENDPOINT, Map.of("sec_user_id", secUserId));
    }

    public JsonNode getUserPosts(String secUserId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sec_user_id", secUserId);
        params.put("max_cursor", 0);
        params.put("count", 20);
        params.put("sort_type", 0);
        return get(USER_POSTS_ENDPOINT, params);
    }

    public JsonNode getOneVideo(String awemeId) {
        return get(ONE_VIDEO_ENDPOINT, Map.of("aweme_id", awemeId));
    }

    public JsonNode getOneVideoByShareUrl(String shareUrl) {
        ServiceException firstError;
        try {
            return get(ONE_VIDEO_BY_SHARE_URL_ENDPOINT, Map.of("share_url", shareUrl));
        } catch (ServiceException ex) {
            firstError = ex;
        }

        sleepBeforeRetry();
        try {
            return get(ONE_VIDEO_BY_SHARE_URL_ENDPOINT, Map.of("share_url", shareUrl));
        } catch (ServiceException ignored) {
            sleepBeforeRetry();
        }

        try {
            return get(WEB_ONE_VIDEO_BY_SHARE_URL_ENDPOINT, Map.of("share_url", shareUrl));
        } catch (ServiceException fallbackError) {
            throw new ServiceException(
                "TikHub 无法解析该抖音链接，App 接口重试和 Web 备用接口均失败。首次错误：{}；备用接口错误：{}",
                firstError.getMessage(),
                fallbackError.getMessage()
            );
        }
    }

    public List<TikHubCallLogDraft> getCallLogs() {
        return List.copyOf(callLogs);
    }

    private JsonNode get(String endpoint, Map<String, Object> params) {
        checkBudget(endpoint);
        String url = buildUrl(endpoint);
        long started = System.currentTimeMillis();
        TikHubCallLogDraft log = new TikHubCallLogDraft();
        log.setEndpoint(endpoint);
        log.setRequestMethod("GET");
        log.setRequestKey(requestKey(params));
        log.setCalledAt(new Date());
        log.setUnitPriceUsd(unitPriceUsd());
        log.setUnitPriceCny(unitPriceCny());
        log.setEstimatedCostCny(unitPriceCny());
        HttpRequest request = HttpRequest.get(url)
            .header("Authorization", "Bearer " + properties.getApiToken())
            .header("Accept", "application/json")
            .header("User-Agent", "CreatorMonitor/1.0")
            .timeout(properties.getTimeoutSeconds() * 1000);
        applyParams(request, params);
        try (HttpResponse response = request.execute()) {
            log.setHttpStatus(response.getStatus());
            log.setLatencyMs((int) (System.currentTimeMillis() - started));
            if (!response.isOk()) {
                String responseBody = response.body();
                log.setSuccess(false);
                log.setErrorMessage(truncate(responseBody));
                callLogs.add(log);
                String providerMessage = extractErrorMessage(responseBody);
                throw new ServiceException(
                    "TikHub HTTP {} at {}{}",
                    response.getStatus(),
                    endpoint,
                    StringUtils.isBlank(providerMessage) ? "" : "：" + providerMessage
                );
            }
            JsonNode root = JsonUtils.getObjectMapper().readTree(response.body());
            String code = TikHubJsonSupport.findText(root, "code", "status_code", "statusCode");
            log.setProviderCode(code);
            if (!isSuccessCode(code)) {
                String message = TikHubJsonSupport.findText(root, "message", "msg", "error", "detail");
                log.setSuccess(false);
                log.setErrorMessage(truncate(message));
                callLogs.add(log);
                throw new ServiceException("TikHub API error at {}: {}", endpoint, message == null ? code : message);
            }
            recordSuccess();
            log.setSuccess(true);
            callLogs.add(log);
            return root;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.setLatencyMs((int) (System.currentTimeMillis() - started));
            log.setSuccess(false);
            log.setErrorMessage(truncate(e.getMessage()));
            callLogs.add(log);
            throw new ServiceException("TikHub request failed at {}: {}", endpoint, e.getMessage());
        }
    }

    private void checkBudget(String endpoint) {
        BigDecimal budget = properties.getDailyBudgetUsd();
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal projected = spentTodayUsd.add(estimatedCostUsd).add(unitPriceUsd());
        if (projected.compareTo(budget) > 0) {
            throw new ServiceException("TikHub daily budget exceeded before calling {}", endpoint);
        }
    }

    private void recordSuccess() {
        requestCount++;
        estimatedCostUsd = estimatedCostUsd.add(unitPriceUsd());
    }

    private BigDecimal unitPriceUsd() {
        return properties.getEstimatedUnitPriceUsd() == null
            ? BigDecimal.ZERO
            : properties.getEstimatedUnitPriceUsd();
    }

    private BigDecimal unitPriceCny() {
        BigDecimal rate = properties.getUsdToCnyRate() == null
            ? BigDecimal.ZERO
            : properties.getUsdToCnyRate();
        return unitPriceUsd().multiply(rate).setScale(6, RoundingMode.HALF_UP);
    }

    private String buildUrl(String endpoint) {
        StringBuilder url = new StringBuilder(properties.getApiBaseUrl().replaceAll("/+$", ""));
        url.append(endpoint);
        return url.toString();
    }

    private void applyParams(HttpRequest request, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            request.form(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private String requestKey(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        Object key = params.get("aweme_id");
        if (key == null) {
            key = params.get("sec_user_id");
        }
        if (key == null) {
            key = params.get("share_url");
        }
        if (key == null) {
            key = params.get("url");
        }
        return key == null ? null : truncate(String.valueOf(key));
    }

    private boolean isSuccessCode(String code) {
        return code == null
            || "0".equals(code)
            || "200".equals(code)
            || "success".equalsIgnoreCase(code)
            || "ok".equalsIgnoreCase(code)
            || "true".equalsIgnoreCase(code);
    }

    private String extractErrorMessage(String responseBody) {
        if (StringUtils.isBlank(responseBody)) {
            return null;
        }
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(responseBody);
            return TikHubJsonSupport.findText(root, "message_zh", "message", "msg", "error", "detail");
        } catch (Exception ignored) {
            return truncate(responseBody);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException("TikHub retry was interrupted.");
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
