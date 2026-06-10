package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmCollectionRun;
import org.dromara.creator.domain.CmMonitorTarget;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @SaCheckPermission("creator:target:list")
    @GetMapping("/list")
    public TableDataInfo<CmMonitorTarget> list(CmMonitorTarget query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryMonitorTargetPage(query, pageQuery);
    }

    @SaCheckPermission("creator:target:query")
    @GetMapping("/{targetId}")
    public R<CmMonitorTarget> getInfo(@NotNull(message = "targetId不能为空") @PathVariable Long targetId) {
        return R.ok(creatorMonitorDataService.queryTargetById(targetId));
    }

    @SaCheckPermission("creator:target:query")
    @GetMapping("/{targetId}/runs")
    public R<List<CmCollectionRun>> runs(@NotNull(message = "targetId不能为空") @PathVariable Long targetId,
                                         @RequestParam(defaultValue = "30") int limit) {
        return R.ok(creatorMonitorDataService.queryRecentCollectionRuns(targetId, limit));
    }

    @SaCheckPermission("creator:target:remove")
    @DeleteMapping("/{targetIds}")
    public R<Void> remove(@NotEmpty(message = "targetIds不能为空") @PathVariable Long[] targetIds) {
        return creatorMonitorDataService.deleteTargetsByIds(List.of(targetIds)) ? R.ok() : R.fail();
    }
}
