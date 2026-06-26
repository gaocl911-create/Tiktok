package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request for creating a single-content monitor from a share link.
 */
@Data
public class ContentLinkCreateBo {

    @NotBlank(message = "platform is required")
    private String platform = "douyin";

    @NotBlank(message = "contentInput is required")
    private String contentInput;

    private String targetName;
    private String remark;
    private String tags;

    private Long ownerUserId;
    private Long ownerDeptId;
    private Long directSuperiorUserId;

    @NotNull(message = "contentCollectIntervalMin is required")
    @Min(value = 15, message = "contentCollectIntervalMin must be at least 15 minutes")
    @Max(value = 1440, message = "contentCollectIntervalMin must be at most 1440 minutes")
    private Integer contentCollectIntervalMin = 120;
}
