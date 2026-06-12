package org.dromara.creator.domain.vo;

import lombok.Data;
import org.dromara.creator.domain.CmCollectionRun;
import org.dromara.creator.domain.CmContentPost;
import org.dromara.creator.domain.CmCreatorAccount;
import org.dromara.creator.domain.CmMonitorTarget;

/**
 * Result returned after creating monitor business records.
 */
@Data
public class MonitorCreateResultVo {

    private CmCreatorAccount creator;
    private CmContentPost content;
    private CmMonitorTarget target;
    private CmCollectionRun run;
    private Boolean creatorCreated;
    private Boolean contentCreated;
    private Boolean targetCreated;
}
