package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtTaskClaimVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long claimId;
    private String tenantId;
    private Long taskId;
    private Long profileId;
    private Long userId;
    private String claimStatus;
    private Date claimTime;
    private Date submitTime;
    private Date finishTime;
    private String remark;
    private Date createTime;

    private String taskTitle;
    private String platform;
    private String realName;
    private String phone;

    private Integer claimRound;
    private Integer assignIndex;
    private Long textId;
    private String assignedText;
    private Long imageId;
    private String assignedImageUrl;
    private String assignedImageName;

    private Long submissionId;
    private String contentUrl;
    private String contentDesc;
    private String screenshotUrl;
    private String submissionStatus;
    private Date auditTime;
    private String rejectReason;
    private Long monitorContentId;
}
