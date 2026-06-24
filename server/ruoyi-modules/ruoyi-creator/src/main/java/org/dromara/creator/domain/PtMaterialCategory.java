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
@TableName("pt_material_category")
public class PtMaterialCategory extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "category_id")
    private Long categoryId;

    private String categoryType;
    private String categoryName;
    private Integer sort;
    private String status;
    private String remark;

    @TableLogic
    private String delFlag;
}
