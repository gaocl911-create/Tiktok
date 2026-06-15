package org.dromara.creator.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.creator.domain.PtStaffProfile;
import org.dromara.creator.domain.vo.PtStaffProfileVo;
import org.dromara.creator.service.IPtStaffProfileService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Miniapp user profile controller.
 * Provides APIs for the WeChat mini program to query and update staff profile.
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/user")
public class MiniappUserController {

    private final IPtStaffProfileService staffProfileService;

    /**
     * Get current user's staff profile and onboarding status.
     */
    @GetMapping("/profile")
    public R<PtStaffProfileVo> getMyProfile() {
        return R.ok(staffProfileService.queryMyProfile());
    }

    /**
     * Update current user's staff profile (real name, wechat id, region, douyin id).
     */
    @PutMapping("/profile")
    public R<PtStaffProfileVo> updateMyProfile(@RequestBody PtStaffProfile profile) {
        // Only allow updating safe fields from the mini program
        PtStaffProfileVo vo = staffProfileService.updateMyProfile(toBo(profile));
        return R.ok(vo);
    }

    /**
     * Submit onboarding profile for audit review.
     * onboarding_status changes from incomplete/rejected → pending.
     */
    @PostMapping("/profile/submit")
    public R<PtStaffProfileVo> submitForAudit() {
        return R.ok(staffProfileService.submitForAudit());
    }

    private org.dromara.creator.domain.bo.PtStaffProfileBo toBo(PtStaffProfile profile) {
        org.dromara.creator.domain.bo.PtStaffProfileBo bo = new org.dromara.creator.domain.bo.PtStaffProfileBo();
        bo.setRealName(profile.getRealName());
        bo.setPhone(profile.getPhone());
        bo.setWechatId(profile.getWechatId());
        bo.setRegion(profile.getRegion());
        bo.setDouyinId(profile.getDouyinId());
        bo.setRemark(profile.getRemark());
        return bo;
    }
}
