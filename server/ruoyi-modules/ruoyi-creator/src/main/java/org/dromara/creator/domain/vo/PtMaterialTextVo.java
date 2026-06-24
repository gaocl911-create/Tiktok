package org.dromara.creator.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class PtMaterialTextVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long textId;
    private String tenantId;
    private Long categoryId;
    private String categoryName;
    private String content;
    private Integer sort;
    private String status;
    private String remark;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}
