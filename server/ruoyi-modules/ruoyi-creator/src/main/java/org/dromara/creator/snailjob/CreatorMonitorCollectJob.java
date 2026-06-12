package org.dromara.creator.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import lombok.RequiredArgsConstructor;
import org.dromara.creator.service.ICreatorMonitorCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled entry for creator monitor collection.
 */
@Component
@RequiredArgsConstructor
@JobExecutor(name = "creatorMonitorCollectJob")
public class CreatorMonitorCollectJob {

    private final ICreatorMonitorCommandService creatorMonitorCommandService;

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        Integer count = creatorMonitorCommandService.collectDueTargets(100, "snail_job");
        String message = "creator monitor collected due targets: " + count;
        SnailJobLog.LOCAL.info(message);
        SnailJobLog.REMOTE.info(message);
        return ExecuteResult.success(message);
    }

    @Scheduled(
        initialDelayString = "${creator.monitor.scheduler.initial-delay-ms:60000}",
        fixedDelayString = "${creator.monitor.scheduler.fixed-delay-ms:60000}"
    )
    public void collectDueTargetsByLocalScheduler() {
        Integer count = creatorMonitorCommandService.collectDueTargets(100, "local_scheduler");
        if (count > 0) {
            SnailJobLog.LOCAL.info("creator monitor local scheduler collected due targets: {}", count);
        }
    }
}
