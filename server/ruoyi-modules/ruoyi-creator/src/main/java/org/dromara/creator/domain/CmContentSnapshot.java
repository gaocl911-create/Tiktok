package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 * Content interaction metrics snapshot.
 */
@Data
@TableName("cm_content_snapshot")
public class CmContentSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "snapshot_id")
    private Long snapshotId;

    private String tenantId;
    private Long contentId;
    private Long targetId;
    private Date collectedAt;
    private BigInteger likeCount;
    private BigInteger commentCount;
    private BigInteger collectCount;
    private BigInteger shareCount;
    private BigInteger playCount;
    private BigInteger likeDelta;
    private BigInteger commentDelta;
    private BigInteger collectDelta;
    private BigInteger shareDelta;
    private String metricsStatus;
    private String missingMetricFields;
    private String rawSnapshotJson;
    private Date createTime;
}
