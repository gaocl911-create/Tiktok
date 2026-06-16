package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.bo.PtPromotionTaskBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.service.IPtPromotionTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/task")
public class ParttimeTaskController {

    private final IPtPromotionTaskService promotionTaskService;

    @SaCheckPermission("parttime:task:list")
    @GetMapping("/list")
    public TableDataInfo<PtPromotionTask> list(PtPromotionTask query, PageQuery pageQuery) {
        return promotionTaskService.queryTaskPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:task:query")
    @GetMapping("/{taskId}")
    public R<PtPromotionTaskVo> getInfo(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(promotionTaskService.queryById(taskId));
    }

    @SaCheckPermission("parttime:task:add")
    @PostMapping
    public R<PtPromotionTaskVo> add(@Valid @RequestBody PtPromotionTaskBo bo) {
        return R.ok(promotionTaskService.createTask(bo));
    }

    @SaCheckPermission("parttime:task:edit")
    @PutMapping
    public R<PtPromotionTaskVo> edit(@Valid @RequestBody PtPromotionTaskBo bo) {
        return R.ok(promotionTaskService.updateTask(bo));
    }

    @SaCheckPermission("parttime:task:publish")
    @PostMapping("/{taskId}/publish")
    public R<PtPromotionTaskVo> publish(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(promotionTaskService.publish(taskId));
    }

    @SaCheckPermission("parttime:task:pause")
    @PostMapping("/{taskId}/pause")
    public R<PtPromotionTaskVo> pause(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(promotionTaskService.pause(taskId));
    }

    @SaCheckPermission("parttime:task:finish")
    @PostMapping("/{taskId}/finish")
    public R<PtPromotionTaskVo> finish(@NotNull(message = "taskId is required") @PathVariable Long taskId) {
        return R.ok(promotionTaskService.finish(taskId));
    }
}
