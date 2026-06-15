package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.DeptService;
import org.dromara.common.core.service.UserService;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.creator.domain.PtStaffProfile;
import org.dromara.creator.domain.bo.PtStaffProfileBo;
import org.dromara.creator.domain.vo.PtStaffProfileVo;
import org.dromara.creator.mapper.PtStaffProfileMapper;
import org.dromara.creator.service.IPtStaffProfileService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@RequiredArgsConstructor
@Service
public class PtStaffProfileServiceImpl implements IPtStaffProfileService {

    private final PtStaffProfileMapper ptStaffProfileMapper;
    private final UserService userService;
    private final DeptService deptService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtStaffProfile createEmptyProfile(PtStaffProfileBo bo) {
        PtStaffProfile profile = new PtStaffProfile();
        profile.setUserId(bo.getUserId());
        profile.setTenantId(bo.getTenantId());
        profile.setOnboardingStatus(bo.getOnboardingStatus() == null ? "incomplete" : bo.getOnboardingStatus());
        profile.setPhoneVerified(0);
        ptStaffProfileMapper.insert(profile);
        return profile;
    }

    @Override
    public PtStaffProfileVo queryMyProfile() {
        Long userId = LoginHelper.getUserId();
        PtStaffProfile profile = selectByUserId(userId);
        if (profile == null) {
            throw new ServiceException("兼职资料不存在");
        }
        return toVo(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtStaffProfileVo updateMyProfile(PtStaffProfileBo bo) {
        Long userId = LoginHelper.getUserId();
        PtStaffProfile profile = selectByUserId(userId);
        if (profile == null) {
            throw new ServiceException("兼职资料不存在");
        }
        String status = profile.getOnboardingStatus();
        if (!"incomplete".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("当前状态不允许修改资料，状态：" + status);
        }
        if (bo.getRealName() != null) {
            profile.setRealName(bo.getRealName());
        }
        if (bo.getPhone() != null) {
            profile.setPhone(bo.getPhone());
        }
        if (bo.getWechatId() != null) {
            profile.setWechatId(bo.getWechatId());
        }
        if (bo.getRegion() != null) {
            profile.setRegion(bo.getRegion());
        }
        if (bo.getDouyinId() != null) {
            profile.setDouyinId(bo.getDouyinId());
        }
        if (bo.getRemark() != null) {
            profile.setRemark(bo.getRemark());
        }
        ptStaffProfileMapper.updateById(profile);
        return toVo(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtStaffProfileVo submitForAudit() {
        Long userId = LoginHelper.getUserId();
        PtStaffProfile profile = selectByUserId(userId);
        if (profile == null) {
            throw new ServiceException("兼职资料不存在");
        }
        String status = profile.getOnboardingStatus();
        if (!"incomplete".equals(status) && !"rejected".equals(status)) {
            throw new ServiceException("当前状态不允许提交审核，状态：" + status);
        }
        if (profile.getRealName() == null || profile.getRealName().isBlank()) {
            throw new ServiceException("请先填写真实姓名");
        }
        if (profile.getPhone() == null || profile.getPhone().isBlank()) {
            throw new ServiceException("请先绑定手机号");
        }
        profile.setOnboardingStatus("pending");
        ptStaffProfileMapper.updateById(profile);
        return toVo(profile);
    }

    @Override
    public TableDataInfo<PtStaffProfile> queryStaffPage(PtStaffProfile query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtStaffProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getTenantId() != null, PtStaffProfile::getTenantId, query.getTenantId());
        lqw.like(query.getRealName() != null, PtStaffProfile::getRealName, query.getRealName());
        lqw.eq(query.getOnboardingStatus() != null, PtStaffProfile::getOnboardingStatus, query.getOnboardingStatus());
        lqw.eq(query.getUserId() != null, PtStaffProfile::getUserId, query.getUserId());
        lqw.orderByDesc(PtStaffProfile::getCreateTime);
        Page<PtStaffProfile> page = ptStaffProfileMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtStaffProfileVo approve(Long profileId) {
        PtStaffProfile profile = ptStaffProfileMapper.selectById(profileId);
        if (profile == null) {
            throw new ServiceException("兼职资料不存在");
        }
        if (!"pending".equals(profile.getOnboardingStatus())) {
            throw new ServiceException("只能审核待审核状态的资料，当前状态：" + profile.getOnboardingStatus());
        }
        profile.setOnboardingStatus("approved");
        profile.setAuditBy(LoginHelper.getUserId());
        profile.setAuditAt(new Date());
        ptStaffProfileMapper.updateById(profile);
        return toVo(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtStaffProfileVo reject(Long profileId, String rejectReason) {
        PtStaffProfile profile = ptStaffProfileMapper.selectById(profileId);
        if (profile == null) {
            throw new ServiceException("兼职资料不存在");
        }
        if (!"pending".equals(profile.getOnboardingStatus())) {
            throw new ServiceException("只能审核待审核状态的资料，当前状态：" + profile.getOnboardingStatus());
        }
        profile.setOnboardingStatus("rejected");
        profile.setAuditBy(LoginHelper.getUserId());
        profile.setAuditAt(new Date());
        profile.setRejectReason(rejectReason);
        ptStaffProfileMapper.updateById(profile);
        return toVo(profile);
    }

    @Override
    public PtStaffProfile selectByUserId(Long userId) {
        LambdaQueryWrapper<PtStaffProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtStaffProfile::getUserId, userId);
        lqw.last("limit 1");
        return ptStaffProfileMapper.selectOne(lqw);
    }

    private PtStaffProfileVo toVo(PtStaffProfile profile) {
        PtStaffProfileVo vo = new PtStaffProfileVo();
        BeanUtils.copyProperties(profile, vo);
        vo.setNickname(userService.selectNicknameById(profile.getUserId()));
        Long deptId = LoginHelper.getDeptId();
        if (deptId != null) {
            vo.setDeptName(deptService.selectDeptNameByIds(String.valueOf(deptId)));
        }
        return vo;
    }
}
