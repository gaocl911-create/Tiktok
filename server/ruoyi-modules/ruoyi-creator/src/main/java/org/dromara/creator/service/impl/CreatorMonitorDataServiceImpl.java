package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.*;
import org.dromara.creator.mapper.*;
import org.dromara.creator.service.ICreatorMonitorDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data service for creator monitor core tables.
 */
@RequiredArgsConstructor
@Service
public class CreatorMonitorDataServiceImpl implements ICreatorMonitorDataService {

    private final CmCreatorAccountMapper creatorAccountMapper;
    private final CmContentPostMapper contentPostMapper;
    private final CmMonitorTargetMapper monitorTargetMapper;
    private final CmMonitorTargetContentMapper targetContentMapper;
    private final CmCreatorSnapshotMapper creatorSnapshotMapper;
    private final CmContentSnapshotMapper contentSnapshotMapper;
    private final CmCollectionRunMapper collectionRunMapper;
    private final CmApiCallLogMapper apiCallLogMapper;

    @Override
    public CmCreatorAccount queryCreatorById(Long creatorId) {
        return creatorAccountMapper.selectScopedById(creatorId);
    }

    @Override
    public CmCreatorAccount queryCreatorByPlatformId(String tenantId, String platform, String platformCreatorId) {
        LambdaQueryWrapper<CmCreatorAccount> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(tenantId), CmCreatorAccount::getTenantId, tenantId);
        lqw.eq(CmCreatorAccount::getPlatform, platform);
        lqw.eq(CmCreatorAccount::getPlatformCreatorId, platformCreatorId);
        return creatorAccountMapper.selectOne(lqw);
    }

    @Override
    public CmContentPost queryContentById(Long contentId) {
        return contentPostMapper.selectScopedById(contentId);
    }

    @Override
    public CmContentPost queryContentByPlatformId(String tenantId, String platform, String platformContentId) {
        LambdaQueryWrapper<CmContentPost> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(tenantId), CmContentPost::getTenantId, tenantId);
        lqw.eq(CmContentPost::getPlatform, platform);
        lqw.eq(CmContentPost::getPlatformContentId, platformContentId);
        return contentPostMapper.selectOne(lqw);
    }

    @Override
    public CmMonitorTarget queryTargetById(Long targetId) {
        return monitorTargetMapper.selectScopedOne(
            Wrappers.<CmMonitorTarget>lambdaQuery().eq(CmMonitorTarget::getTargetId, targetId));
    }

    @Override
    public CmMonitorTarget queryTargetByContentId(Long contentId) {
        LambdaQueryWrapper<CmMonitorTarget> directTargetQuery = Wrappers.lambdaQuery();
        directTargetQuery.eq(CmMonitorTarget::getContentId, contentId);
        directTargetQuery.eq(CmMonitorTarget::getStatus, "active");
        directTargetQuery.last("limit 1");
        CmMonitorTarget directTarget = monitorTargetMapper.selectScopedOne(directTargetQuery);
        if (directTarget != null) {
            return directTarget;
        }

        LambdaQueryWrapper<CmMonitorTargetContent> relationQuery = Wrappers.lambdaQuery();
        relationQuery.eq(CmMonitorTargetContent::getContentId, contentId);
        relationQuery.eq(CmMonitorTargetContent::getStatus, "active");
        relationQuery.orderByDesc(CmMonitorTargetContent::getFirstBoundAt);
        relationQuery.last("limit 50");
        for (CmMonitorTargetContent relation : targetContentMapper.selectList(relationQuery)) {
            CmMonitorTarget target = monitorTargetMapper.selectScopedOne(
                Wrappers.<CmMonitorTarget>lambdaQuery()
                    .eq(CmMonitorTarget::getTargetId, relation.getTargetId())
                    .eq(CmMonitorTarget::getStatus, "active"));
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    @Override
    public TableDataInfo<CmCreatorAccount> queryCreatorPage(CmCreatorAccount query, PageQuery pageQuery) {
        Page<CmCreatorAccount> page = creatorAccountMapper.selectScopedPage(pageQuery.build(), query);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<CmContentPost> queryContentPage(CmContentPost query, PageQuery pageQuery) {
        Page<CmContentPost> page = contentPostMapper.selectScopedPage(pageQuery.build(), query);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<CmMonitorTarget> queryMonitorTargetPage(CmMonitorTarget query, PageQuery pageQuery) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(query.getTenantId()), CmMonitorTarget::getTenantId, query.getTenantId());
        lqw.eq(StringUtils.isNotBlank(query.getTargetType()), CmMonitorTarget::getTargetType, query.getTargetType());
        lqw.eq(StringUtils.isNotBlank(query.getPlatform()), CmMonitorTarget::getPlatform, query.getPlatform());
        lqw.eq(query.getOwnerUserId() != null, CmMonitorTarget::getOwnerUserId, query.getOwnerUserId());
        lqw.eq(query.getOwnerDeptId() != null, CmMonitorTarget::getOwnerDeptId, query.getOwnerDeptId());
        lqw.eq(query.getCreatorId() != null, CmMonitorTarget::getCreatorId, query.getCreatorId());
        lqw.eq(query.getContentId() != null, CmMonitorTarget::getContentId, query.getContentId());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), CmMonitorTarget::getStatus, query.getStatus());
        lqw.eq(StringUtils.isNotBlank(query.getDataStatus()), CmMonitorTarget::getDataStatus, query.getDataStatus());
        lqw.like(StringUtils.isNotBlank(query.getTargetName()), CmMonitorTarget::getTargetName, query.getTargetName());
        lqw.orderByDesc(CmMonitorTarget::getUpdateTime);
        Page<CmMonitorTarget> page = monitorTargetMapper.selectScopedPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CmContentSnapshot> queryRecentContentSnapshots(Long contentId, int limit) {
        requireVisibleContent(contentId);
        LambdaQueryWrapper<CmContentSnapshot> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmContentSnapshot::getContentId, contentId);
        lqw.orderByDesc(CmContentSnapshot::getCollectedAt);
        lqw.last("limit " + normalizedLimit(limit, 30, 1000));
        return contentSnapshotMapper.selectList(lqw);
    }

    @Override
    public TableDataInfo<CmCollectionRun> queryCollectionRunPage(Long targetId, PageQuery pageQuery) {
        requireVisibleTarget(targetId);
        LambdaQueryWrapper<CmCollectionRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmCollectionRun::getTargetId, targetId);
        lqw.orderByDesc(CmCollectionRun::getStartedAt);
        Page<CmCollectionRun> page = collectionRunMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CmCollectionRun> queryRecentCollectionRuns(Long targetId, int limit) {
        requireVisibleTarget(targetId);
        LambdaQueryWrapper<CmCollectionRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(targetId != null, CmCollectionRun::getTargetId, targetId);
        lqw.orderByDesc(CmCollectionRun::getStartedAt);
        lqw.last("limit " + normalizedLimit(limit, 30, 500));
        return collectionRunMapper.selectList(lqw);
    }

    @Override
    public List<CmCollectionRun> queryRecentContentRuns(Long contentId, int limit) {
        requireVisibleContent(contentId);
        LambdaQueryWrapper<CmCollectionRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmCollectionRun::getContentId, contentId);
        lqw.orderByDesc(CmCollectionRun::getStartedAt);
        lqw.last("limit " + normalizedLimit(limit, 30, 500));
        return collectionRunMapper.selectList(lqw);
    }

    @Override
    public List<CmMonitorTarget> queryActiveTargetsDueForMetrics(String tenantId, int limit) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(tenantId), CmMonitorTarget::getTenantId, tenantId);
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.le(CmMonitorTarget::getNextContentCollectAt, new Date());
        lqw.orderByAsc(CmMonitorTarget::getNextContentCollectAt);
        lqw.last(limit > 0, "limit " + limit);
        return monitorTargetMapper.selectList(lqw);
    }

    @Override
    public Boolean saveCreator(CmCreatorAccount creatorAccount) {
        if (creatorAccount.getCreatorId() == null) {
            return creatorAccountMapper.insert(creatorAccount) > 0;
        }
        return creatorAccountMapper.updateById(creatorAccount) > 0;
    }

    @Override
    public Boolean saveContent(CmContentPost contentPost) {
        if (contentPost.getContentId() == null) {
            return contentPostMapper.insert(contentPost) > 0;
        }
        return contentPostMapper.updateById(contentPost) > 0;
    }

    @Override
    public Boolean saveMonitorTarget(CmMonitorTarget monitorTarget) {
        if (monitorTarget.getTargetId() == null) {
            return monitorTargetMapper.insert(monitorTarget) > 0;
        }
        return monitorTargetMapper.updateById(monitorTarget) > 0;
    }

    @Override
    public Boolean bindTargetContent(CmMonitorTargetContent relation) {
        if (relation.getId() == null) {
            return targetContentMapper.insert(relation) > 0;
        }
        return targetContentMapper.updateById(relation) > 0;
    }

    @Override
    public Boolean saveCreatorSnapshot(CmCreatorSnapshot snapshot) {
        return creatorSnapshotMapper.insert(snapshot) > 0;
    }

    @Override
    public Boolean saveContentSnapshot(CmContentSnapshot snapshot) {
        return contentSnapshotMapper.insert(snapshot) > 0;
    }

    @Override
    public Boolean saveCollectionRun(CmCollectionRun collectionRun) {
        if (collectionRun.getRunId() == null) {
            return collectionRunMapper.insert(collectionRun) > 0;
        }
        return collectionRunMapper.updateById(collectionRun) > 0;
    }

    @Override
    public Boolean saveApiCallLog(CmApiCallLog apiCallLog) {
        return apiCallLogMapper.insert(apiCallLog) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTargetsByIds(Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return false;
        }
        Set<Long> requestedIds = Set.copyOf(targetIds);
        Set<Long> visibleIds = monitorTargetMapper.selectScopedList(
                Wrappers.<CmMonitorTarget>lambdaQuery()
                    .select(CmMonitorTarget::getTargetId)
                    .in(CmMonitorTarget::getTargetId, requestedIds))
            .stream()
            .map(CmMonitorTarget::getTargetId)
            .collect(Collectors.toSet());
        if (!visibleIds.equals(requestedIds)) {
            throw new ServiceException("monitor target not found or access denied.");
        }
        return monitorTargetMapper.deleteByIds(visibleIds) > 0;
    }

    private CmMonitorTarget requireVisibleTarget(Long targetId) {
        CmMonitorTarget target = queryTargetById(targetId);
        if (target == null) {
            throw new ServiceException("monitor target not found or access denied.");
        }
        return target;
    }

    private CmContentPost requireVisibleContent(Long contentId) {
        CmContentPost content = queryContentById(contentId);
        if (content == null) {
            throw new ServiceException("content not found or access denied.");
        }
        return content;
    }

    private int normalizedLimit(int limit, int defaultLimit, int maxLimit) {
        if (limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }
}
