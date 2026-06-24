package org.dromara.creator.service;

import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.PtTaskClaim;
import org.dromara.creator.domain.vo.PtTaskMaterialAssignmentVo;

public interface IPtTaskMaterialAssignmentService {

    PtTaskMaterialAssignmentVo assignForClaim(PtPromotionTask task, PtTaskClaim claim, int assignIndex);

    PtTaskMaterialAssignmentVo queryByClaimId(Long claimId);
}
