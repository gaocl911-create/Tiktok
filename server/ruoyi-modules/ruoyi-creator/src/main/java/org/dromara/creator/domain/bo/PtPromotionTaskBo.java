package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PtPromotionTaskBo {

    private Long taskId;

    @NotBlank(message = "任务标题不能为空")
    private String taskTitle;

    private String platform;

    private String taskDesc;

    private String taskRequirement;

    @NotNull(message = "任务单价不能为空")
    @DecimalMin(value = "0.00", message = "任务单价不能小于0")
    private BigDecimal unitPrice;

    @NotNull(message = "任务名额不能为空")
    @Min(value = 0, message = "任务名额不能小于0")
    private Integer totalQuota;

    private String claimLimitType;

    @Min(value = 0, message = "每人领取次数不能小于0")
    private Integer claimLimitCount;

    private Date startTime;

    private Date endTime;

    private String taskStatus;

    private Long textCategoryId;

    private Long imageCategoryId;

    private String remark;
}
