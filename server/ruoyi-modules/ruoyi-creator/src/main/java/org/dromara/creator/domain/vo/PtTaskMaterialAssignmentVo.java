package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtTaskMaterialAssignmentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long assignmentId;
    private String tenantId;
    private Long taskId;
    private Long claimId;
    private Long userId;
    private Integer assignIndex;
    private Long textId;
    private String assignedText;
    private Long imageId;
    private String assignedImageUrl;
    private String assignedImageName;
    private Date createTime;
}
