package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * External API call log.
 */
@Data
@TableName("cm_api_call_log")
public class CmApiCallLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "call_id")
    private Long callId;

    private String tenantId;
    private Long runId;
    private String provider;
    private String platform;
    private String endpoint;
    private String requestMethod;
    private String requestKey;
    private Integer httpStatus;
    private String providerCode;
    private Boolean success;
    private Integer latencyMs;
    private BigDecimal unitPriceUsd;
    private BigDecimal unitPriceCny;
    private BigDecimal estimatedCostCny;
    private String errorMessage;
    private Date calledAt;
    private Long createBy;
    private Date createTime;
}
