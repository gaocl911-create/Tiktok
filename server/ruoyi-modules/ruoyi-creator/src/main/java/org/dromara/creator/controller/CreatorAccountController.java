package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmCreatorAccount;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creator account management.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/creator/account")
public class CreatorAccountController {

    private final ICreatorMonitorDataService creatorMonitorDataService;

    @SaCheckPermission("creator:account:list")
    @GetMapping("/list")
    public TableDataInfo<CmCreatorAccount> list(CmCreatorAccount query, PageQuery pageQuery) {
        return creatorMonitorDataService.queryCreatorPage(query, pageQuery);
    }

    @SaCheckPermission("creator:account:query")
    @GetMapping("/{creatorId}")
    public R<CmCreatorAccount> getInfo(@NotNull(message = "creatorId不能为空") @PathVariable Long creatorId) {
        return R.ok(creatorMonitorDataService.queryCreatorById(creatorId));
    }
}
