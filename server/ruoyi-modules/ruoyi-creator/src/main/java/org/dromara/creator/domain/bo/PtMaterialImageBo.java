package org.dromara.creator.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PtMaterialImageBo {

    private Long imageId;

    @NotNull(message = "图片分类不能为空")
    private Long categoryId;

    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    private String imageName;
    private Long imageSize;
    private Integer sort;
    private String status;
    private String remark;
}
