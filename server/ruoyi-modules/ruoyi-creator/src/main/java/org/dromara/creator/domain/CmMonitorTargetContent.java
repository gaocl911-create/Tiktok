package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * Monitor target and content relation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_monitor_target_content")
public class CmMonitorTargetContent extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long targetId;
    private Long contentId;
    private String relationSource;
    private Boolean publishedAfterBase;
    private Date firstBoundAt;
    private Date lastCollectedAt;
    private String status;

    @TableLogic
    private String delFlag;
}
