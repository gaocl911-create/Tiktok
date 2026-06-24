package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PtMaterialCategoryBo {

    private Long categoryId;

    @NotBlank(message = "分类类型不能为空")
    private String categoryType;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    private Integer sort;
    private String status;
    private String remark;
}
