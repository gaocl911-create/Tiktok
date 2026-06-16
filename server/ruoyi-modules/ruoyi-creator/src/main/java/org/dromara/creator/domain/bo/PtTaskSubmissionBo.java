package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtTaskSubmissionBo {

    private Long submissionId;

    private Long claimId;

    @NotBlank(message = "作品链接不能为空")
    private String contentUrl;

    private String contentDesc;

    private String screenshotUrl;

    private String remark;
}
