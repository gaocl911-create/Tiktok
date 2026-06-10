package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmContentPost;
import org.dromara.creator.domain.CmContentSnapshot;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @SaCheckPermission("creator:content:list")
    @GetMapping("/list")
    public TableDataInfo<CmContentPost> list(CmContentPost query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryContentPage(query, pageQuery);
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}")
    public R<CmContentPost> getInfo(@NotNull(message = "contentId不能为空") @PathVariable Long contentId) {
        return R.ok(creatorMonitorDataService.queryContentById(contentId));
    }

    @SaCheckPermission("creator:content:query")
    @GetMapping("/{contentId}/snapshots")
    public R<List<CmContentSnapshot>> snapshots(@NotNull(message = "contentId不能为空") @PathVariable Long contentId,
                                                @RequestParam(defaultValue = "30") int limit) {
        return R.ok(creatorMonitorDataService.queryRecentContentSnapshots(contentId, limit));
    }
}
