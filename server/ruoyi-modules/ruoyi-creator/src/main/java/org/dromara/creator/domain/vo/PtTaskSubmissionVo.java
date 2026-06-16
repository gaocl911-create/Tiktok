package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtTaskSubmissionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private String tenantId;
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
    private Date createTime;
    private Date updateTime;

    private String taskTitle;
    private String realName;
    private String phone;
    private String douyinId;
}
