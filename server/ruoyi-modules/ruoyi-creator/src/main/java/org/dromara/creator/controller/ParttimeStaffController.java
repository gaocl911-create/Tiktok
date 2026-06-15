package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtStaffProfile;
import org.dromara.creator.domain.vo.PtStaffProfileVo;
import org.dromara.creator.service.IPtStaffProfileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Part-time staff management controller for admin backend.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/staff")
public class ParttimeStaffController {

    private final IPtStaffProfileService staffProfileService;

    @SaCheckPermission("parttime:staff:list")
    @GetMapping("/list")
    public TableDataInfo<PtStaffProfile> list(PtStaffProfile query, PageQuery pageQuery) {
        return staffProfileService.queryStaffPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:staff:approve")
    @PutMapping("/{profileId}/approve")
    public R<PtStaffProfileVo> approve(@NotNull(message = "profileId is required") @PathVariable Long profileId) {
        return R.ok(staffProfileService.approve(profileId));
    }

    @SaCheckPermission("parttime:staff:reject")
    @PutMapping("/{profileId}/reject")
    public R<PtStaffProfileVo> reject(@NotNull(message = "profileId is required") @PathVariable Long profileId,
                                      @RequestParam String rejectReason) {
        return R.ok(staffProfileService.reject(profileId, rejectReason));
    }
}
