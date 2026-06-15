package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtStaffProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long profileId;
    private Long userId;
    private String tenantId;
    private String realName;
    private String phone;
    private Integer phoneVerified;
    private String wechatId;
    private String region;
    private String douyinId;
    private Long inviterUserId;
    private String onboardingStatus;
    private Long auditBy;
    private Date auditAt;
    private String rejectReason;
    private String remark;

    // 用户基础信息（来自 sys_user，便于前端展示）
    private String nickname;
    private String avatarUrl;
    private String deptName;
}
