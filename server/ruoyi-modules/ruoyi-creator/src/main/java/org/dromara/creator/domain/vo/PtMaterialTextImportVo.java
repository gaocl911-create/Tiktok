package org.dromara.creator.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文案素材导入模板。
 */
@Data
public class PtMaterialTextImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "文案内容")
    private String content;

    @ExcelProperty(value = "排序")
    private Integer sort;

    @ExcelProperty(value = "状态（启用/停用）")
    private String status;

    @ExcelProperty(value = "备注")
    private String remark;
}
