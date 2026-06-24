package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtMaterialCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String tenantId;
    private String categoryType;
    private String categoryName;
    private Integer sort;
    private String status;
    private String remark;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}
