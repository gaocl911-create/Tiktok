package org.dromara.creator.client;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * Normalized content profile from TikHub.
 */
@Data
public class TikHubContentProfile {

    private String platformContentId;
    private String contentType = "video";
    private String title;
    private String description;
    private String coverUrl;
    private String contentUrl;
    private String shareUrl;
    private Date publishTime;
    private BigInteger likeCount = BigInteger.ZERO;
    private BigInteger commentCount = BigInteger.ZERO;
    private BigInteger collectCount = BigInteger.ZERO;
    private BigInteger shareCount = BigInteger.ZERO;
    private BigInteger playCount = BigInteger.ZERO;
    private String metricsStatus = "success";
    private List<String> missingMetricFields = List.of();
    private TikHubCreatorProfile creator;
    private String rawJson;
}
