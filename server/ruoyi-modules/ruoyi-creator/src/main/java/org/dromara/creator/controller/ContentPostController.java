package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmContentPost;
import org.dromara.creator.domain.CmContentSnapshot;
import org.dromara.creator.domain.CmCollectionRun;
import org.dromara.creator.domain.CmMonitorTarget;
import org.dromara.creator.domain.bo.ContentLinkCreateBo;
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
 * Content post management.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/creator/content")
public class ContentPostController {

    private final ICreatorMonitorDataService creatorMonitorDataService;
    private final ICreatorMonitorCommandService creatorMonitorCommandService;

    @SaCheckPermission("creator:content:list")
    @GetMapping("/list")
    public TableDataInfo<CmContentPost> list(CmContentPost query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryContentPage(query, pageQuery);
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}")
    public R<CmContentPost> getInfo(@NotNull(message = "contentId is required") @PathVariable Long contentId) {
        return R.ok(creatorMonitorDataService.queryContentById(contentId));
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}/snapshots")
    public R<List<CmContentSnapshot>> snapshots(@NotNull(message = "contentId is required") @PathVariable Long contentId,
                                                @RequestParam(defaultValue = "30") int limit) {
        return R.ok(creatorMonitorDataService.queryRecentContentSnapshots(contentId, limit));
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}/target")
    public R<CmMonitorTarget> target(@NotNull(message = "contentId is required") @PathVariable Long contentId) {
        return R.ok(creatorMonitorDataService.queryTargetByContentId(contentId));
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}/runs")
    public R<List<CmCollectionRun>> runs(@NotNull(message = "contentId is required") @PathVariable Long contentId,
                                         @RequestParam(defaultValue = "50") int limit) {
        return R.ok(creatorMonitorDataService.queryRecentContentRuns(contentId, limit));
    }

    /**
     * Add one post by share text/link. This creates a single-content target and monitors only this post.
     */
    @SaCheckPermission("creator:content:add")
    @PostMapping("/link")
    public R<MonitorCreateResultVo> addLink(@Valid @RequestBody ContentLinkCreateBo bo) {
        return R.ok(creatorMonitorCommandService.addContentLinkMonitor(bo));
    }

    /**
     * Cancel content monitoring while preserving shared data and collection history.
     */
    @SaCheckPermission("creator:content:remove")
    @DeleteMapping("/{contentIds}")
    public R<Void> remove(@NotEmpty(message = "contentIds is required") @PathVariable Long[] contentIds) {
        creatorMonitorCommandService.deleteContents(List.of(contentIds));
        return R.ok();
    }
}
