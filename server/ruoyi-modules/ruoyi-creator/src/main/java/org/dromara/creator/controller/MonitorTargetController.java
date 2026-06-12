package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmCollectionRun;
import org.dromara.creator.domain.CmMonitorTarget;
import org.dromara.creator.domain.bo.MonitorTargetCreateBo;
import org.dromara.creator.domain.vo.MonitorCreateResultVo;
import org.dromara.creator.service.ICreatorMonitorCommandService;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Monitor target management.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/creator/target")
public class MonitorTargetController {

    private final ICreatorMonitorDataService creatorMonitorDataService;
    private final ICreatorMonitorCommandService creatorMonitorCommandService;

    @SaCheckPermission("creator:target:list")
    @GetMapping("/list")
    public TableDataInfo<CmMonitorTarget> list(CmMonitorTarget query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryMonitorTargetPage(query, pageQuery);
    }

    @SaCheckPermission("creator:target:query")
    @GetMapping("/{targetId}")
    public R<CmMonitorTarget> getInfo(@NotNull(message = "targetId is required") @PathVariable Long targetId) {
        return R.ok(creatorMonitorDataService.queryTargetById(targetId));
    }

    @SaCheckPermission("creator:target:query")
    @GetMapping("/{targetId}/runs")
    public R<List<CmCollectionRun>> runs(@NotNull(message = "targetId is required") @PathVariable Long targetId,
                                         @RequestParam(defaultValue = "30") int limit) {
        return R.ok(creatorMonitorDataService.queryRecentCollectionRuns(targetId, limit));
    }

    @SaCheckPermission("creator:target:add")
    @PostMapping
    public R<MonitorCreateResultVo> add(@Valid @RequestBody MonitorTargetCreateBo bo) {
        return R.ok(creatorMonitorCommandService.createMonitorTarget(bo));
    }

    @SaCheckPermission("creator:target:collect")
    @PostMapping("/{targetId}/collect")
    public R<MonitorCreateResultVo> collect(@NotNull(message = "targetId is required") @PathVariable Long targetId) {
        return R.ok(creatorMonitorCommandService.collectTargetNow(targetId, "manual"));
    }

    @SaCheckPermission("creator:target:collect")
    @PostMapping("/collect-due")
    public R<Integer> collectDue(@RequestParam(defaultValue = "100") int limit) {
        return R.ok(creatorMonitorCommandService.collectDueTargets(limit, "manual_batch"));
    }

    @SaCheckPermission("creator:target:remove")
    @DeleteMapping("/{targetIds}")
    public R<Void> remove(@NotEmpty(message = "targetIds is required") @PathVariable Long[] targetIds) {
        return creatorMonitorDataService.deleteTargetsByIds(List.of(targetIds)) ? R.ok() : R.fail();
    }
}
