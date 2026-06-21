package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmCreatorAccount;
import org.dromara.creator.domain.bo.CreatorContactWechatBo;
import org.dromara.creator.domain.bo.CreatorMonitorCreateBo;
import org.dromara.creator.domain.vo.MonitorCreateResultVo;
import org.dromara.creator.service.ICreatorMonitorCommandService;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Creator account management.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/creator/account")
public class CreatorAccountController {

    private final ICreatorMonitorDataService creatorMonitorDataService;
    private final ICreatorMonitorCommandService creatorMonitorCommandService;

    @SaCheckPermission("creator:account:list")
    @GetMapping("/list")
    public TableDataInfo<CmCreatorAccount> list(CmCreatorAccount query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryCreatorPage(query, pageQuery);
    }

    @SaCheckPermission("creator:account:query")
    @GetMapping("/{creatorId}")
    public R<CmCreatorAccount> getInfo(@NotNull(message = "creatorId is required") @PathVariable Long creatorId) {
        return R.ok(creatorMonitorDataService.queryCreatorById(creatorId));
    }

    /**
     * Refresh only the creator profile. This does not scan or collect creator posts.
     */
    @SaCheckPermission("creator:target:collect")
    @PostMapping("/{creatorId}/collect-profile")
    public R<MonitorCreateResultVo> collectProfile(
        @NotNull(message = "creatorId is required") @PathVariable Long creatorId
    ) {
        return R.ok(creatorMonitorCommandService.collectCreatorProfileNow(creatorId, "manual_profile"));
    }

    /**
     * Add an author monitor. It collects author profile first; later scheduled runs discover new posts.
     */
    @SaCheckPermission("creator:account:add")
    @PostMapping("/monitor")
    public R<MonitorCreateResultVo> addMonitor(@Valid @RequestBody CreatorMonitorCreateBo bo) {
        return R.ok(creatorMonitorCommandService.addCreatorMonitor(bo));
    }

    /**
     * Update the contact WeChat account for a creator monitor.
     */
    @SaCheckPermission("creator:account:edit")
    @PutMapping("/{creatorId}/contact-wechat")
    public R<Void> updateContactWechat(
        @NotNull(message = "creatorId is required") @PathVariable Long creatorId,
        @RequestBody CreatorContactWechatBo bo
    ) {
        creatorMonitorCommandService.updateCreatorContactWechat(creatorId, bo == null ? null : bo.getContactWechat());
        return R.ok();
    }

    /**
     * Update whether scheduled jobs should discover new posts from this creator homepage.
     */
    @SaCheckPermission("creator:account:edit")
    @PutMapping("/{creatorId}/discover-new-content")
    public R<Void> updateDiscoverNewContent(
        @NotNull(message = "creatorId is required") @PathVariable Long creatorId,
        @RequestParam(defaultValue = "false") Boolean discoverNewContent
    ) {
        creatorMonitorCommandService.updateCreatorDiscoverNewContent(creatorId, discoverNewContent);
        return R.ok();
    }

    /**
     * Cancel creator monitoring while preserving shared data and collection history.
     */
    @SaCheckPermission("creator:account:remove")
    @DeleteMapping("/{creatorIds}")
    public R<Void> remove(@NotEmpty(message = "creatorIds is required") @PathVariable Long[] creatorIds) {
        creatorMonitorCommandService.deleteCreators(List.of(creatorIds));
        return R.ok();
    }
}
