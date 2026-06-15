package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtStaffProfileBo {

    private Long profileId;

    private Long userId;

    private String realName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private Integer phoneVerified;

    private String wechatId;

    private String region;

    private String douyinId;

    private Long inviterUserId;

    private String onboardingStatus;

    private String remark;

    private String tenantId;
}
