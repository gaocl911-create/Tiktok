package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Collection run log.
 */
@Data
@TableName("cm_collection_run")
public class CmCollectionRun implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "run_id")
    private Long runId;

    private String tenantId;
    private String runType;
    private String triggerSource;
    private String provider;
    private String platform;
    private Long targetId;
    private Long creatorId;
    private Long contentId;
    private String snailJobId;
    private String status;
    private Date startedAt;
    private Date endedAt;
    private Integer durationMs;
    private Integer discoveredCount;
    private Integer collectedCount;
    private Integer failedCount;
    private Integer apiCallCount;
    private BigDecimal estimatedCostCny;
    private String errorCode;
    private String errorMessage;
    private String resultSummaryJson;
    private Long createDept;
    private Long createBy;
    private Date createTime;
}
