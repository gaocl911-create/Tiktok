package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.DeptService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.creator.client.*;
import org.dromara.creator.config.TikHubProperties;
import org.dromara.creator.domain.*;
import org.dromara.creator.domain.bo.ContentLinkCreateBo;
import org.dromara.creator.domain.bo.CreatorMonitorCreateBo;
import org.dromara.creator.domain.bo.MonitorTargetCreateBo;
import org.dromara.creator.domain.vo.MonitorCreateResultVo;
import org.dromara.creator.mapper.*;
import org.dromara.creator.service.ICreatorAlertService;
import org.dromara.creator.service.ICreatorMonitorCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RLock;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Business write and collection commands.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class CreatorMonitorCommandServiceImpl implements ICreatorMonitorCommandService {

    private static final String PLATFORM_DOUYIN = "douyin";
    private static final String PROVIDER_TIKHUB = "tikhub";
    private static final String TARGET_CREATOR_COLLECTION = "creator_collection";
    private static final String TARGET_SINGLE_CONTENT = "single_content";
    private static final String TARGET_COLLECT_LOCK_PREFIX = "creator:monitor:collect:";
    private static final String TARGET_COLLECT_BUSY_MESSAGE = "该监控目标正在采集中，请稍后重试";
    private static final long STALE_RUN_TIMEOUT_MS = 2 * 60 * 60 * 1000L;
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private final TikHubProperties tikHubProperties;
    private final CmCreatorAccountMapper creatorAccountMapper;
    private final CmContentPostMapper contentPostMapper;
    private final CmMonitorTargetMapper monitorTargetMapper;
    private final CmMonitorTargetContentMapper targetContentMapper;
    private final CmCreatorSnapshotMapper creatorSnapshotMapper;
    private final CmContentSnapshotMapper contentSnapshotMapper;
    private final CmCollectionRunMapper collectionRunMapper;
    private final CmApiCallLogMapper apiCallLogMapper;
    private final ICreatorAlertService creatorAlertService;
    private final DeptService deptService;

    private final TikHubDouyinMapper tikHubDouyinMapper = new TikHubDouyinMapper();

    @Override
    public MonitorCreateResultVo addCreatorMonitor(CreatorMonitorCreateBo bo) {
        assertDouyin(bo.getPlatform());
        Date startedAt = new Date();
        CmCollectionRun run = startRun("creator_profile_add", "manual", null, null, null, bo.getPlatform());
        TikHubClient client = null;
        try {
            client = newTikHubClient();
            String secUserId = resolveSecUserId(client, bo.getProfileInput());
            TikHubCreatorProfile profile = tikHubDouyinMapper.mapCreator(client.getProfile(secUserId), secUserId);
            CmCreatorAccount creator = findCreator(profile);
            boolean creatorCreated = creator == null;
            creator = upsertCreator(creator, bo.getPlatform(), profile);
            CmMonitorTarget target = findOwnedCreatorTarget(creator.getCreatorId());
            boolean targetCreated = target == null;
            target = upsertCreatorTarget(target, creator, bo, startedAt);
            saveCreatorSnapshot(creator, target.getTargetId(), profile);
            finishRunSuccess(run, target, creator, null, client, 0, 1);
            saveApiLogs(client, run);
            return result(creator, null, target, run, creatorCreated, false, targetCreated);
        } catch (RuntimeException ex) {
            finishRunFailed(run, client, ex);
            saveApiLogs(client, run);
            throw ex;
        }
    }

    @Override
    public MonitorCreateResultVo addContentLinkMonitor(ContentLinkCreateBo bo) {
        assertDouyin(bo.getPlatform());
        String shareUrl = extractDouyinUrl(bo.getContentInput());
        Date startedAt = new Date();
        CmCollectionRun run = startRun("single_content_add", "manual", null, null, null, bo.getPlatform());
        TikHubClient client = null;
        try {
            client = newTikHubClient();
            TikHubContentProfile contentProfile = tikHubDouyinMapper.mapOneContent(client.getOneVideoByShareUrl(shareUrl));
            TikHubCreatorProfile creatorProfile = contentProfile.getCreator();
            if (creatorProfile == null) {
                creatorProfile = fallbackCreatorFromContent(contentProfile);
            }
            CmCreatorAccount creator = findCreator(creatorProfile);
            boolean creatorCreated = creator == null;
            if (shouldEnrichCreatorProfile(creator, creatorProfile)) {
                String secUserId = creatorProfile.getPlatformCreatorId();
                creatorProfile = tikHubDouyinMapper.mapCreator(client.getProfile(secUserId), secUserId);
                creator = upsertCreator(creator, bo.getPlatform(), creatorProfile);
            } else if (creator == null) {
                creator = upsertCreator(null, bo.getPlatform(), creatorProfile);
            }
            CmContentPost content = findContent(bo.getPlatform(), contentProfile.getPlatformContentId());
            boolean contentCreated = content == null;
            content = upsertContent(content, creator, bo.getPlatform(), contentProfile, "manual_link");
            CmMonitorTarget target = findOwnedSingleContentTarget(content.getContentId());
            boolean targetCreated = target == null;
            target = upsertSingleContentTarget(target, creator, content, bo, startedAt);
            bindTargetContent(target, content, "manual_link", true);
            saveContentSnapshot(content, target.getTargetId(), contentProfile);
            finishRunSuccess(run, target, creator, content, client, 0, 1);
            saveApiLogs(client, run);
            return result(creator, content, target, run, creatorCreated, contentCreated, targetCreated);
        } catch (RuntimeException ex) {
            finishRunFailed(run, client, ex);
            saveApiLogs(client, run);
            throw ex;
        }
    }

    @Override
    public MonitorCreateResultVo createMonitorTarget(MonitorTargetCreateBo bo) {
        assertDouyin(bo.getPlatform());
        Date now = new Date();
        CmCreatorAccount creator = bo.getCreatorId() == null ? null : creatorAccountMapper.selectById(bo.getCreatorId());
        CmContentPost content = bo.getContentId() == null ? null : contentPostMapper.selectById(bo.getContentId());
        if (TARGET_CREATOR_COLLECTION.equals(bo.getTargetType()) && creator == null) {
            throw new ServiceException("creatorId is required for creator_collection target.");
        }
        if (TARGET_SINGLE_CONTENT.equals(bo.getTargetType()) && content == null) {
            throw new ServiceException("contentId is required for single_content target.");
        }
        CmMonitorTarget target = new CmMonitorTarget();
        target.setTargetType(bo.getTargetType());
        target.setPlatform(bo.getPlatform());
        target.setTargetName(defaultText(bo.getTargetName(), content != null ? content.getTitle() : creator.getNickname()));
        applyOwner(target, bo.getOwnerUserId(), bo.getOwnerDeptId(), bo.getDirectSuperiorUserId());
        target.setCreatorId(creator != null ? creator.getCreatorId() : content.getCreatorId());
        target.setContentId(content == null ? null : content.getContentId());
        target.setBaselineTime(bo.getBaselineTime() == null ? now : bo.getBaselineTime());
        target.setDiscoverNewContent(Boolean.TRUE.equals(bo.getDiscoverNewContent()));
        target.setProfileCollectIntervalMin(defaultInt(bo.getProfileCollectIntervalMin(), 360));
        target.setContentCollectIntervalMin(defaultInt(bo.getContentCollectIntervalMin(), 30));
        target.setNextProfileCollectAt(addMinutes(now, target.getProfileCollectIntervalMin()));
        target.setNextContentCollectAt(addMinutes(now, target.getContentCollectIntervalMin()));
        target.setNextDiscoveryAt(addMinutes(now, target.getContentCollectIntervalMin()));
        target.setStatus("active");
        target.setDataStatus("waiting_collect");
        target.setRemark(bo.getRemark());
        target.setTags(bo.getTags());
        monitorTargetMapper.insert(target);
        if (content != null) {
            bindTargetContent(target, content, "manual_target", true);
        }
        return result(creator, content, target, null, false, false, true);
    }

    @Override
    public Integer collectDueTargets(int limit, String triggerSource) {
        Date now = new Date();
        recoverStaleRuns(now);
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.and(wrapper -> wrapper
            .le(CmMonitorTarget::getNextContentCollectAt, now)
            .or()
            .le(CmMonitorTarget::getNextProfileCollectAt, now)
            .or()
            .le(CmMonitorTarget::getNextDiscoveryAt, now));
        lqw.orderByAsc(CmMonitorTarget::getNextContentCollectAt);
        lqw.last("limit " + Math.max(1, limit));
        int count = 0;
        List<CmMonitorTarget> dueTargets = LoginHelper.isLogin()
            ? monitorTargetMapper.selectScopedList(lqw)
            : monitorTargetMapper.selectList(lqw);
        for (CmMonitorTarget target : dueTargets) {
            try {
                collectTargetNow(target.getTargetId(), triggerSource);
                count++;
            } catch (ServiceException ex) {
                if (TARGET_COLLECT_BUSY_MESSAGE.equals(ex.getMessage())) {
                    continue;
                }
                log.warn("creator monitor target collection failed, targetId={}, triggerSource={}, message={}",
                    target.getTargetId(), triggerSource, ex.getMessage());
            } catch (RuntimeException ex) {
                log.warn("creator monitor target collection failed, targetId={}, triggerSource={}",
                    target.getTargetId(), triggerSource, ex);
            }
        }
        return count;
    }

    @Override
    public MonitorCreateResultVo collectTargetNow(Long targetId, String triggerSource) {
        RLock lock = RedisUtils.getClient().getLock(TARGET_COLLECT_LOCK_PREFIX + targetId);
        if (!lock.tryLock()) {
            throw new ServiceException(TARGET_COLLECT_BUSY_MESSAGE);
        }
        try {
            CmMonitorTarget target = LoginHelper.isLogin()
                ? monitorTargetMapper.selectScopedOne(
                    Wrappers.<CmMonitorTarget>lambdaQuery().eq(CmMonitorTarget::getTargetId, targetId))
                : monitorTargetMapper.selectById(targetId);
            if (target == null) {
                throw new ServiceException("monitor target not found or access denied.");
            }
            CmCreatorAccount creator = creatorAccountMapper.selectById(target.getCreatorId());
            if (creator == null) {
                throw new ServiceException("target creator not found.");
            }
            CmContentPost singleContent = null;
            if (!TARGET_CREATOR_COLLECTION.equals(target.getTargetType())) {
                singleContent = contentPostMapper.selectById(target.getContentId());
                if (singleContent == null) {
                    throw new ServiceException("target content not found.");
                }
            }
            String actualTriggerSource = defaultText(triggerSource, "manual");
            CmCollectionRun run = startRun("target_collect", actualTriggerSource, target, creator, singleContent, target.getPlatform());
            TikHubClient client = null;
            try {
                client = newTikHubClient();
                int discovered = 0;
                int collected = 0;
                if (TARGET_CREATOR_COLLECTION.equals(target.getTargetType())) {
                    discovered = collectCreatorCollection(target, creator, client, isImmediateTrigger(actualTriggerSource));
                    collected = collectBoundContentMetrics(target, creator, client, actualTriggerSource);
                } else {
                    collectSingleContent(target, singleContent, client);
                    collected = 1;
                }
                finishRunSuccess(run, target, creator, singleContent, client, discovered, collected);
                saveApiLogs(client, run);
                return result(creator, singleContent, target, run, false, false, false);
            } catch (RuntimeException ex) {
                finishRunFailed(run, client, ex);
                saveApiLogs(client, run);
                throw ex;
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public MonitorCreateResultVo collectCreatorProfileNow(Long creatorId, String triggerSource) {
        CmMonitorTarget target = LoginHelper.isLogin()
            ? findScopedCreatorTarget(creatorId)
            : findCreatorTarget(creatorId);
        if (LoginHelper.isLogin() && target == null) {
            throw new ServiceException("creator not found or access denied.");
        }
        CmCreatorAccount creator = creatorAccountMapper.selectById(creatorId);
        if (creator == null) {
            throw new ServiceException("creator not found.");
        }
        CmCollectionRun run = startRun(
            "creator_profile_collect",
            defaultText(triggerSource, "manual_profile"),
            target,
            creator,
            null,
            creator.getPlatform()
        );
        TikHubClient client = null;
        try {
            client = newTikHubClient();
            TikHubCreatorProfile profile = tikHubDouyinMapper.mapCreator(
                client.getProfile(creator.getPlatformCreatorId()),
                creator.getPlatformCreatorId()
            );
            creator = upsertCreator(creator, creator.getPlatform(), profile);
            saveCreatorSnapshot(creator, target == null ? null : target.getTargetId(), profile);
            if (target != null) {
                Date now = new Date();
                target.setLastProfileCollectAt(now);
                target.setNextProfileCollectAt(addMinutes(now, target.getProfileCollectIntervalMin()));
                monitorTargetMapper.updateById(target);
            }
            finishRunSuccess(run, target, creator, null, client, 0, 1);
            saveApiLogs(client, run);
            return result(creator, null, target, run, false, false, false);
        } catch (RuntimeException ex) {
            finishRunFailed(run, client, ex);
            saveApiLogs(client, run);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCreators(Collection<Long> creatorIds) {
        if (creatorIds == null || creatorIds.isEmpty()) {
            return;
        }
        List<Long> targetIds = monitorTargetMapper.selectList(
            Wrappers.<CmMonitorTarget>lambdaQuery()
                .in(CmMonitorTarget::getCreatorId, creatorIds)
                .eq(CmMonitorTarget::getTargetType, TARGET_CREATOR_COLLECTION)
                .eq(CmMonitorTarget::getStatus, "active")
                .eq(!canManageAllTargets(), CmMonitorTarget::getOwnerUserId, LoginHelper.getUserId())
        ).stream().map(CmMonitorTarget::getTargetId).toList();
        if (targetIds.isEmpty()) {
            throw new ServiceException("未找到可取消的账号监控，或当前用户无权操作");
        }
        markRelationsRemovedByTargetIds(targetIds);
        markTargetsRemoved(targetIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContents(Collection<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return;
        }
        List<Long> singleTargetIds = monitorTargetMapper.selectList(
            Wrappers.<CmMonitorTarget>lambdaQuery()
                .eq(CmMonitorTarget::getTargetType, TARGET_SINGLE_CONTENT)
                .in(CmMonitorTarget::getContentId, contentIds)
                .eq(CmMonitorTarget::getStatus, "active")
                .eq(!canManageAllTargets(), CmMonitorTarget::getOwnerUserId, LoginHelper.getUserId())
        ).stream().map(CmMonitorTarget::getTargetId).toList();
        if (singleTargetIds.isEmpty()) {
            throw new ServiceException("未找到可取消的作品监控，或当前用户无权操作");
        }
        markRelationsRemovedByTargetIds(singleTargetIds);
        markTargetsRemoved(singleTargetIds);
    }

    private boolean canManageAllTargets() {
        return LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin();
    }

    private void markRelationsRemovedByTargetIds(Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return;
        }
        CmMonitorTargetContent update = new CmMonitorTargetContent();
        update.setStatus("removed");
        targetContentMapper.update(update,
            Wrappers.<CmMonitorTargetContent>lambdaUpdate()
                .in(CmMonitorTargetContent::getTargetId, targetIds)
                .eq(CmMonitorTargetContent::getStatus, "active"));
    }

    private void markTargetsRemoved(Collection<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return;
        }
        CmMonitorTarget update = new CmMonitorTarget();
        update.setStatus("removed");
        update.setDataStatus("removed");
        monitorTargetMapper.update(update,
            Wrappers.<CmMonitorTarget>lambdaUpdate()
                .in(CmMonitorTarget::getTargetId, targetIds)
                .eq(CmMonitorTarget::getStatus, "active"));
    }

    private int collectCreatorCollection(CmMonitorTarget target, CmCreatorAccount creator, TikHubClient client,
                                         boolean forceCollect) {
        Date now = new Date();
        if (forceCollect || target.getNextProfileCollectAt() == null || !target.getNextProfileCollectAt().after(now)) {
            TikHubCreatorProfile profile = tikHubDouyinMapper.mapCreator(client.getProfile(creator.getPlatformCreatorId()), creator.getPlatformCreatorId());
            upsertCreator(creator, creator.getPlatform(), profile);
            saveCreatorSnapshot(creator, target.getTargetId(), profile);
            target.setLastProfileCollectAt(now);
            target.setNextProfileCollectAt(addMinutes(now, target.getProfileCollectIntervalMin()));
        }
        if (!forceCollect && target.getNextDiscoveryAt() != null && target.getNextDiscoveryAt().after(now)) {
            return 0;
        }
        List<TikHubContentProfile> profiles = tikHubDouyinMapper.mapContentList(client.getUserPosts(creator.getPlatformCreatorId()));
        int discovered = 0;
        for (TikHubContentProfile profile : profiles) {
            if (!isAfterBaseline(profile, target)) {
                continue;
            }
            if (findContent(creator.getPlatform(), profile.getPlatformContentId()) != null) {
                continue;
            }
            CmContentPost content = upsertContent(null, creator, creator.getPlatform(), profile, "creator_discovery");
            bindTargetContent(target, content, "creator_discovery", true);
            saveContentSnapshot(content, target.getTargetId(), profile);
            discovered++;
        }
        target.setLastDiscoveryAt(now);
        target.setNextDiscoveryAt(addMinutes(now, target.getContentCollectIntervalMin()));
        monitorTargetMapper.updateById(target);
        return discovered;
    }

    private boolean isImmediateTrigger(String triggerSource) {
        return "manual".equals(triggerSource);
    }

    private int collectBoundContentMetrics(CmMonitorTarget target, CmCreatorAccount creator, TikHubClient client,
                                           String triggerSource) {
        LambdaQueryWrapper<CmMonitorTargetContent> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTargetContent::getTargetId, target.getTargetId());
        lqw.eq(CmMonitorTargetContent::getStatus, "active");
        int collected = 0;
        Date now = new Date();
        for (CmMonitorTargetContent relation : targetContentMapper.selectList(lqw)) {
            CmContentPost content = contentPostMapper.selectById(relation.getContentId());
            if (content == null) {
                continue;
            }
            CmCollectionRun contentRun = startRun(
                "content_metrics",
                triggerSource,
                target,
                creator,
                content,
                target.getPlatform()
            );
            int requestCountBefore = client.getRequestCount();
            BigDecimal costBefore = client.getEstimatedCostUsd();
            try {
                collectSingleContent(target, content, client);
                finishContentRunSuccess(contentRun, client, requestCountBefore, costBefore);
                relation.setLastCollectedAt(now);
                targetContentMapper.updateById(relation);
                collected++;
            } catch (RuntimeException ex) {
                finishContentRunFailed(contentRun, client, requestCountBefore, costBefore, ex);
                throw ex;
            }
        }
        target.setLastContentCollectAt(now);
        target.setNextContentCollectAt(addMinutes(now, target.getContentCollectIntervalMin()));
        monitorTargetMapper.updateById(target);
        return collected;
    }

    private void collectSingleContent(CmMonitorTarget target, CmContentPost content, TikHubClient client) {
        TikHubContentProfile profile = tikHubDouyinMapper.mapOneContent(client.getOneVideo(content.getPlatformContentId()));
        upsertContent(content, content.getCreatorId() == null ? null : creatorAccountMapper.selectById(content.getCreatorId()), content.getPlatform(), profile, content.getAddedSource());
        saveContentSnapshot(content, target.getTargetId(), profile);
        Date now = new Date();
        target.setLastContentCollectAt(now);
        target.setNextContentCollectAt(addMinutes(now, target.getContentCollectIntervalMin()));
        monitorTargetMapper.updateById(target);
    }

    private String resolveSecUserId(TikHubClient client, String input) {
        String value = normalizeInput(input);
        if (value.startsWith("MS4w")) {
            return value;
        }
        String url = value.contains("douyin.com") ? extractDouyinUrl(value) : "https://www.douyin.com/user/" + value;
        return tikHubDouyinMapper.mapSecUserId(client.getSecUserId(url));
    }

    private CmCreatorAccount upsertCreator(CmCreatorAccount creator, String platform, TikHubCreatorProfile profile) {
        if (creator == null) {
            creator = new CmCreatorAccount();
            creator.setPlatform(platform);
            creator.setPlatformCreatorId(defaultText(profile.getPlatformCreatorId(), "unknown-" + System.currentTimeMillis()));
        }
        creator.setPlatformUserId(profile.getPlatformUserId());
        creator.setPlatformDisplayId(profile.getPlatformDisplayId());
        creator.setNickname(defaultText(profile.getNickname(), creator.getPlatformCreatorId()));
        creator.setAvatarUrl(profile.getAvatarUrl());
        creator.setSignature(profile.getSignature());
        creator.setHomepageUrl(defaultText(profile.getHomepageUrl(), "https://www.douyin.com/user/" + creator.getPlatformCreatorId()));
        creator.setIpLocation(profile.getIpLocation());
        creator.setGender(profile.getGender());
        creator.setFollowerCount(profile.getFollowerCount());
        creator.setFollowingCount(profile.getFollowingCount());
        creator.setTotalFavoritedCount(profile.getTotalFavoritedCount());
        creator.setContentCount(profile.getContentCount());
        creator.setProfileStatus("success");
        creator.setLastProfileCollectAt(new Date());
        creator.setRawProfileJson(profile.getRawJson());
        if (creator.getCreatorId() == null) {
            creatorAccountMapper.insert(creator);
        } else {
            creatorAccountMapper.updateById(creator);
        }
        return creator;
    }

    private boolean shouldEnrichCreatorProfile(CmCreatorAccount creator, TikHubCreatorProfile embeddedProfile) {
        if (StringUtils.isBlank(embeddedProfile.getPlatformCreatorId())) {
            return false;
        }
        if (creator == null) {
            return true;
        }
        return nullToZero(creator.getFollowerCount()).signum() == 0
            && nullToZero(embeddedProfile.getTotalFavoritedCount()).signum() > 0;
    }

    private CmContentPost upsertContent(CmContentPost content, CmCreatorAccount creator, String platform, TikHubContentProfile profile, String addedSource) {
        if (content == null) {
            content = new CmContentPost();
            content.setPlatform(platform);
            content.setPlatformContentId(profile.getPlatformContentId());
            content.setFirstSeenAt(new Date());
        }
        if (creator != null) {
            content.setCreatorId(creator.getCreatorId());
        }
        content.setContentType(profile.getContentType());
        content.setTitle(truncate(profile.getTitle(), 500));
        content.setDescription(profile.getDescription());
        content.setCoverUrl(profile.getCoverUrl());
        content.setContentUrl(profile.getContentUrl());
        content.setShareUrl(profile.getShareUrl());
        content.setPublishTime(profile.getPublishTime());
        content.setAddedSource(defaultText(addedSource, "manual"));
        content.setLatestLikeCount(profile.getLikeCount());
        content.setLatestCommentCount(profile.getCommentCount());
        content.setLatestCollectCount(profile.getCollectCount());
        content.setLatestShareCount(profile.getShareCount());
        content.setLatestPlayCount(profile.getPlayCount());
        content.setMetricsStatus(profile.getMetricsStatus());
        content.setLastMetricsCollectAt(new Date());
        content.setRawContentJson(profile.getRawJson());
        if (content.getContentId() == null) {
            contentPostMapper.insert(content);
        } else {
            contentPostMapper.updateById(content);
        }
        return content;
    }

    private CmMonitorTarget upsertCreatorTarget(CmMonitorTarget target, CmCreatorAccount creator, CreatorMonitorCreateBo bo, Date now) {
        if (target == null) {
            target = new CmMonitorTarget();
            target.setTargetType(TARGET_CREATOR_COLLECTION);
            target.setPlatform(creator.getPlatform());
            target.setCreatorId(creator.getCreatorId());
            target.setBaselineTime(now);
            target.setDiscoverNewContent(true);
            target.setStatus("active");
        }
        target.setTargetName(defaultText(bo.getTargetName(), creator.getNickname()));
        applyOwner(target, bo.getOwnerUserId(), bo.getOwnerDeptId(), bo.getDirectSuperiorUserId());
        target.setProfileCollectIntervalMin(defaultInt(bo.getProfileCollectIntervalMin(), 360));
        target.setContentCollectIntervalMin(defaultInt(bo.getContentCollectIntervalMin(), 30));
        target.setLastProfileCollectAt(now);
        target.setNextProfileCollectAt(addMinutes(now, target.getProfileCollectIntervalMin()));
        target.setNextContentCollectAt(addMinutes(now, target.getContentCollectIntervalMin()));
        target.setNextDiscoveryAt(addMinutes(now, target.getContentCollectIntervalMin()));
        target.setDataStatus("waiting_new_content");
        target.setRemark(bo.getRemark());
        target.setTags(bo.getTags());
        if (target.getTargetId() == null) {
            monitorTargetMapper.insert(target);
        } else {
            monitorTargetMapper.updateById(target);
        }
        return target;
    }

    private CmMonitorTarget upsertSingleContentTarget(CmMonitorTarget target, CmCreatorAccount creator, CmContentPost content, ContentLinkCreateBo bo, Date now) {
        if (target == null) {
            target = new CmMonitorTarget();
            target.setTargetType(TARGET_SINGLE_CONTENT);
            target.setPlatform(content.getPlatform());
            target.setCreatorId(creator.getCreatorId());
            target.setContentId(content.getContentId());
            target.setBaselineTime(now);
            target.setDiscoverNewContent(false);
            target.setStatus("active");
        }
        target.setTargetName(defaultText(bo.getTargetName(), content.getTitle()));
        applyOwner(target, bo.getOwnerUserId(), bo.getOwnerDeptId(), bo.getDirectSuperiorUserId());
        target.setProfileCollectIntervalMin(360);
        target.setContentCollectIntervalMin(defaultInt(bo.getContentCollectIntervalMin(), 30));
        target.setLastContentCollectAt(now);
        target.setNextContentCollectAt(addMinutes(now, target.getContentCollectIntervalMin()));
        target.setDataStatus("metrics_ready");
        target.setRemark(bo.getRemark());
        target.setTags(bo.getTags());
        if (target.getTargetId() == null) {
            monitorTargetMapper.insert(target);
        } else {
            monitorTargetMapper.updateById(target);
        }
        return target;
    }

    private void saveCreatorSnapshot(CmCreatorAccount creator, Long targetId, TikHubCreatorProfile profile) {
        CmCreatorSnapshot snapshot = new CmCreatorSnapshot();
        snapshot.setTenantId(creator.getTenantId());
        snapshot.setCreatorId(creator.getCreatorId());
        snapshot.setTargetId(targetId);
        snapshot.setCollectedAt(new Date());
        snapshot.setFollowerCount(profile.getFollowerCount());
        snapshot.setFollowingCount(profile.getFollowingCount());
        snapshot.setTotalFavoritedCount(profile.getTotalFavoritedCount());
        snapshot.setContentCount(profile.getContentCount());
        snapshot.setProfileStatus("success");
        snapshot.setRawSnapshotJson(profile.getRawJson());
        snapshot.setCreateTime(new Date());
        creatorSnapshotMapper.insert(snapshot);
    }

    private void saveContentSnapshot(CmContentPost content, Long targetId, TikHubContentProfile profile) {
        CmContentSnapshot previous = latestContentSnapshot(content.getContentId());
        CmContentSnapshot snapshot = new CmContentSnapshot();
        snapshot.setTenantId(content.getTenantId());
        snapshot.setContentId(content.getContentId());
        snapshot.setTargetId(targetId);
        snapshot.setCollectedAt(new Date());
        snapshot.setLikeCount(profile.getLikeCount());
        snapshot.setCommentCount(profile.getCommentCount());
        snapshot.setCollectCount(profile.getCollectCount());
        snapshot.setShareCount(profile.getShareCount());
        snapshot.setPlayCount(profile.getPlayCount());
        snapshot.setLikeDelta(profile.getLikeCount().subtract(previous == null ? BigInteger.ZERO : nullToZero(previous.getLikeCount())));
        snapshot.setCommentDelta(profile.getCommentCount().subtract(previous == null ? BigInteger.ZERO : nullToZero(previous.getCommentCount())));
        snapshot.setCollectDelta(profile.getCollectCount().subtract(previous == null ? BigInteger.ZERO : nullToZero(previous.getCollectCount())));
        snapshot.setShareDelta(profile.getShareCount().subtract(previous == null ? BigInteger.ZERO : nullToZero(previous.getShareCount())));
        snapshot.setMetricsStatus(profile.getMetricsStatus());
        snapshot.setMissingMetricFields(JsonUtils.toJsonString(profile.getMissingMetricFields()));
        snapshot.setRawSnapshotJson(profile.getRawJson());
        snapshot.setCreateTime(new Date());
        contentSnapshotMapper.insert(snapshot);
        creatorAlertService.evaluateContentSnapshot(content, snapshot);
    }

    private CmContentSnapshot latestContentSnapshot(Long contentId) {
        LambdaQueryWrapper<CmContentSnapshot> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmContentSnapshot::getContentId, contentId);
        lqw.orderByDesc(CmContentSnapshot::getCollectedAt);
        lqw.last("limit 1");
        return contentSnapshotMapper.selectOne(lqw);
    }

    private void bindTargetContent(CmMonitorTarget target, CmContentPost content, String source, boolean publishedAfterBase) {
        LambdaQueryWrapper<CmMonitorTargetContent> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTargetContent::getTargetId, target.getTargetId());
        lqw.eq(CmMonitorTargetContent::getContentId, content.getContentId());
        CmMonitorTargetContent relation = targetContentMapper.selectOne(lqw);
        if (relation == null) {
            relation = new CmMonitorTargetContent();
            relation.setTargetId(target.getTargetId());
            relation.setContentId(content.getContentId());
            relation.setRelationSource(source);
            relation.setPublishedAfterBase(publishedAfterBase);
            relation.setFirstBoundAt(new Date());
            relation.setStatus("active");
            targetContentMapper.insert(relation);
        }
    }

    private CmCollectionRun startRun(String runType, String triggerSource, CmMonitorTarget target, CmCreatorAccount creator, CmContentPost content, String platform) {
        CmCollectionRun run = new CmCollectionRun();
        run.setRunType(runType);
        run.setTriggerSource(triggerSource);
        run.setProvider(PROVIDER_TIKHUB);
        run.setPlatform(platform);
        run.setTargetId(target == null ? null : target.getTargetId());
        run.setCreatorId(creator == null ? null : creator.getCreatorId());
        run.setContentId(content == null ? null : content.getContentId());
        run.setStatus("running");
        run.setStartedAt(new Date());
        run.setCreateTime(new Date());
        collectionRunMapper.insert(run);
        return run;
    }

    private void recoverStaleRuns(Date now) {
        Date cutoff = new Date(now.getTime() - STALE_RUN_TIMEOUT_MS);
        List<CmCollectionRun> staleRuns = collectionRunMapper.selectList(
            Wrappers.<CmCollectionRun>lambdaQuery()
                .eq(CmCollectionRun::getStatus, "running")
                .lt(CmCollectionRun::getStartedAt, cutoff)
        );
        for (CmCollectionRun run : staleRuns) {
            run.setStatus("failed");
            run.setEndedAt(now);
            run.setDurationMs((int) Math.min(Integer.MAX_VALUE, now.getTime() - run.getStartedAt().getTime()));
            run.setFailedCount(1);
            run.setErrorCode("STALE_RUN_RECOVERED");
            run.setErrorMessage("采集进程中断或运行超时，系统已自动结束该记录。");
            run.setResultSummaryJson(JsonUtils.toJsonString(Map.of(
                "provider", defaultText(run.getProvider(), PROVIDER_TIKHUB),
                "recovered", true,
                "reason", "stale_running_record"
            )));
            collectionRunMapper.updateById(run);
        }
    }

    private void finishRunSuccess(CmCollectionRun run, CmMonitorTarget target, CmCreatorAccount creator, CmContentPost content, TikHubClient client, int discovered, int collected) {
        Date ended = new Date();
        run.setTargetId(target == null ? run.getTargetId() : target.getTargetId());
        run.setCreatorId(creator == null ? run.getCreatorId() : creator.getCreatorId());
        run.setContentId(content == null ? run.getContentId() : content.getContentId());
        run.setStatus("success");
        run.setEndedAt(ended);
        run.setDurationMs((int) (ended.getTime() - run.getStartedAt().getTime()));
        run.setDiscoveredCount(discovered);
        run.setCollectedCount(collected);
        run.setFailedCount(0);
        run.setApiCallCount(client.getRequestCount());
        run.setEstimatedCostCny(client.getEstimatedCostUsd().multiply(tikHubProperties.getUsdToCnyRate()));
        run.setResultSummaryJson(JsonUtils.toJsonString(Map.of(
            "provider", PROVIDER_TIKHUB,
            "apiCallCount", client.getRequestCount(),
            "estimatedCostUsd", client.getEstimatedCostUsd(),
            "discoveredCount", discovered,
            "collectedCount", collected
        )));
        collectionRunMapper.updateById(run);
    }

    private void finishRunFailed(CmCollectionRun run, TikHubClient client, RuntimeException ex) {
        Date ended = new Date();
        run.setStatus("failed");
        run.setEndedAt(ended);
        run.setDurationMs((int) (ended.getTime() - run.getStartedAt().getTime()));
        run.setFailedCount(1);
        run.setApiCallCount(client == null ? 0 : client.getRequestCount());
        run.setErrorCode(ex.getClass().getSimpleName());
        run.setErrorMessage(truncate(ex.getMessage(), 1000));
        run.setResultSummaryJson(JsonUtils.toJsonString(Map.of(
            "provider", PROVIDER_TIKHUB,
            "apiCallCount", client == null ? 0 : client.getRequestCount(),
            "estimatedCostUsd", client == null ? BigDecimal.ZERO : client.getEstimatedCostUsd(),
            "error", ex.getMessage() == null ? "" : ex.getMessage()
        )));
        collectionRunMapper.updateById(run);
    }

    private void finishContentRunSuccess(CmCollectionRun run, TikHubClient client, int requestCountBefore,
                                         BigDecimal costBefore) {
        Date ended = new Date();
        int requestCount = Math.max(0, client.getRequestCount() - requestCountBefore);
        BigDecimal costUsd = client.getEstimatedCostUsd().subtract(costBefore).max(BigDecimal.ZERO);
        run.setStatus("success");
        run.setEndedAt(ended);
        run.setDurationMs((int) (ended.getTime() - run.getStartedAt().getTime()));
        run.setCollectedCount(1);
        run.setFailedCount(0);
        run.setApiCallCount(requestCount);
        run.setEstimatedCostCny(costUsd.multiply(tikHubProperties.getUsdToCnyRate()));
        run.setResultSummaryJson(JsonUtils.toJsonString(Map.of(
            "provider", PROVIDER_TIKHUB,
            "apiCallCount", requestCount,
            "estimatedCostUsd", costUsd,
            "collectedCount", 1
        )));
        collectionRunMapper.updateById(run);
    }

    private void finishContentRunFailed(CmCollectionRun run, TikHubClient client, int requestCountBefore,
                                        BigDecimal costBefore, RuntimeException ex) {
        Date ended = new Date();
        int requestCount = Math.max(0, client.getRequestCount() - requestCountBefore);
        BigDecimal costUsd = client.getEstimatedCostUsd().subtract(costBefore).max(BigDecimal.ZERO);
        run.setStatus("failed");
        run.setEndedAt(ended);
        run.setDurationMs((int) (ended.getTime() - run.getStartedAt().getTime()));
        run.setCollectedCount(0);
        run.setFailedCount(1);
        run.setApiCallCount(requestCount);
        run.setEstimatedCostCny(costUsd.multiply(tikHubProperties.getUsdToCnyRate()));
        run.setErrorCode(ex.getClass().getSimpleName());
        run.setErrorMessage(truncate(ex.getMessage(), 1000));
        run.setResultSummaryJson(JsonUtils.toJsonString(Map.of(
            "provider", PROVIDER_TIKHUB,
            "apiCallCount", requestCount,
            "estimatedCostUsd", costUsd,
            "error", ex.getMessage() == null ? "" : ex.getMessage()
        )));
        collectionRunMapper.updateById(run);
    }

    private void saveApiLogs(TikHubClient client, CmCollectionRun run) {
        if (client == null) {
            return;
        }
        for (TikHubCallLogDraft draft : client.getCallLogs()) {
            CmApiCallLog log = new CmApiCallLog();
            log.setRunId(run.getRunId());
            log.setProvider(PROVIDER_TIKHUB);
            log.setPlatform(run.getPlatform());
            log.setEndpoint(draft.getEndpoint());
            log.setRequestMethod(draft.getRequestMethod());
            log.setRequestKey(draft.getRequestKey());
            log.setHttpStatus(draft.getHttpStatus());
            log.setProviderCode(draft.getProviderCode());
            log.setSuccess(draft.getSuccess());
            log.setLatencyMs(draft.getLatencyMs());
            log.setUnitPriceUsd(draft.getUnitPriceUsd());
            log.setUnitPriceCny(draft.getUnitPriceCny());
            log.setEstimatedCostCny(draft.getEstimatedCostCny());
            log.setErrorMessage(draft.getErrorMessage());
            log.setCalledAt(draft.getCalledAt());
            log.setCreateBy(LoginHelper.getUserId());
            log.setCreateTime(new Date());
            apiCallLogMapper.insert(log);
        }
    }

    private TikHubClient newTikHubClient() {
        return new TikHubClient(tikHubProperties, spentTodayUsd());
    }

    private BigDecimal spentTodayUsd() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        LambdaQueryWrapper<CmApiCallLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmApiCallLog::getProvider, PROVIDER_TIKHUB);
        lqw.eq(CmApiCallLog::getSuccess, true);
        lqw.ge(CmApiCallLog::getCalledAt, calendar.getTime());
        BigDecimal total = BigDecimal.ZERO;
        for (CmApiCallLog log : apiCallLogMapper.selectList(lqw)) {
            if (log.getUnitPriceUsd() != null) {
                total = total.add(log.getUnitPriceUsd());
            }
        }
        return total;
    }

    private CmCreatorAccount findCreator(TikHubCreatorProfile profile) {
        if (profile == null) {
            return null;
        }
        if (StringUtils.isBlank(profile.getPlatformCreatorId()) && StringUtils.isBlank(profile.getPlatformDisplayId())) {
            return null;
        }
        if (StringUtils.isNotBlank(profile.getPlatformCreatorId())) {
            LambdaQueryWrapper<CmCreatorAccount> lqw = Wrappers.lambdaQuery();
            lqw.eq(CmCreatorAccount::getPlatform, PLATFORM_DOUYIN);
            lqw.eq(CmCreatorAccount::getPlatformCreatorId, profile.getPlatformCreatorId());
            CmCreatorAccount creator = creatorAccountMapper.selectOne(lqw);
            if (creator != null) {
                return creator;
            }
        }
        if (StringUtils.isBlank(profile.getPlatformDisplayId())) {
            return null;
        }
        LambdaQueryWrapper<CmCreatorAccount> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmCreatorAccount::getPlatform, PLATFORM_DOUYIN);
        lqw.eq(CmCreatorAccount::getPlatformDisplayId, profile.getPlatformDisplayId());
        return creatorAccountMapper.selectOne(lqw);
    }

    private CmContentPost findContent(String platform, String platformContentId) {
        // Use the bypass-soft-delete query so a user-deleted content becomes a blacklist:
        // future creator scans will see it "exists" and skip the re-insert.
        return contentPostMapper.selectIncludingDeleted(platform, platformContentId);
    }

    private CmMonitorTarget findCreatorTarget(Long creatorId) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTarget::getCreatorId, creatorId);
        lqw.eq(CmMonitorTarget::getTargetType, TARGET_CREATOR_COLLECTION);
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.last("limit 1");
        return monitorTargetMapper.selectOne(lqw);
    }

    private CmMonitorTarget findOwnedCreatorTarget(Long creatorId) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTarget::getCreatorId, creatorId);
        lqw.eq(CmMonitorTarget::getTargetType, TARGET_CREATOR_COLLECTION);
        lqw.eq(CmMonitorTarget::getOwnerUserId, LoginHelper.getUserId());
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.last("limit 1");
        return monitorTargetMapper.selectOne(lqw);
    }

    private CmMonitorTarget findScopedCreatorTarget(Long creatorId) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTarget::getCreatorId, creatorId);
        lqw.eq(CmMonitorTarget::getTargetType, TARGET_CREATOR_COLLECTION);
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.last("limit 1");
        return monitorTargetMapper.selectScopedOne(lqw);
    }

    private CmMonitorTarget findOwnedSingleContentTarget(Long contentId) {
        LambdaQueryWrapper<CmMonitorTarget> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmMonitorTarget::getContentId, contentId);
        lqw.eq(CmMonitorTarget::getTargetType, TARGET_SINGLE_CONTENT);
        lqw.eq(CmMonitorTarget::getOwnerUserId, LoginHelper.getUserId());
        lqw.eq(CmMonitorTarget::getStatus, "active");
        lqw.last("limit 1");
        return monitorTargetMapper.selectOne(lqw);
    }

    private TikHubCreatorProfile fallbackCreatorFromContent(TikHubContentProfile contentProfile) {
        TikHubCreatorProfile profile = new TikHubCreatorProfile();
        profile.setPlatformCreatorId("unknown-author-" + contentProfile.getPlatformContentId());
        profile.setNickname("Author " + contentProfile.getPlatformContentId());
        profile.setHomepageUrl("https://www.douyin.com/video/" + contentProfile.getPlatformContentId());
        return profile;
    }

    private boolean isAfterBaseline(TikHubContentProfile profile, CmMonitorTarget target) {
        if (profile.getPublishTime() == null || target.getBaselineTime() == null) {
            return true;
        }
        return profile.getPublishTime().after(target.getBaselineTime());
    }

    private void applyOwner(CmMonitorTarget target, Long ownerUserId, Long ownerDeptId, Long superiorUserId) {
        Long resolvedOwnerUserId = LoginHelper.isLogin() ? LoginHelper.getUserId() : ownerUserId;
        Long resolvedOwnerDeptId = LoginHelper.isLogin() ? LoginHelper.getDeptId() : ownerDeptId;
        target.setOwnerUserId(defaultLong(resolvedOwnerUserId, LoginHelper.getUserId()));
        target.setOwnerDeptId(defaultLong(resolvedOwnerDeptId, LoginHelper.getDeptId()));
        Long deptLeader = target.getOwnerDeptId() == null ? null : deptService.selectDeptLeaderById(target.getOwnerDeptId());
        target.setDirectSuperiorUserId(defaultLong(deptLeader, superiorUserId));
    }

    private MonitorCreateResultVo result(CmCreatorAccount creator, CmContentPost content, CmMonitorTarget target, CmCollectionRun run,
                                         boolean creatorCreated, boolean contentCreated, boolean targetCreated) {
        MonitorCreateResultVo result = new MonitorCreateResultVo();
        result.setCreator(creator);
        result.setContent(content);
        result.setTarget(target);
        result.setRun(run);
        result.setCreatorCreated(creatorCreated);
        result.setContentCreated(contentCreated);
        result.setTargetCreated(targetCreated);
        return result;
    }

    private String extractDouyinUrl(String input) {
        String value = normalizeInput(input);
        Matcher matcher = URL_PATTERN.matcher(value);
        while (matcher.find()) {
            String url = matcher.group().replaceAll("[，。；、）)\\]】>]+$", "");
            if (url.contains("douyin.com")) {
                return url;
            }
        }
        if (value.startsWith("http") && value.contains("douyin.com")) {
            return value;
        }
        throw new ServiceException("Douyin share URL is required.");
    }

    private String normalizeInput(String input) {
        if (StringUtils.isBlank(input)) {
            throw new ServiceException("input is required.");
        }
        return input.trim();
    }

    private void assertDouyin(String platform) {
        if (!PLATFORM_DOUYIN.equals(platform)) {
            throw new ServiceException("Only douyin is supported in this phase.");
        }
    }

    private Date addMinutes(Date date, Integer minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, defaultInt(minutes, 30));
        return calendar.getTime();
    }

    private BigInteger nullToZero(BigInteger value) {
        return value == null ? BigInteger.ZERO : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private Long defaultLong(Long value, Long fallback) {
        return value == null ? fallback : value;
    }

    private Integer defaultInt(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
