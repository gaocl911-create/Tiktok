package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.bo.PtPromotionTaskBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;
import org.dromara.creator.mapper.PtPromotionTaskMapper;
import org.dromara.creator.service.IPtPromotionTaskService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@RequiredArgsConstructor
@Service
public class PtPromotionTaskServiceImpl implements IPtPromotionTaskService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_PAUSED = "paused";
    private static final String STATUS_FINISHED = "finished";

    private final PtPromotionTaskMapper ptPromotionTaskMapper;

    @Override
    public TableDataInfo<PtPromotionTask> queryTaskPage(PtPromotionTask query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtPromotionTask> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getTenantId() != null, PtPromotionTask::getTenantId, query.getTenantId());
        lqw.eq(query.getPlatform() != null && !query.getPlatform().isBlank(), PtPromotionTask::getPlatform, query.getPlatform());
        lqw.eq(query.getTaskStatus() != null && !query.getTaskStatus().isBlank(), PtPromotionTask::getTaskStatus, query.getTaskStatus());
        lqw.like(query.getTaskTitle() != null && !query.getTaskTitle().isBlank(), PtPromotionTask::getTaskTitle, query.getTaskTitle());
        lqw.orderByDesc(PtPromotionTask::getCreateTime);
        Page<PtPromotionTask> page = ptPromotionTaskMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
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
        if (task.getClaimedCount() != null && task.getClaimedCount() > 0 && bo.getTotalQuota() < task.getClaimedCount()) {
            throw new ServiceException("任务名额不能小于已领取数量");
        }
        copyBo(task, bo);
        task.setPlatform(defaultPlatform(bo.getPlatform()));
        ptPromotionTaskMapper.updateById(task);
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
        task.setStartTime(bo.getStartTime());
        task.setEndTime(bo.getEndTime());
        task.setRemark(bo.getRemark());
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
        if (task.getTotalQuota() == null || task.getTotalQuota() <= 0) {
            throw new ServiceException("任务名额必须大于0");
        }
        if (task.getEndTime() != null && task.getEndTime().before(new Date())) {
            throw new ServiceException("截止时间已过，不能发布任务");
        }
    }

    private PtPromotionTaskVo toVo(PtPromotionTask task) {
        PtPromotionTaskVo vo = new PtPromotionTaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
