package org.dromara.creator.service;

import org.dromara.creator.domain.*;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * Data service for creator monitor core tables.
 */
public interface ICreatorMonitorDataService {

    CmCreatorAccount queryCreatorById(Long creatorId);

    CmCreatorAccount queryCreatorByPlatformId(String tenantId, String platform, String platformCreatorId);

    CmContentPost queryContentById(Long contentId);

    CmContentPost queryContentByPlatformId(String tenantId, String platform, String platformContentId);

    CmMonitorTarget queryTargetById(Long targetId);

    CmMonitorTarget queryTargetByContentId(Long contentId);

    TableDataInfo<CmCreatorAccount> queryCreatorPage(CmCreatorAccount query, PageQuery pageQuery);

    TableDataInfo<CmContentPost> queryContentPage(CmContentPost query, PageQuery pageQuery);

    TableDataInfo<CmMonitorTarget> queryMonitorTargetPage(CmMonitorTarget query, PageQuery pageQuery);

    List<CmContentSnapshot> queryRecentContentSnapshots(Long contentId, int limit);

    TableDataInfo<CmCollectionRun> queryCollectionRunPage(Long targetId, PageQuery pageQuery);

    List<CmCollectionRun> queryRecentCollectionRuns(Long targetId, int limit);

    List<CmCollectionRun> queryRecentContentRuns(Long contentId, int limit);

    List<CmMonitorTarget> queryActiveTargetsDueForMetrics(String tenantId, int limit);

    Boolean saveCreator(CmCreatorAccount creatorAccount);

    Boolean saveContent(CmContentPost contentPost);

    Boolean saveMonitorTarget(CmMonitorTarget monitorTarget);

    Boolean bindTargetContent(CmMonitorTargetContent relation);

    Boolean saveCreatorSnapshot(CmCreatorSnapshot snapshot);

    Boolean saveContentSnapshot(CmContentSnapshot snapshot);

    Boolean saveCollectionRun(CmCollectionRun collectionRun);

    Boolean saveApiCallLog(CmApiCallLog apiCallLog);

    Boolean deleteTargetsByIds(Collection<Long> targetIds);
}
