package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.PtStaffProfile;
import org.dromara.creator.domain.PtTaskClaim;
import org.dromara.creator.domain.PtTaskSubmission;
import org.dromara.creator.domain.bo.ContentLinkCreateBo;
import org.dromara.creator.domain.bo.PtTaskSubmissionBo;
import org.dromara.creator.domain.vo.MonitorCreateResultVo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.domain.vo.PtTaskClaimVo;
import org.dromara.creator.domain.vo.PtTaskMaterialAssignmentVo;
import org.dromara.creator.domain.vo.PtTaskSubmissionVo;
import org.dromara.creator.mapper.PtPromotionTaskMapper;
import org.dromara.creator.mapper.PtStaffProfileMapper;
import org.dromara.creator.mapper.PtTaskClaimMapper;
import org.dromara.creator.mapper.PtTaskSubmissionMapper;
import org.dromara.creator.service.ICreatorMonitorCommandService;
import org.dromara.creator.service.IPtTaskMaterialAssignmentService;
import org.dromara.creator.service.IPtTaskParticipationService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class PtTaskParticipationServiceImpl implements IPtTaskParticipationService {

    private static final String TASK_PUBLISHED = "published";
    private static final String STAFF_APPROVED = "approved";
    private static final String CLAIM_CLAIMED = "claimed";
    private static final String CLAIM_SUBMITTED = "submitted";
    private static final String CLAIM_APPROVED = "approved";
    private static final String CLAIM_REJECTED = "rejected";
    private static final String SUBMISSION_PENDING = "pending";
    private static final String SUBMISSION_APPROVED = "approved";
    private static final String SUBMISSION_REJECTED = "rejected";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final PtPromotionTaskMapper ptPromotionTaskMapper;
    private final PtStaffProfileMapper ptStaffProfileMapper;
    private final PtTaskClaimMapper ptTaskClaimMapper;
    private final PtTaskSubmissionMapper ptTaskSubmissionMapper;
    private final ICreatorMonitorCommandService creatorMonitorCommandService;
    private final IPtTaskMaterialAssignmentService taskMaterialAssignmentService;

    @Override
    public TableDataInfo<PtPromotionTask> queryPublishedTaskPage(PtPromotionTask query, PageQuery pageQuery) {
        Date now = new Date();
        LambdaQueryWrapper<PtPromotionTask> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtPromotionTask::getTaskStatus, TASK_PUBLISHED);
        lqw.eq(StringUtils.isNotBlank(query.getPlatform()), PtPromotionTask::getPlatform, query.getPlatform());
        lqw.like(StringUtils.isNotBlank(query.getTaskTitle()), PtPromotionTask::getTaskTitle, query.getTaskTitle());
        lqw.and(wrapper -> wrapper.isNull(PtPromotionTask::getStartTime).or().le(PtPromotionTask::getStartTime, now));
        lqw.and(wrapper -> wrapper.isNull(PtPromotionTask::getEndTime).or().ge(PtPromotionTask::getEndTime, now));
        lqw.orderByDesc(PtPromotionTask::getPublishTime);
        return TableDataInfo.build(ptPromotionTaskMapper.selectPage(pageQuery.build(), lqw));
    }

    @Override
    public PtPromotionTaskVo queryPublishedTaskById(Long taskId) {
        PtPromotionTask task = getTask(taskId);
        ensureTaskCanClaim(task);
        PtPromotionTaskVo vo = new PtPromotionTaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtTaskClaimVo claimTask(Long taskId) {
        PtStaffProfile profile = getApprovedMyProfile();
        PtPromotionTask task = getTaskForUpdate(taskId);
        ensureTaskCanClaim(task);

        PtTaskClaim existing = findClaim(taskId, LoginHelper.getUserId());
        if (existing != null) {
            return toClaimVo(existing);
        }

        int claimedCount = task.getClaimedCount() == null ? 0 : task.getClaimedCount();
        int totalQuota = task.getTotalQuota() == null ? 0 : task.getTotalQuota();
        if (claimedCount >= totalQuota) {
            throw new ServiceException("任务名额已满");
        }

        Date now = new Date();
        PtTaskClaim claim = new PtTaskClaim();
        claim.setTenantId(LoginHelper.getTenantId());
        claim.setTaskId(task.getTaskId());
        claim.setProfileId(profile.getProfileId());
        claim.setUserId(LoginHelper.getUserId());
        claim.setClaimStatus(CLAIM_CLAIMED);
        claim.setClaimTime(now);
        ptTaskClaimMapper.insert(claim);

        taskMaterialAssignmentService.assignForClaim(task, claim, claimedCount + 1);

        task.setClaimedCount(claimedCount + 1);
        ptPromotionTaskMapper.updateById(task);
        return toClaimVo(claim);
    }

    @Override
    public PtTaskClaimVo queryMyClaimById(Long claimId) {
        PtTaskClaim claim = getClaim(claimId);
        if (!LoginHelper.getUserId().equals(claim.getUserId())) {
            throw new ServiceException("只能查看自己的领取任务");
        }
        return toClaimVo(claim);
    }

    @Override
    public TableDataInfo<PtTaskClaimVo> queryMyClaimPage(PageQuery pageQuery) {
        LambdaQueryWrapper<PtTaskClaim> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskClaim::getUserId, LoginHelper.getUserId());
        lqw.orderByDesc(PtTaskClaim::getCreateTime);
        Page<PtTaskClaim> page = ptTaskClaimMapper.selectPage(pageQuery.build(), lqw);
        List<PtTaskClaimVo> rows = page.getRecords().stream().map(this::toClaimVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtTaskSubmissionVo submitContent(PtTaskSubmissionBo bo) {
        if (bo.getClaimId() == null) {
            throw new ServiceException("领取记录ID不能为空");
        }
        PtTaskClaim claim = getClaim(bo.getClaimId());
        if (!LoginHelper.getUserId().equals(claim.getUserId())) {
            throw new ServiceException("只能提交自己的领取任务");
        }
        if (CLAIM_APPROVED.equals(claim.getClaimStatus())) {
            throw new ServiceException("该任务已审核通过，不能重复提交");
        }
        if (hasPendingSubmission(claim.getClaimId())) {
            throw new ServiceException("已有待审核作品，请等待后台审核");
        }
        PtPromotionTask task = getTask(claim.getTaskId());
        if (!TASK_PUBLISHED.equals(task.getTaskStatus())) {
            throw new ServiceException("任务当前不可提交");
        }

        String contentUrl = normalizeContentUrl(bo.getContentUrl());
        Date now = new Date();
        PtTaskSubmission submission = new PtTaskSubmission();
        submission.setTenantId(LoginHelper.getTenantId());
        submission.setClaimId(claim.getClaimId());
        submission.setTaskId(claim.getTaskId());
        submission.setProfileId(claim.getProfileId());
        submission.setUserId(claim.getUserId());
        submission.setPlatform(defaultPlatform(task.getPlatform()));
        submission.setContentUrl(contentUrl);
        submission.setContentDesc(bo.getContentDesc());
        submission.setScreenshotUrl(bo.getScreenshotUrl());
        submission.setSubmissionStatus(SUBMISSION_PENDING);
        submission.setSubmitTime(now);
        submission.setRemark(bo.getRemark());
        ptTaskSubmissionMapper.insert(submission);

        claim.setClaimStatus(CLAIM_SUBMITTED);
        claim.setSubmitTime(now);
        ptTaskClaimMapper.updateById(claim);

        task.setSubmittedCount((task.getSubmittedCount() == null ? 0 : task.getSubmittedCount()) + 1);
        ptPromotionTaskMapper.updateById(task);
        return toSubmissionVo(submission);
    }

    @Override
    public TableDataInfo<PtTaskSubmissionVo> querySubmissionPage(PtTaskSubmission query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtTaskSubmission> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getTaskId() != null, PtTaskSubmission::getTaskId, query.getTaskId());
        lqw.eq(query.getUserId() != null, PtTaskSubmission::getUserId, query.getUserId());
        lqw.eq(StringUtils.isNotBlank(query.getPlatform()), PtTaskSubmission::getPlatform, query.getPlatform());
        lqw.eq(StringUtils.isNotBlank(query.getSubmissionStatus()), PtTaskSubmission::getSubmissionStatus, query.getSubmissionStatus());
        lqw.like(StringUtils.isNotBlank(query.getContentUrl()), PtTaskSubmission::getContentUrl, query.getContentUrl());
        lqw.orderByDesc(PtTaskSubmission::getSubmitTime);
        Page<PtTaskSubmission> page = ptTaskSubmissionMapper.selectPage(pageQuery.build(), lqw);
        List<PtTaskSubmissionVo> rows = page.getRecords().stream().map(this::toSubmissionVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PtTaskSubmissionVo querySubmissionById(Long submissionId) {
        return toSubmissionVo(getSubmission(submissionId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtTaskSubmissionVo approveSubmission(Long submissionId) {
        PtTaskSubmission submission = getSubmission(submissionId);
        if (!SUBMISSION_PENDING.equals(submission.getSubmissionStatus())) {
            throw new ServiceException("只能审核待审核作品");
        }
        PtPromotionTask task = getTask(submission.getTaskId());
        ContentLinkCreateBo bo = new ContentLinkCreateBo();
        bo.setPlatform(defaultPlatform(submission.getPlatform()));
        bo.setContentInput(submission.getContentUrl());
        bo.setTargetName(task.getTaskTitle());
        bo.setRemark("兼职任务审核通过自动监测，任务ID：" + task.getTaskId() + "，提交ID：" + submission.getSubmissionId());
        bo.setTags("parttime,task:" + task.getTaskId());
        MonitorCreateResultVo result = creatorMonitorCommandService.addContentLinkMonitor(bo);

        Date now = new Date();
        submission.setSubmissionStatus(SUBMISSION_APPROVED);
        submission.setAuditBy(LoginHelper.getUserId());
        submission.setAuditTime(now);
        if (result.getContent() != null) {
            submission.setMonitorContentId(result.getContent().getContentId());
        }
        if (result.getTarget() != null) {
            submission.setMonitorTargetId(result.getTarget().getTargetId());
        }
        if (result.getRun() != null) {
            submission.setMonitorRunId(result.getRun().getRunId());
        }
        ptTaskSubmissionMapper.updateById(submission);

        PtTaskClaim claim = getClaim(submission.getClaimId());
        claim.setClaimStatus(CLAIM_APPROVED);
        claim.setFinishTime(now);
        ptTaskClaimMapper.updateById(claim);

        task.setApprovedCount((task.getApprovedCount() == null ? 0 : task.getApprovedCount()) + 1);
        ptPromotionTaskMapper.updateById(task);
        return toSubmissionVo(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtTaskSubmissionVo rejectSubmission(Long submissionId, String rejectReason) {
        PtTaskSubmission submission = getSubmission(submissionId);
        if (!SUBMISSION_PENDING.equals(submission.getSubmissionStatus())) {
            throw new ServiceException("只能驳回待审核作品");
        }
        if (StringUtils.isBlank(rejectReason)) {
            throw new ServiceException("请输入驳回原因");
        }
        Date now = new Date();
        submission.setSubmissionStatus(SUBMISSION_REJECTED);
        submission.setAuditBy(LoginHelper.getUserId());
        submission.setAuditTime(now);
        submission.setRejectReason(rejectReason);
        ptTaskSubmissionMapper.updateById(submission);

        PtTaskClaim claim = getClaim(submission.getClaimId());
        claim.setClaimStatus(CLAIM_REJECTED);
        ptTaskClaimMapper.updateById(claim);
        return toSubmissionVo(submission);
    }

    private PtStaffProfile getApprovedMyProfile() {
        LambdaQueryWrapper<PtStaffProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtStaffProfile::getUserId, LoginHelper.getUserId());
        lqw.last("limit 1");
        PtStaffProfile profile = ptStaffProfileMapper.selectOne(lqw);
        if (profile == null) {
            throw new ServiceException("请先完成兼职入驻资料");
        }
        if (!STAFF_APPROVED.equals(profile.getOnboardingStatus())) {
            throw new ServiceException("兼职入驻审核通过后才能领取任务");
        }
        return profile;
    }

    private PtPromotionTask getTask(Long taskId) {
        PtPromotionTask task = ptPromotionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("兼职任务不存在");
        }
        return task;
    }

    private PtPromotionTask getTaskForUpdate(Long taskId) {
        LambdaQueryWrapper<PtPromotionTask> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtPromotionTask::getTaskId, taskId);
        lqw.last("limit 1 for update");
        PtPromotionTask task = ptPromotionTaskMapper.selectOne(lqw);
        if (task == null) {
            throw new ServiceException("兼职任务不存在");
        }
        return task;
    }

    private PtTaskClaim getClaim(Long claimId) {
        PtTaskClaim claim = ptTaskClaimMapper.selectById(claimId);
        if (claim == null) {
            throw new ServiceException("领取记录不存在");
        }
        return claim;
    }

    private PtTaskSubmission getSubmission(Long submissionId) {
        PtTaskSubmission submission = ptTaskSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new ServiceException("作品提交记录不存在");
        }
        return submission;
    }

    private PtTaskClaim findClaim(Long taskId, Long userId) {
        LambdaQueryWrapper<PtTaskClaim> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskClaim::getTaskId, taskId);
        lqw.eq(PtTaskClaim::getUserId, userId);
        lqw.last("limit 1");
        return ptTaskClaimMapper.selectOne(lqw);
    }

    private boolean hasPendingSubmission(Long claimId) {
        LambdaQueryWrapper<PtTaskSubmission> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskSubmission::getClaimId, claimId);
        lqw.eq(PtTaskSubmission::getSubmissionStatus, SUBMISSION_PENDING);
        lqw.last("limit 1");
        return ptTaskSubmissionMapper.selectOne(lqw) != null;
    }

    private void ensureTaskCanClaim(PtPromotionTask task) {
        Date now = new Date();
        if (!TASK_PUBLISHED.equals(task.getTaskStatus())) {
            throw new ServiceException("任务未发布，暂不能领取");
        }
        if (task.getStartTime() != null && task.getStartTime().after(now)) {
            throw new ServiceException("任务尚未开始");
        }
        if (task.getEndTime() != null && task.getEndTime().before(now)) {
            throw new ServiceException("任务已截止");
        }
    }

    private PtTaskClaimVo toClaimVo(PtTaskClaim claim) {
        PtTaskClaimVo vo = new PtTaskClaimVo();
        BeanUtils.copyProperties(claim, vo);
        PtPromotionTask task = ptPromotionTaskMapper.selectById(claim.getTaskId());
        if (task != null) {
            vo.setTaskTitle(task.getTaskTitle());
            vo.setPlatform(task.getPlatform());
        }
        PtStaffProfile profile = ptStaffProfileMapper.selectById(claim.getProfileId());
        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setPhone(profile.getPhone());
        }
        fillAssignment(vo, claim.getClaimId());
        return vo;
    }

    private PtTaskSubmissionVo toSubmissionVo(PtTaskSubmission submission) {
        PtTaskSubmissionVo vo = new PtTaskSubmissionVo();
        BeanUtils.copyProperties(submission, vo);
        PtPromotionTask task = ptPromotionTaskMapper.selectById(submission.getTaskId());
        if (task != null) {
            vo.setTaskTitle(task.getTaskTitle());
        }
        PtStaffProfile profile = ptStaffProfileMapper.selectById(submission.getProfileId());
        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setPhone(profile.getPhone());
            vo.setDouyinId(profile.getDouyinId());
        }
        fillAssignment(vo, submission.getClaimId());
        return vo;
    }

    private void fillAssignment(PtTaskClaimVo vo, Long claimId) {
        PtTaskMaterialAssignmentVo assignment = taskMaterialAssignmentService.queryByClaimId(claimId);
        if (assignment == null) {
            return;
        }
        vo.setAssignIndex(assignment.getAssignIndex());
        vo.setTextId(assignment.getTextId());
        vo.setAssignedText(assignment.getAssignedText());
        vo.setImageId(assignment.getImageId());
        vo.setAssignedImageUrl(assignment.getAssignedImageUrl());
        vo.setAssignedImageName(assignment.getAssignedImageName());
    }

    private void fillAssignment(PtTaskSubmissionVo vo, Long claimId) {
        PtTaskMaterialAssignmentVo assignment = taskMaterialAssignmentService.queryByClaimId(claimId);
        if (assignment == null) {
            return;
        }
        vo.setAssignIndex(assignment.getAssignIndex());
        vo.setTextId(assignment.getTextId());
        vo.setAssignedText(assignment.getAssignedText());
        vo.setImageId(assignment.getImageId());
        vo.setAssignedImageUrl(assignment.getAssignedImageUrl());
        vo.setAssignedImageName(assignment.getAssignedImageName());
    }

    private String defaultPlatform(String platform) {
        return StringUtils.isBlank(platform) ? "douyin" : platform;
    }

    private String normalizeContentUrl(String input) {
        String value = input == null ? "" : input.trim();
        if (StringUtils.isBlank(value)) {
            throw new ServiceException("作品链接不能为空");
        }
        Matcher matcher = URL_PATTERN.matcher(value);
        if (matcher.find()) {
            return trimUrl(matcher.group());
        }
        return value;
    }

    private String trimUrl(String url) {
        return url.replaceAll("[，。；;、）)\\]】>]+$", "");
    }
}
