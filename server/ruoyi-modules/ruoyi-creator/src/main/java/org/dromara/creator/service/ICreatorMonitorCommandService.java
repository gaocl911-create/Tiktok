package org.dromara.creator.service;

import org.dromara.creator.domain.bo.ContentLinkCreateBo;
import org.dromara.creator.domain.bo.CreatorMonitorCreateBo;
import org.dromara.creator.domain.bo.MonitorTargetCreateBo;
import org.dromara.creator.domain.vo.MonitorCreateResultVo;

import java.util.Collection;

/**
 * Business write and collection commands for creator monitor.
 */
public interface ICreatorMonitorCommandService {

    MonitorCreateResultVo addCreatorMonitor(CreatorMonitorCreateBo bo);

    MonitorCreateResultVo addContentLinkMonitor(ContentLinkCreateBo bo);

    MonitorCreateResultVo createMonitorTarget(MonitorTargetCreateBo bo);

    Integer collectDueTargets(int limit, String triggerSource);

    MonitorCreateResultVo collectTargetNow(Long targetId, String triggerSource);

    MonitorCreateResultVo collectCreatorProfileNow(Long creatorId, String triggerSource);

    /**
     * Cancel the current user's creator monitoring targets.
     * Shared creator data, content, snapshots, runs and alert history are preserved.
     *
     * @param creatorIds creator ids whose monitoring should be cancelled
     */
    void deleteCreators(Collection<Long> creatorIds);

    /**
     * Cancel the current user's monitoring relationship for the selected contents.
     * Shared content data and historical records are preserved.
     *
     * @param contentIds content ids whose monitoring should be cancelled
     */
    void deleteContents(Collection<Long> contentIds);
}
