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
@TableName("pt_task_submission")
public class PtTaskSubmission extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "submission_id")
    private Long submissionId;

    private Long claimId;
    private Long taskId;
    private Long profileId;
    private Long userId;
    private String platform;
    private String contentUrl;
    private String contentDesc;
    private String screenshotUrl;
    private String submissionStatus;
    private Date submitTime;
    private Long auditBy;
    private Date auditTime;
    private String rejectReason;
    private Long monitorContentId;
    private Long monitorTargetId;
    private Long monitorRunId;
    private String remark;

    @TableLogic
    private String delFlag;
}
