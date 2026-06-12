package org.dromara.creator.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.json.utils.JsonUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Defensive JSON helpers for third-party social API payloads.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TikHubJsonSupport {

    public static JsonNode unwrapData(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return root;
        }
        JsonNode node = root;
        for (String key : List.of("data", "result", "aweme_detail", "aweme", "item")) {
            JsonNode child = node.get(key);
            if (child != null && !child.isNull()) {
                node = child;
            }
        }
        return node;
    }

    public static String findText(JsonNode node, String... keys) {
        JsonNode found = findValue(node, Set.of(keys), 0);
        if (found == null || found.isNull()) {
            return null;
        }
        if (found.isTextual() || found.isNumber() || found.isBoolean()) {
            String value = found.asText();
            return value == null || value.isBlank() ? null : value;
        }
        if (found.isArray() && !found.isEmpty()) {
            return findText(found.get(0), keys);
        }
        return null;
    }

    public static BigInteger findBigInt(JsonNode node, String... keys) {
        String value = findText(node, keys);
        if (value == null) {
            return BigInteger.ZERO;
        }
        try {
            return new BigInteger(value);
        } catch (NumberFormatException ignored) {
            return BigInteger.ZERO;
        }
    }

    public static boolean hasAny(JsonNode node, String... keys) {
        return findValue(node, Set.of(keys), 0) != null;
    }

    public static Date findDate(JsonNode node, String... keys) {
        String value = findText(node, keys);
        if (value == null) {
            return null;
        }
        try {
            long timestamp = Long.parseLong(value);
            if (timestamp <= 0) {
                return null;
            }
            if (timestamp < 10_000_000_000L) {
                timestamp *= 1000;
            }
            return new Date(timestamp);
        } catch (NumberFormatException ignored) {
            try {
                return Date.from(java.time.Instant.parse(value));
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    public static JsonNode findObject(JsonNode node, String... keys) {
        JsonNode found = findValue(node, Set.of(keys), 0);
        if (found != null && found.isObject()) {
            return found;
        }
        return null;
    }

    public static List<JsonNode> findAwemeObjects(JsonNode node, int limit) {
        List<JsonNode> results = new ArrayList<>();
        collectAwemeObjects(node, results, limit, 0);
        return results;
    }

    public static String toJson(JsonNode node) {
        return node == null ? null : JsonUtils.toJsonString(node);
    }

    public static String findImageUrl(JsonNode node) {
        String direct = findText(node, "url", "uri", "avatar_url", "cover_url");
        if (direct != null && direct.startsWith("http")) {
            return direct;
        }
        JsonNode urlList = findValue(node, Set.of("url_list", "urlList"), 0);
        if (urlList != null && urlList.isArray()) {
            for (JsonNode item : urlList) {
                if (item.isTextual() && item.asText().startsWith("http")) {
                    return item.asText();
                }
            }
        }
        return null;
    }

    private static JsonNode findValue(JsonNode node, Set<String> keys, int depth) {
        if (node == null || node.isNull() || depth > 8) {
            return null;
        }
        if (node.isObject()) {
            for (String key : keys) {
                JsonNode direct = node.get(key);
                if (direct != null && !direct.isNull()) {
                    return direct;
                }
            }
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext()) {
                JsonNode found = findValue(values.next(), keys, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findValue(child, keys, depth + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void collectAwemeObjects(JsonNode node, List<JsonNode> results, int limit, int depth) {
        if (node == null || node.isNull() || depth > 8 || results.size() >= limit) {
            return;
        }
        if (node.isObject()) {
            if (looksLikeAweme(node)) {
                results.add(node);
                return;
            }
            Iterator<JsonNode> values = node.elements();
            while (values.hasNext() && results.size() < limit) {
                collectAwemeObjects(values.next(), results, limit, depth + 1);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                if (results.size() >= limit) {
                    return;
                }
                collectAwemeObjects(child, results, limit, depth + 1);
            }
        }
    }

    private static boolean looksLikeAweme(JsonNode node) {
        return node.has("aweme_id")
            || node.has("awemeId")
            || (node.has("statistics") && (node.has("desc") || node.has("create_time")));
    }
}
