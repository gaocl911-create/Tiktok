package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
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
    private Integer contentCollectIntervalMin = 30;
}
