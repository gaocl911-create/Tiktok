package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * Business monitor target.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_monitor_target")
public class CmMonitorTarget extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "target_id")
    private Long targetId;

    private String targetType;
    private String platform;
    private String targetName;
    private Long ownerUserId;
    private Long ownerDeptId;
    private Long directSuperiorUserId;
    private Long creatorId;
    private Long contentId;
    private Date baselineTime;
    private Boolean discoverNewContent;
    private Integer profileCollectIntervalMin;
    private Integer contentCollectIntervalMin;
    private Date lastProfileCollectAt;
    private Date lastContentCollectAt;
    private Date lastDiscoveryAt;
    private Date nextProfileCollectAt;
    private Date nextContentCollectAt;
    private Date nextDiscoveryAt;
    private String status;
    private String dataStatus;
    private String remark;
    private String contactWechat;
    private String tags;

    @TableLogic
    private String delFlag;
}
