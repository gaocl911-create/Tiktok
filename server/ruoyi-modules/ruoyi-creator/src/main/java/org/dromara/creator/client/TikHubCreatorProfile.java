package org.dromara.creator.client;

import lombok.Data;

import java.math.BigInteger;

/**
 * Normalized creator profile from TikHub.
 */
@Data
public class TikHubCreatorProfile {

    private String platformCreatorId;
    private String platformUserId;
    private String platformDisplayId;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private String homepageUrl;
    private String ipLocation;
    private String gender;
    private BigInteger followerCount = BigInteger.ZERO;
    private BigInteger followingCount = BigInteger.ZERO;
    private BigInteger totalFavoritedCount = BigInteger.ZERO;
    private Integer contentCount = 0;
    private String rawJson;
}
