package org.dromara.creator.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigInteger;
import java.util.Date;

/**
 * Alert event generated from a content snapshot.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cm_alert_event")
public class CmAlertEvent extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "event_id")
    private Long eventId;

    private Long ruleId;
    private Long contentId;
    private Long creatorId;
    private Long targetId;
    private Long snapshotId;
    private String eventTitle;
    private String contentTitle;
    private String creatorNickname;
    private String metricType;
    private String ruleType;
    private Integer windowMinutes;
    private BigInteger thresholdValue;
    private BigInteger observedValue;
    private Date windowStartAt;
    private Date windowEndAt;
    private String severity;
    private String status;
    private Integer triggerCount;
    private Date firstTriggeredAt;
    private Date lastTriggeredAt;
    private Long handledBy;
    private Date handledAt;
    private String handleNote;
}
