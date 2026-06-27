package org.dromara.creator.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.bo.PtTaskSubmissionBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.domain.vo.PtTaskClaimVo;
import org.dromara.creator.domain.vo.PtTaskSubmissionVo;
import org.dromara.creator.service.IPtTaskParticipationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/task")
public class MiniappTaskController {

    private final IPtTaskParticipationService taskParticipationService;

    @GetMapping("/list")
    public TableDataInfo<PtPromotionTask> list(PtPromotionTask query, PageQuery pageQuery) {
        return taskParticipationService.queryPublishedTaskPage(query, pageQuery);
    }

    @GetMapping("/{taskId}")
    public R<PtPromotionTaskVo> getInfo(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(taskParticipationService.queryPublishedTaskById(taskId));
    }

    @PostMapping("/{taskId}/claim")
    public R<PtTaskClaimVo> claim(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(taskParticipationService.claimTask(taskId));
    }

    @GetMapping("/claim/{claimId}")
    public R<PtTaskClaimVo> getClaim(@NotNull(message = "claimId is required") @PathVariable Long claimId) {
        return R.ok(taskParticipationService.queryMyClaimById(claimId));
    }

    @GetMapping("/my")
    public TableDataInfo<PtTaskClaimVo> myTasks(PageQuery pageQuery,
                                                @RequestParam(required = false) String group) {
        return taskParticipationService.queryMyClaimPage(pageQuery, group);
    }

    @PostMapping("/claim/{claimId}/submit-content")
    public R<PtTaskSubmissionVo> submitContent(@NotNull(message = "claimId is required") @PathVariable Long claimId,
                                               @Valid @RequestBody PtTaskSubmissionBo bo) {
        bo.setClaimId(claimId);
        return R.ok(taskParticipationService.submitContent(bo));
    }
}
