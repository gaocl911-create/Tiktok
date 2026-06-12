package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmAlertEvent;
import org.dromara.creator.domain.CmAlertRule;
import org.dromara.creator.domain.bo.AlertEventHandleBo;
import org.dromara.creator.domain.bo.AlertRuleBo;
import org.dromara.creator.service.ICreatorAlertService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/creator/alert")
public class CreatorAlertController {

    private final ICreatorAlertService creatorAlertService;

    @SaCheckPermission("creator:alert:list")
    @GetMapping("/rule/list")
    public TableDataInfo<CmAlertRule> ruleList(CmAlertRule query, PageQuery pageQuery) {
        return creatorAlertService.queryRulePage(query, pageQuery);
    }

    @SaCheckPermission("creator:alert:list")
    @GetMapping("/rule/enabled")
    public R<List<CmAlertRule>> enabledRules() {
        return R.ok(creatorAlertService.queryEnabledRules());
    }

    @SaCheckPermission("creator:alert:rule:add")
    @PostMapping("/rule")
    public R<CmAlertRule> addRule(@Valid @RequestBody AlertRuleBo bo) {
        bo.setRuleId(null);
        return R.ok(creatorAlertService.saveRule(bo));
    }

    @SaCheckPermission("creator:alert:rule:edit")
    @PutMapping("/rule")
    public R<CmAlertRule> editRule(@Valid @RequestBody AlertRuleBo bo) {
        if (bo.getRuleId() == null) {
            return R.fail("ruleId is required");
        }
        return R.ok(creatorAlertService.saveRule(bo));
    }

    @SaCheckPermission("creator:alert:rule:remove")
    @DeleteMapping("/rule/{ruleIds}")
    public R<Void> removeRules(@NotEmpty(message = "ruleIds is required") @PathVariable Long[] ruleIds) {
        return creatorAlertService.deleteRules(List.of(ruleIds)) ? R.ok() : R.fail();
    }

    @SaCheckPermission("creator:alert:list")
    @GetMapping("/event/list")
    public TableDataInfo<CmAlertEvent> eventList(CmAlertEvent query, PageQuery pageQuery) {
        return creatorAlertService.queryEventPage(query, pageQuery);
    }

    @SaCheckPermission("creator:alert:event:handle")
    @PutMapping("/event/{eventId}/handle")
    public R<CmAlertEvent> handleEvent(@NotNull(message = "eventId is required") @PathVariable Long eventId,
                                       @Valid @RequestBody AlertEventHandleBo bo) {
        return R.ok(creatorAlertService.handleEvent(eventId, bo));
    }
}
