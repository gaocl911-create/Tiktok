package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigInteger;

/**
 * Configurable content metric alert rule.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_alert_rule")
public class CmAlertRule extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "rule_id")
    private Long ruleId;

    private String ruleName;
    private String metricType;
    private String ruleType;
    private Integer windowMinutes;
    private BigInteger thresholdValue;
    private String scopeType;
    private Long scopeId;
    private String severity;
    private Integer cooldownMinutes;
    private Boolean enabled;

    @TableLogic
    private String delFlag;
}
