package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtStaffProfile;
import org.dromara.creator.domain.bo.PtStaffProfileBo;
import org.dromara.creator.domain.vo.PtStaffProfileVo;

public interface IPtStaffProfileService {

    /**
     * 创建空的兼职资料档案（微信登录自动建档时调用）
     */
    PtStaffProfile createEmptyProfile(PtStaffProfileBo bo);

    /**
     * 查询当前登录用户的兼职资料
     */
    PtStaffProfileVo queryMyProfile();

    /**
     * 更新当前登录用户的兼职资料
     */
    PtStaffProfileVo updateMyProfile(PtStaffProfileBo bo);

    /**
     * 提交入驻审核（onboarding_status → pending）
     */
    PtStaffProfileVo submitForAudit();

    /**
     * 分页查询兼职人员列表（后台管理）
     */
    TableDataInfo<PtStaffProfile> queryStaffPage(PtStaffProfile query, PageQuery pageQuery);

    /**
     * 审核通过
     */
    PtStaffProfileVo approve(Long profileId);

    /**
     * 审核驳回
     */
    PtStaffProfileVo reject(Long profileId, String rejectReason);

    /**
     * 根据 userId 查询兼职资料
     */
    PtStaffProfile selectByUserId(Long userId);
}
