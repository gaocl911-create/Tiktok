package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.PtTaskSubmission;
import org.dromara.creator.domain.bo.PtTaskSubmissionBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.domain.vo.PtTaskClaimVo;
import org.dromara.creator.domain.vo.PtTaskSubmissionVo;

public interface IPtTaskParticipationService {

    TableDataInfo<PtPromotionTask> queryPublishedTaskPage(PtPromotionTask query, PageQuery pageQuery);

    PtPromotionTaskVo queryPublishedTaskById(Long taskId);

    PtTaskClaimVo claimTask(Long taskId);

    PtTaskClaimVo queryMyClaimById(Long claimId);

    TableDataInfo<PtTaskClaimVo> queryMyClaimPage(PageQuery pageQuery);

    PtTaskSubmissionVo submitContent(PtTaskSubmissionBo bo);

    TableDataInfo<PtTaskSubmissionVo> querySubmissionPage(PtTaskSubmission query, PageQuery pageQuery);

    PtTaskSubmissionVo querySubmissionById(Long submissionId);

    PtTaskSubmissionVo approveSubmission(Long submissionId);

    PtTaskSubmissionVo rejectSubmission(Long submissionId, String rejectReason);
}
