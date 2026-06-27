package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PtPromotionTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private String tenantId;
    private String taskTitle;
    private String platform;
    private String taskDesc;
    private String taskRequirement;
    private BigDecimal unitPrice;
    private Integer totalQuota;
    private String claimLimitType;
    private Integer claimLimitCount;
    private Integer claimedCount;
    private Integer submittedCount;
    private Integer approvedCount;
    private Date startTime;
    private Date endTime;
    private String taskStatus;
    private Date publishTime;
    private Date pauseTime;
    private Date finishTime;
    private Long textCategoryId;
    private Long imageCategoryId;
    private String textCategoryName;
    private String imageCategoryName;
    private String remark;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}
