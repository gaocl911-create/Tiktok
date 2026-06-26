package org.dromara.creator.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.dromara.common.core.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps TikHub Douyin JSON payloads into internal normalized profiles.
 */
public class TikHubDouyinMapper {

    private static final String UNTITLED_CONTENT = "\u6682\u65e0\u4f5c\u54c1\u6587\u6848";
    private static final String GENERIC_SHARE_DESCRIPTION =
        "\u5728\u6296\u97f3\uff0c\u8bb0\u5f55\u7f8e\u597d\u751f\u6d3b";

    public String mapSecUserId(JsonNode payload) {
        JsonNode data = TikHubJsonSupport.unwrapData(payload);
        String secUserId = TikHubJsonSupport.findText(data, "sec_user_id", "secUid", "sec_uid", "secUserId");
        if (secUserId == null && data != null && data.isTextual() && data.asText().startsWith("MS4w")) {
            secUserId = data.asText();
        }
        if (secUserId == null) {
            throw new ServiceException("TikHub did not return sec_user_id.");
        }
        return secUserId;
    }

    public TikHubCreatorProfile mapCreator(JsonNode payload, String fallbackSecUserId) {
        JsonNode data = TikHubJsonSupport.unwrapData(payload);
        JsonNode user = TikHubJsonSupport.findObject(data, "user", "user_info", "author");
        if (user == null) {
            user = data;
        }
        TikHubCreatorProfile profile = new TikHubCreatorProfile();
        String secUid = firstNotBlank(
            TikHubJsonSupport.findText(user, "sec_uid", "sec_user_id", "secUid", "secUserId"),
            fallbackSecUserId
        );
        profile.setPlatformCreatorId(secUid);
        profile.setPlatformUserId(TikHubJsonSupport.findText(user, "uid", "id", "user_id"));
        profile.setPlatformDisplayId(resolveDisplayId(user));
        profile.setNickname(firstNotBlank(TikHubJsonSupport.findText(user, "nickname", "name"), profile.getPlatformDisplayId(), profile.getPlatformUserId(), secUid));
        profile.setAvatarUrl(TikHubJsonSupport.findImageUrl(TikHubJsonSupport.findObject(user, "avatar_thumb", "avatar_medium", "avatar_larger", "avatar_url", "avatar")));
        profile.setSignature(TikHubJsonSupport.findText(user, "signature", "desc", "description", "bio"));
        profile.setHomepageUrl(secUid == null ? null : "https://www.douyin.com/user/" + secUid);
        profile.setIpLocation(TikHubJsonSupport.findText(user, "ip_location", "province", "city", "location"));
        profile.setGender(TikHubJsonSupport.findText(user, "gender"));
        profile.setFollowerCount(TikHubJsonSupport.findBigInt(user, "follower_count", "fans_count", "mplatform_followers_count"));
        profile.setFollowingCount(TikHubJsonSupport.findBigInt(user, "following_count", "follow_count"));
        profile.setTotalFavoritedCount(TikHubJsonSupport.findBigInt(user, "total_favorited", "favorited_count", "total_like_count", "like_count"));
        profile.setContentCount(TikHubJsonSupport.findBigInt(user, "aweme_count", "video_count", "content_count", "post_count").intValue());
        profile.setRawJson(TikHubJsonSupport.toJson(payload));
        return profile;
    }

    public TikHubContentProfile mapOneContent(JsonNode payload) {
        JsonNode data = TikHubJsonSupport.unwrapData(payload);
        assertContentAvailable(data);
        List<JsonNode> posts = TikHubJsonSupport.findAwemeObjects(data, 1);
        JsonNode item = posts.isEmpty() ? data : posts.get(0);
        assertContentAvailable(item);
        return mapContent(item, payload);
    }

    public List<TikHubContentProfile> mapContentList(JsonNode payload) {
        JsonNode data = TikHubJsonSupport.unwrapData(payload);
        List<TikHubContentProfile> profiles = new ArrayList<>();
        for (JsonNode item : TikHubJsonSupport.findAwemeObjects(data, 20)) {
            profiles.add(mapContent(item, item));
        }
        return profiles;
    }

    public TikHubContentProfile mapContent(JsonNode item, JsonNode rawPayload) {
        assertContentAvailable(item);
        String awemeId = firstNotBlank(TikHubJsonSupport.findText(item, "aweme_id", "awemeId", "id", "id_str"));
        if (awemeId == null) {
            throw new ServiceException("TikHub content payload is missing aweme_id.");
        }
        JsonNode statistics = TikHubJsonSupport.findObject(item, "statistics", "stats");
        JsonNode metricsRoot = statistics == null ? item : statistics;
        TikHubContentProfile profile = new TikHubContentProfile();
        profile.setPlatformContentId(awemeId);
        JsonNode shareInfo = item == null ? null : item.get("share_info");
        String description = firstNotBlank(
            directText(item, "desc", "caption", "description", "share_desc"),
            meaningfulShareDescription(directText(shareInfo, "share_desc", "share_desc_info"))
        );
        profile.setTitle(firstNotBlank(
            directText(item, "item_title", "preview_title", "title", "share_title"),
            firstLine(description),
            UNTITLED_CONTENT
        ));
        profile.setDescription(description);
        profile.setCoverUrl(resolveCoverUrl(item));
        String shareUrl = firstNotBlank(TikHubJsonSupport.findText(item, "share_url", "url", "content_url"), "https://www.douyin.com/video/" + awemeId);
        profile.setShareUrl(shareUrl);
        profile.setContentUrl(shareUrl);
        profile.setPublishTime(TikHubJsonSupport.findDate(item, "create_time", "publish_time", "published_at"));
        profile.setContentType(inferContentType(item));
        profile.setLikeCount(TikHubJsonSupport.findBigInt(metricsRoot, "digg_count", "like_count", "liked_count"));
        profile.setCommentCount(TikHubJsonSupport.findBigInt(metricsRoot, "comment_count", "comments_count"));
        profile.setCollectCount(TikHubJsonSupport.findBigInt(metricsRoot, "collect_count", "collects_count", "favorite_count", "favorites_count"));
        profile.setShareCount(TikHubJsonSupport.findBigInt(metricsRoot, "share_count", "share_num", "shares_count"));
        profile.setPlayCount(TikHubJsonSupport.findBigInt(metricsRoot, "play_count", "view_count", "play_times"));
        List<String> missing = missingMetrics(metricsRoot);
        profile.setMissingMetricFields(missing);
        profile.setMetricsStatus(missing.isEmpty() ? "success" : "partial");
        JsonNode author = TikHubJsonSupport.findObject(item, "author", "user", "user_info");
        if (author != null) {
            profile.setCreator(mapCreator(author, null));
        }
        profile.setRawJson(TikHubJsonSupport.toJson(rawPayload));
        return profile;
    }

    private void assertContentAvailable(JsonNode node) {
        JsonNode filterDetail = TikHubJsonSupport.findObject(node, "filter_detail");
        if (filterDetail == null) {
            return;
        }
        String reason = directText(filterDetail, "filter_reason", "reason");
        String notice = directText(filterDetail, "notice");
        String detail = directText(filterDetail, "detail_msg", "detail", "msg", "message");
        if (!isUnavailableFilterDetail(reason, notice, detail)) {
            return;
        }
        throw new ServiceException("TikHub content unavailable: {}",
            firstNotBlank(reason, detail, notice, "filter_detail"));
    }

    private boolean isUnavailableFilterDetail(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.contains("deleted")
                || normalized.contains("self_see")
                || normalized.contains("private")
                || normalized.contains("unavailable")
                || normalized.contains("status_")
                || value.contains("作品不见")
                || value.contains("已被删除")
                || value.contains("无法观看")
                || value.contains("不可见")
                || value.contains("权限")) {
                return true;
            }
        }
        return false;
    }

    private List<String> missingMetrics(JsonNode node) {
        List<String> missing = new ArrayList<>();
        if (!TikHubJsonSupport.hasAny(node, "digg_count", "like_count", "liked_count")) {
            missing.add("like_count");
        }
        if (!TikHubJsonSupport.hasAny(node, "comment_count", "comments_count")) {
            missing.add("comment_count");
        }
        if (!TikHubJsonSupport.hasAny(node, "collect_count", "collects_count", "favorite_count", "favorites_count")) {
            missing.add("collect_count");
        }
        if (!TikHubJsonSupport.hasAny(node, "share_count", "share_num", "shares_count")) {
            missing.add("share_count");
        }
        return missing;
    }

    private String inferContentType(JsonNode item) {
        JsonNode images = firstPresent(item, "images", "image_list", "image_infos", "original_images");
        if (images != null && images.isArray() && !images.isEmpty()) {
            return "image";
        }
        return "video";
    }

    private String resolveCoverUrl(JsonNode item) {
        JsonNode video = item == null ? null : item.get("video");
        if (video != null && video.isObject()) {
            for (String key : List.of("cover", "origin_cover", "dynamic_cover")) {
                String videoCover = TikHubJsonSupport.findImageUrl(video.get(key));
                if (videoCover != null) {
                    return videoCover;
                }
            }
        }

        JsonNode images = firstPresent(item, "images", "image_list", "image_infos", "original_images");
        String imageCover = TikHubJsonSupport.findImageUrl(images);
        if (imageCover != null) {
            return imageCover;
        }

        for (String key : List.of("cover", "video_cover", "origin_cover", "dynamic_cover")) {
            String directCover = TikHubJsonSupport.findImageUrl(item == null ? null : item.get(key));
            if (directCover != null) {
                return directCover;
            }
        }
        return null;
    }

    private JsonNode firstPresent(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String resolveDisplayId(JsonNode user) {
        return firstValidDisplayId(
            directText(user, "unique_id"),
            directText(user, "short_id"),
            directText(user, "douyin_id"),
            directText(user, "display_id"),
            directText(user, "account_id"),
            directText(user, "uid"),
            directText(user, "id"),
            directText(user, "user_id")
        );
    }

    private String firstValidDisplayId(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"0".equals(value)) {
                return value;
            }
        }
        return null;
    }

    private String directText(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && (value.isTextual() || value.isNumber() || value.isBoolean())) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int lineBreak = value.indexOf('\n');
        return lineBreak < 0 ? value : value.substring(0, lineBreak).trim();
    }

    private String meaningfulShareDescription(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (GENERIC_SHARE_DESCRIPTION.equals(normalized)) {
            return null;
        }
        return normalized;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
