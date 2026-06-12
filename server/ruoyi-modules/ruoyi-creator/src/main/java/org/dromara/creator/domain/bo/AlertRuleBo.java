package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigInteger;

/**
 * Alert rule create/update request.
 */
@Data
public class AlertRuleBo {

    private Long ruleId;

    @NotBlank(message = "ruleName is required")
    private String ruleName;

    @NotBlank(message = "metricType is required")
    private String metricType;

    @NotBlank(message = "ruleType is required")
    private String ruleType;

    private Integer windowMinutes;

    @NotNull(message = "thresholdValue is required")
    @Min(value = 1, message = "thresholdValue must be greater than zero")
    private BigInteger thresholdValue;

    @NotBlank(message = "scopeType is required")
    private String scopeType;

    private Long scopeId;

    @NotBlank(message = "severity is required")
    private String severity;

    @Min(value = 0, message = "cooldownMinutes cannot be negative")
    private Integer cooldownMinutes = 120;

    private Boolean enabled = true;
}
