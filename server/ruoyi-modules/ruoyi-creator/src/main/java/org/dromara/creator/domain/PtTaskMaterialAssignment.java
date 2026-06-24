package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pt_task_material_assignment")
public class PtTaskMaterialAssignment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "assignment_id")
    private Long assignmentId;

    private Long taskId;
    private Long claimId;
    private Long userId;
    private Integer assignIndex;
    private Long textId;
    private String textSnapshot;
    private Long imageId;
    private String imageUrlSnapshot;
    private String imageNameSnapshot;

    @TableLogic
    private String delFlag;
}
