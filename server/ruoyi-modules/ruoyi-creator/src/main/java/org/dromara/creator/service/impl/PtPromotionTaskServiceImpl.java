package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.*;
import org.dromara.creator.domain.bo.PtPromotionTaskBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.mapper.*;
import org.dromara.creator.service.IPtPromotionTaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PtPromotionTaskServiceImpl implements IPtPromotionTaskService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_PAUSED = "paused";
    private static final String STATUS_FINISHED = "finished";
    private static final String CLAIM_LIMIT_ONCE = "once";
    private static final String CLAIM_LIMIT_LIMITED = "limited";
    private static final String CLAIM_LIMIT_UNLIMITED = "unlimited";
    private static final String CATEGORY_TEXT = "text";
    private static final String CATEGORY_IMAGE = "image";
    private static final String MATERIAL_ENABLED = "0";
    private static final String ASSIGN_MODE_SEQUENCE_LOOP = "sequence_loop";

    private final PtPromotionTaskMapper ptPromotionTaskMapper;
    private final PtTaskMaterialConfigMapper ptTaskMaterialConfigMapper;
    private final PtMaterialCategoryMapper ptMaterialCategoryMapper;
    private final PtMaterialTextMapper ptMaterialTextMapper;
    private final PtMaterialImageMapper ptMaterialImageMapper;

    @Override
    public TableDataInfo<PtPromotionTaskVo> queryTaskPage(PtPromotionTask query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtPromotionTask> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getTenantId() != null, PtPromotionTask::getTenantId, query.getTenantId());
        lqw.eq(query.getPlatform() != null && !query.getPlatform().isBlank(), PtPromotionTask::getPlatform, query.getPlatform());
        lqw.eq(query.getTaskStatus() != null && !query.getTaskStatus().isBlank(), PtPromotionTask::getTaskStatus, query.getTaskStatus());
        lqw.like(query.getTaskTitle() != null && !query.getTaskTitle().isBlank(), PtPromotionTask::getTaskTitle, query.getTaskTitle());
        lqw.orderByDesc(PtPromotionTask::getCreateTime);
        Page<PtPromotionTask> page = ptPromotionTaskMapper.selectPage(pageQuery.build(), lqw);
        List<PtPromotionTaskVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PtPromotionTaskVo queryById(Long taskId) {
        PtPromotionTask task = getRequired(taskId);
        return toVo(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtPromotionTaskVo createTask(PtPromotionTaskBo bo) {
        validateTimeRange(bo);
        PtPromotionTask task = new PtPromotionTask();
        copyBo(task, bo);
        task.setPlatform(defaultPlatform(bo.getPlatform()));
        task.setTaskStatus(STATUS_DRAFT);
        task.setClaimedCount(0);
        task.setSubmittedCount(0);
        task.setApprovedCount(0);
        ptPromotionTaskMapper.insert(task);
        saveMaterialConfig(task.getTaskId(), bo);
        return toVo(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtPromotionTaskVo updateTask(PtPromotionTaskBo bo) {
        if (bo.getTaskId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        validateTimeRange(bo);
        PtPromotionTask task = getRequired(bo.getTaskId());
        if (!STATUS_DRAFT.equals(task.getTaskStatus()) && !STATUS_PAUSED.equals(task.getTaskStatus())) {
            throw new ServiceException("仅草稿或已暂停任务允许编辑，请先暂停任务");
        }
        int approvedCount = task.getApprovedCount() == null ? 0 : task.getApprovedCount();
        if (bo.getTotalQuota() != null && bo.getTotalQuota() > 0 && bo.getTotalQuota() < approvedCount) {
            throw new ServiceException("任务名额不能小于已通过数量");
        }
        copyBo(task, bo);
        task.setPlatform(defaultPlatform(bo.getPlatform()));
        ptPromotionTaskMapper.updateById(task);
        saveMaterialConfig(task.getTaskId(), bo);
        return toVo(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtPromotionTaskVo publish(Long taskId) {
        PtPromotionTask task = getRequired(taskId);
        if (!STATUS_DRAFT.equals(task.getTaskStatus()) && !STATUS_PAUSED.equals(task.getTaskStatus())) {
            throw new ServiceException("仅草稿或已暂停任务允许发布");
        }
        validatePublishable(task);
        task.setTaskStatus(STATUS_PUBLISHED);
        task.setPublishTime(new Date());
        task.setPauseTime(null);
        task.setFinishTime(null);
        ptPromotionTaskMapper.updateById(task);
        return toVo(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtPromotionTaskVo pause(Long taskId) {
        PtPromotionTask task = getRequired(taskId);
        if (!STATUS_PUBLISHED.equals(task.getTaskStatus())) {
            throw new ServiceException("仅已发布任务允许暂停");
        }
        task.setTaskStatus(STATUS_PAUSED);
        task.setPauseTime(new Date());
        ptPromotionTaskMapper.updateById(task);
        return toVo(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtPromotionTaskVo finish(Long taskId) {
        PtPromotionTask task = getRequired(taskId);
        if (!STATUS_PUBLISHED.equals(task.getTaskStatus()) && !STATUS_PAUSED.equals(task.getTaskStatus())) {
            throw new ServiceException("仅已发布或已暂停任务允许结束");
        }
        task.setTaskStatus(STATUS_FINISHED);
        task.setFinishTime(new Date());
        ptPromotionTaskMapper.updateById(task);
        return toVo(task);
    }

    private PtPromotionTask getRequired(Long taskId) {
        PtPromotionTask task = ptPromotionTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("兼职任务不存在");
        }
        return task;
    }

    private void copyBo(PtPromotionTask task, PtPromotionTaskBo bo) {
        task.setTaskTitle(bo.getTaskTitle());
        task.setTaskDesc(bo.getTaskDesc());
        task.setTaskRequirement(bo.getTaskRequirement());
        task.setUnitPrice(bo.getUnitPrice() == null ? BigDecimal.ZERO : bo.getUnitPrice());
        task.setTotalQuota(bo.getTotalQuota());
        applyClaimLimit(task, bo);
        task.setStartTime(bo.getStartTime());
        task.setEndTime(bo.getEndTime());
        task.setRemark(bo.getRemark());
    }

    private void applyClaimLimit(PtPromotionTask task, PtPromotionTaskBo bo) {
        String limitType = bo.getClaimLimitType();
        if (limitType == null || limitType.isBlank()) {
            limitType = CLAIM_LIMIT_ONCE;
        }
        if (!CLAIM_LIMIT_ONCE.equals(limitType)
            && !CLAIM_LIMIT_LIMITED.equals(limitType)
            && !CLAIM_LIMIT_UNLIMITED.equals(limitType)) {
            throw new ServiceException("每人领取限制配置不正确");
        }
        int limitCount = bo.getClaimLimitCount() == null ? 0 : bo.getClaimLimitCount();
        if (CLAIM_LIMIT_ONCE.equals(limitType)) {
            limitCount = 1;
        } else if (CLAIM_LIMIT_UNLIMITED.equals(limitType)) {
            limitCount = 0;
        } else if (limitCount < 1) {
            throw new ServiceException("限制领取时，每人领取次数至少为1");
        }
        task.setClaimLimitType(limitType);
        task.setClaimLimitCount(limitCount);
    }

    private String defaultPlatform(String platform) {
        return platform == null || platform.isBlank() ? "douyin" : platform;
    }

    private void validateTimeRange(PtPromotionTaskBo bo) {
        if (bo.getStartTime() != null && bo.getEndTime() != null && bo.getEndTime().before(bo.getStartTime())) {
            throw new ServiceException("截止时间不能早于开始时间");
        }
    }

    private void validatePublishable(PtPromotionTask task) {
        if (task.getTaskTitle() == null || task.getTaskTitle().isBlank()) {
            throw new ServiceException("请先填写任务标题");
        }
        if (task.getUnitPrice() == null || task.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("任务单价不能小于0");
        }
        if (task.getTotalQuota() == null || task.getTotalQuota() < 0) {
            throw new ServiceException("任务名额不能小于0");
        }
        if (CLAIM_LIMIT_LIMITED.equals(task.getClaimLimitType())
            && (task.getClaimLimitCount() == null || task.getClaimLimitCount() < 1)) {
            throw new ServiceException("限制领取时，每人领取次数至少为1");
        }
        if (task.getEndTime() != null && task.getEndTime().before(new Date())) {
            throw new ServiceException("截止时间已过，不能发布任务");
        }
        validateMaterialReady(task.getTaskId());
    }

    private void saveMaterialConfig(Long taskId, PtPromotionTaskBo bo) {
        if (bo.getTextCategoryId() == null && bo.getImageCategoryId() == null) {
            return;
        }
        if (bo.getTextCategoryId() == null || bo.getImageCategoryId() == null) {
            throw new ServiceException("文案分类和图片分类必须同时选择");
        }
        ensureCategory(bo.getTextCategoryId(), CATEGORY_TEXT);
        ensureCategory(bo.getImageCategoryId(), CATEGORY_IMAGE);

        PtTaskMaterialConfig config = queryMaterialConfig(taskId);
        if (config == null) {
            config = new PtTaskMaterialConfig();
            config.setTaskId(taskId);
            config.setAssignMode(ASSIGN_MODE_SEQUENCE_LOOP);
            config.setTextCategoryId(bo.getTextCategoryId());
            config.setImageCategoryId(bo.getImageCategoryId());
            ptTaskMaterialConfigMapper.insert(config);
            return;
        }
        config.setTextCategoryId(bo.getTextCategoryId());
        config.setImageCategoryId(bo.getImageCategoryId());
        config.setAssignMode(ASSIGN_MODE_SEQUENCE_LOOP);
        ptTaskMaterialConfigMapper.updateById(config);
    }

    private void validateMaterialReady(Long taskId) {
        PtTaskMaterialConfig config = queryMaterialConfig(taskId);
        if (config == null || config.getTextCategoryId() == null || config.getImageCategoryId() == null) {
            throw new ServiceException("请先选择文案分类和图片分类");
        }
        ensureCategory(config.getTextCategoryId(), CATEGORY_TEXT);
        ensureCategory(config.getImageCategoryId(), CATEGORY_IMAGE);
        if (countEnabledTexts(config.getTextCategoryId()) <= 0) {
            throw new ServiceException("文案分类下没有启用文案，不能发布任务");
        }
        if (countEnabledImages(config.getImageCategoryId()) <= 0) {
            throw new ServiceException("图片分类下没有启用图片，不能发布任务");
        }
    }

    private PtTaskMaterialConfig queryMaterialConfig(Long taskId) {
        LambdaQueryWrapper<PtTaskMaterialConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskMaterialConfig::getTaskId, taskId);
        lqw.last("limit 1");
        return ptTaskMaterialConfigMapper.selectOne(lqw);
    }

    private void ensureCategory(Long categoryId, String expectedType) {
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(categoryId);
        if (category == null || !expectedType.equals(category.getCategoryType()) || !MATERIAL_ENABLED.equals(category.getStatus())) {
            throw new ServiceException(CATEGORY_TEXT.equals(expectedType) ? "请选择启用的文案分类" : "请选择启用的图片分类");
        }
    }

    private long countEnabledTexts(Long categoryId) {
        LambdaQueryWrapper<PtMaterialText> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtMaterialText::getCategoryId, categoryId);
        lqw.eq(PtMaterialText::getStatus, MATERIAL_ENABLED);
        return ptMaterialTextMapper.selectCount(lqw);
    }

    private long countEnabledImages(Long categoryId) {
        LambdaQueryWrapper<PtMaterialImage> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtMaterialImage::getCategoryId, categoryId);
        lqw.eq(PtMaterialImage::getStatus, MATERIAL_ENABLED);
        return ptMaterialImageMapper.selectCount(lqw);
    }

    private PtPromotionTaskVo toVo(PtPromotionTask task) {
        PtPromotionTaskVo vo = new PtPromotionTaskVo();
        BeanUtils.copyProperties(task, vo);
        PtTaskMaterialConfig config = queryMaterialConfig(task.getTaskId());
        if (config != null) {
            vo.setTextCategoryId(config.getTextCategoryId());
            vo.setImageCategoryId(config.getImageCategoryId());
            vo.setTextCategoryName(queryCategoryName(config.getTextCategoryId()));
            vo.setImageCategoryName(queryCategoryName(config.getImageCategoryId()));
        }
        return vo;
    }

    private String queryCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }
}
