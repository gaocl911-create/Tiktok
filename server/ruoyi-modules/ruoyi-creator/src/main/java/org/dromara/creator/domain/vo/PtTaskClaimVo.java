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
}
