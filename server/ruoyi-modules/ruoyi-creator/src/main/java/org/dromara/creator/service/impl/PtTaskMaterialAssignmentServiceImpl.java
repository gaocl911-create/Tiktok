package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.creator.domain.*;
import org.dromara.creator.domain.vo.PtTaskMaterialAssignmentVo;
import org.dromara.creator.mapper.PtMaterialImageMapper;
import org.dromara.creator.mapper.PtMaterialTextMapper;
import org.dromara.creator.mapper.PtTaskMaterialAssignmentMapper;
import org.dromara.creator.mapper.PtTaskMaterialConfigMapper;
import org.dromara.creator.service.IPtTaskMaterialAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PtTaskMaterialAssignmentServiceImpl implements IPtTaskMaterialAssignmentService {

    private static final String ASSIGN_MODE_SEQUENCE_LOOP = "sequence_loop";
    private static final String STATUS_ENABLED = "0";

    private final PtTaskMaterialConfigMapper ptTaskMaterialConfigMapper;
    private final PtTaskMaterialAssignmentMapper ptTaskMaterialAssignmentMapper;
    private final PtMaterialTextMapper ptMaterialTextMapper;
    private final PtMaterialImageMapper ptMaterialImageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtTaskMaterialAssignmentVo assignForClaim(PtPromotionTask task, PtTaskClaim claim, int assignIndex) {
        PtTaskMaterialAssignment existing = queryAssignment(claim.getClaimId());
        if (existing != null) {
            return toVo(existing);
        }
        PtTaskMaterialConfig config = queryConfig(task.getTaskId());
        if (config == null || config.getTextCategoryId() == null || config.getImageCategoryId() == null) {
            throw new ServiceException("任务未配置文案分类和图片分类，暂不能领取");
        }

        List<PtMaterialText> texts = queryEnabledTexts(config.getTextCategoryId());
        if (texts.isEmpty()) {
            throw new ServiceException("任务绑定的文案分类暂无可用文案");
        }
        List<PtMaterialImage> images = queryEnabledImages(config.getImageCategoryId());
        if (images.isEmpty()) {
            throw new ServiceException("任务绑定的图片分类暂无可用图片");
        }

        int safeIndex = Math.max(assignIndex, 1);
        PtMaterialText selectedText = texts.get((safeIndex - 1) % texts.size());
        PtMaterialImage selectedImage = images.get((safeIndex - 1) % images.size());

        PtTaskMaterialAssignment assignment = new PtTaskMaterialAssignment();
        assignment.setTenantId(claim.getTenantId());
        assignment.setTaskId(task.getTaskId());
        assignment.setClaimId(claim.getClaimId());
        assignment.setUserId(claim.getUserId());
        assignment.setAssignIndex(safeIndex);
        assignment.setTextId(selectedText.getTextId());
        assignment.setTextSnapshot(selectedText.getContent());
        assignment.setImageId(selectedImage.getImageId());
        assignment.setImageUrlSnapshot(selectedImage.getImageUrl());
        assignment.setImageNameSnapshot(selectedImage.getImageName());
        ptTaskMaterialAssignmentMapper.insert(assignment);
        return toVo(assignment);
    }

    @Override
    public PtTaskMaterialAssignmentVo queryByClaimId(Long claimId) {
        PtTaskMaterialAssignment assignment = queryAssignment(claimId);
        return assignment == null ? null : toVo(assignment);
    }

    private PtTaskMaterialConfig queryConfig(Long taskId) {
        LambdaQueryWrapper<PtTaskMaterialConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskMaterialConfig::getTaskId, taskId);
        lqw.last("limit 1");
        return ptTaskMaterialConfigMapper.selectOne(lqw);
    }

    private PtTaskMaterialAssignment queryAssignment(Long claimId) {
        LambdaQueryWrapper<PtTaskMaterialAssignment> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtTaskMaterialAssignment::getClaimId, claimId);
        lqw.last("limit 1");
        return ptTaskMaterialAssignmentMapper.selectOne(lqw);
    }

    private List<PtMaterialText> queryEnabledTexts(Long categoryId) {
        LambdaQueryWrapper<PtMaterialText> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtMaterialText::getCategoryId, categoryId);
        lqw.eq(PtMaterialText::getStatus, STATUS_ENABLED);
        lqw.orderByAsc(PtMaterialText::getSort, PtMaterialText::getTextId);
        return ptMaterialTextMapper.selectList(lqw);
    }

    private List<PtMaterialImage> queryEnabledImages(Long categoryId) {
        LambdaQueryWrapper<PtMaterialImage> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtMaterialImage::getCategoryId, categoryId);
        lqw.eq(PtMaterialImage::getStatus, STATUS_ENABLED);
        lqw.orderByAsc(PtMaterialImage::getSort, PtMaterialImage::getImageId);
        return ptMaterialImageMapper.selectList(lqw);
    }

    private PtTaskMaterialAssignmentVo toVo(PtTaskMaterialAssignment assignment) {
        PtTaskMaterialAssignmentVo vo = new PtTaskMaterialAssignmentVo();
        vo.setAssignmentId(assignment.getAssignmentId());
        vo.setTenantId(assignment.getTenantId());
        vo.setTaskId(assignment.getTaskId());
        vo.setClaimId(assignment.getClaimId());
        vo.setUserId(assignment.getUserId());
        vo.setAssignIndex(assignment.getAssignIndex());
        vo.setTextId(assignment.getTextId());
        vo.setAssignedText(assignment.getTextSnapshot());
        vo.setImageId(assignment.getImageId());
        vo.setAssignedImageUrl(assignment.getImageUrlSnapshot());
        vo.setAssignedImageName(assignment.getImageNameSnapshot());
        vo.setCreateTime(assignment.getCreateTime());
        return vo;
    }
}
