package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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
        return creatorAccountMapper.selectById(creatorId);
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
        return contentPostMapper.selectById(contentId);
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
        return monitorTargetMapper.selectById(targetId);
    }

    @Override
    public TableDataInfo<CmCreatorAccount> queryCreatorPage(CmCreatorAccount query, PageQuery pageQuery) {
        LambdaQueryWrapper<CmCreatorAccount> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(query.getTenantId()), CmCreatorAccount::getTenantId, query.getTenantId());
        lqw.eq(StringUtils.isNotBlank(query.getPlatform()), CmCreatorAccount::getPlatform, query.getPlatform());
        lqw.eq(StringUtils.isNotBlank(query.getPlatformCreatorId()), CmCreatorAccount::getPlatformCreatorId, query.getPlatformCreatorId());
        lqw.like(StringUtils.isNotBlank(query.getNickname()), CmCreatorAccount::getNickname, query.getNickname());
        lqw.eq(StringUtils.isNotBlank(query.getProfileStatus()), CmCreatorAccount::getProfileStatus, query.getProfileStatus());
        lqw.orderByDesc(CmCreatorAccount::getUpdateTime);
        Page<CmCreatorAccount> page = creatorAccountMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<CmContentPost> queryContentPage(CmContentPost query, PageQuery pageQuery) {
        LambdaQueryWrapper<CmContentPost> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(query.getTenantId()), CmContentPost::getTenantId, query.getTenantId());
        lqw.eq(StringUtils.isNotBlank(query.getPlatform()), CmContentPost::getPlatform, query.getPlatform());
        lqw.eq(query.getCreatorId() != null, CmContentPost::getCreatorId, query.getCreatorId());
        lqw.eq(StringUtils.isNotBlank(query.getPlatformContentId()), CmContentPost::getPlatformContentId, query.getPlatformContentId());
        lqw.eq(StringUtils.isNotBlank(query.getAddedSource()), CmContentPost::getAddedSource, query.getAddedSource());
        lqw.eq(StringUtils.isNotBlank(query.getMetricsStatus()), CmContentPost::getMetricsStatus, query.getMetricsStatus());
        lqw.and(StringUtils.isNotBlank(query.getTitle()), wrapper -> wrapper
            .like(CmContentPost::getTitle, query.getTitle())
            .or()
            .like(CmContentPost::getDescription, query.getTitle()));
        lqw.orderByDesc(CmContentPost::getPublishTime, CmContentPost::getFirstSeenAt);
        Page<CmContentPost> page = contentPostMapper.selectPage(pageQuery.build(), lqw);
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
        Page<CmMonitorTarget> page = monitorTargetMapper.selectPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CmContentSnapshot> queryRecentContentSnapshots(Long contentId, int limit) {
        LambdaQueryWrapper<CmContentSnapshot> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmContentSnapshot::getContentId, contentId);
        lqw.orderByDesc(CmContentSnapshot::getCollectedAt);
        lqw.last(limit > 0, "limit " + limit);
        return contentSnapshotMapper.selectList(lqw);
    }

    @Override
    public List<CmCollectionRun> queryRecentCollectionRuns(Long targetId, int limit) {
        LambdaQueryWrapper<CmCollectionRun> lqw = Wrappers.lambdaQuery();
        lqw.eq(targetId != null, CmCollectionRun::getTargetId, targetId);
        lqw.orderByDesc(CmCollectionRun::getStartedAt);
        lqw.last(limit > 0, "limit " + limit);
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
        return monitorTargetMapper.deleteByIds(targetIds) > 0;
    }
}
