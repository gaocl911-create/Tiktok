package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PtMaterialTextBo {

    private Long textId;

    @NotNull(message = "文案分类不能为空")
    private Long categoryId;

    @NotBlank(message = "文案内容不能为空")
    private String content;

    private Integer sort;
    private String status;
    private String remark;
}
