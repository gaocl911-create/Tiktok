package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pt_promotion_task")
public class PtPromotionTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id")
    private Long taskId;

    private String taskTitle;
    private String platform;
    private String taskDesc;
    private String taskRequirement;
    private BigDecimal unitPrice;
    private Integer totalQuota;
    private Integer claimedCount;
    private Integer submittedCount;
    private Integer approvedCount;
    private Date startTime;
    private Date endTime;
    private String taskStatus;
    private Date publishTime;
    private Date pauseTime;
    private Date finishTime;
    private String remark;

    @TableLogic
    private String delFlag;
}
