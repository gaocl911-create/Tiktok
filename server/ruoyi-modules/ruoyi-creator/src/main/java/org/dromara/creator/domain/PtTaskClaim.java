package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pt_task_claim")
public class PtTaskClaim extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "claim_id")
    private Long claimId;

    private Long taskId;
    private Long profileId;
    private Long userId;
    private String claimStatus;
    private Date claimTime;
    private Date submitTime;
    private Date finishTime;
    private String remark;

    @TableLogic
    private String delFlag;
}
