package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

/**
 * Request for creating a monitor target from existing creator/content records.
 */
@Data
public class MonitorTargetCreateBo {

    @NotBlank(message = "targetType is required")
    private String targetType;

    @NotBlank(message = "platform is required")
    private String platform = "douyin";

    private String targetName;
    private Long creatorId;
    private Long contentId;
    private Long ownerUserId;
    private Long ownerDeptId;
    private Long directSuperiorUserId;
    private Date baselineTime;
    private Boolean discoverNewContent;
    private Integer profileCollectIntervalMin = 360;
    private Integer contentCollectIntervalMin = 120;
    private String remark;
    private String tags;
}
