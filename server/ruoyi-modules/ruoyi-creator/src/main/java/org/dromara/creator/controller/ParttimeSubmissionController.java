package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtTaskSubmission;
import org.dromara.creator.domain.vo.PtTaskSubmissionVo;
import org.dromara.creator.service.IPtTaskParticipationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/submission")
public class ParttimeSubmissionController {

    private final IPtTaskParticipationService taskParticipationService;

    @SaCheckPermission("parttime:submission:list")
    @GetMapping("/list")
    public TableDataInfo<PtTaskSubmissionVo> list(PtTaskSubmission query, PageQuery pageQuery) {
        return taskParticipationService.querySubmissionPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:submission:query")
    @GetMapping("/{submissionId}")
    public R<PtTaskSubmissionVo> getInfo(@NotNull(message = "submissionId is required") @PathVariable Long submissionId) {
        return R.ok(taskParticipationService.querySubmissionById(submissionId));
    }

    @SaCheckPermission("parttime:submission:approve")
    @PostMapping("/{submissionId}/approve")
    public R<PtTaskSubmissionVo> approve(@NotNull(message = "submissionId is required") @PathVariable Long submissionId) {
        return R.ok(taskParticipationService.approveSubmission(submissionId));
    }

    @SaCheckPermission("parttime:submission:reject")
    @PostMapping("/{submissionId}/reject")
    public R<PtTaskSubmissionVo> reject(@NotNull(message = "submissionId is required") @PathVariable Long submissionId,
                                        @RequestParam String rejectReason) {
        return R.ok(taskParticipationService.rejectSubmission(submissionId, rejectReason));
    }
}
