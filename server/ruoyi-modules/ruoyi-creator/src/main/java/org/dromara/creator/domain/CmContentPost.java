package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigInteger;
import java.util.Date;

/**
 * Platform content post.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_content_post")
public class CmContentPost extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "content_id")
    private Long contentId;

    private String platform;
    private Long creatorId;
    private String platformContentId;
    private String contentType;
    private String title;
    private String description;
    private String coverUrl;
    private String contentUrl;
    private String shareUrl;
    private Date publishTime;
    private Date firstSeenAt;
    private String addedSource;
    private BigInteger latestLikeCount;
    private BigInteger latestCommentCount;
    private BigInteger latestCollectCount;
    private BigInteger latestShareCount;
    private BigInteger latestPlayCount;
    private String metricsStatus;
    private Date lastMetricsCollectAt;
    private String rawContentJson;

    @TableLogic
    private String delFlag;
}
