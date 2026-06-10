package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 * Creator profile metrics snapshot.
 */
@Data
@TableName("cm_creator_snapshot")
public class CmCreatorSnapshot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "snapshot_id")
    private Long snapshotId;

    private String tenantId;
    private Long creatorId;
    private Long targetId;
    private Date collectedAt;
    private BigInteger followerCount;
    private BigInteger followingCount;
    private BigInteger totalFavoritedCount;
    private Integer contentCount;
    private String profileStatus;
    private String rawSnapshotJson;
    private Date createTime;
}
