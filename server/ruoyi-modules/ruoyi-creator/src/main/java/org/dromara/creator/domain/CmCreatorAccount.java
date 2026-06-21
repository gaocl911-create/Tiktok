package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigInteger;
import java.util.Date;

/**
 * Platform creator account.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_creator_account")
public class CmCreatorAccount extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "creator_id")
    private Long creatorId;

    private String platform;
    private String platformCreatorId;
    private String platformUserId;
    private String platformDisplayId;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private String homepageUrl;
    private String ipLocation;
    private String gender;
    private BigInteger followerCount;
    private BigInteger followingCount;
    private BigInteger totalFavoritedCount;
    private Integer contentCount;
    private String profileStatus;
    private Date lastProfileCollectAt;
    private Date lastContentScanAt;
    private String rawProfileJson;

    @TableField(exist = false)
    private String addedByName;

    @TableField(exist = false)
    private Long ownerUserId;

    @TableField(exist = false)
    private Long targetId;

    @TableField(exist = false)
    private Boolean discoverNewContent;

    @TableField(exist = false)
    private String contactWechat;

    @TableLogic
    private String delFlag;
}
