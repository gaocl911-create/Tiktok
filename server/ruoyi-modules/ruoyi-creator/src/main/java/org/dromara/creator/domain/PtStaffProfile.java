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
@TableName("pt_staff_profile")
public class PtStaffProfile extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "profile_id")
    private Long profileId;

    private Long userId;
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

    @TableLogic
    private String delFlag;
}
