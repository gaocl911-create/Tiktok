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
@TableName("pt_task_material_config")
public class PtTaskMaterialConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "config_id")
    private Long configId;

    private Long taskId;
    private Long textCategoryId;
    private Long imageCategoryId;
    private String assignMode;

    @TableLogic
    private String delFlag;
}
