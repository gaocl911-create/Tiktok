package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.creator.domain.CmAlertEvent;
import org.dromara.creator.domain.CmAlertRule;
import org.dromara.creator.domain.CmContentPost;
import org.dromara.creator.domain.CmContentSnapshot;
import org.dromara.creator.domain.CmCreatorAccount;
import org.dromara.creator.domain.bo.AlertEventHandleBo;
import org.dromara.creator.domain.bo.AlertRuleBo;
import org.dromara.creator.mapper.CmAlertEventMapper;
import org.dromara.creator.mapper.CmAlertRuleMapper;
import org.dromara.creator.mapper.CmContentSnapshotMapper;
import org.dromara.creator.mapper.CmCreatorAccountMapper;
import org.dromara.creator.service.ICreatorAlertService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class CreatorAlertServiceImpl implements ICreatorAlertService {

    private static final String RULE_CUMULATIVE = "cumulative";
    private static final String RULE_WINDOW_GROWTH = "window_growth";
    private static final Set<String> METRICS = Set.of("like", "comment");
    private static final Set<String> RULE_TYPES = Set.of(RULE_CUMULATIVE, RULE_WINDOW_GROWTH);
    private static final Set<String> SCOPE_TYPES = Set.of("all", "creator", "content");
    private static final Set<String> SEVERITIES = Set.of("normal", "important", "urgent");
    private static final Set<String> EVENT_STATUSES = Set.of("pending", "tracking", "resolved", "ignored");

    private final CmAlertRuleMapper alertRuleMapper;
    private final CmAlertEventMapper alertEventMapper;
    private final CmContentSnapshotMapper contentSnapshotMapper;
    private final CmCreatorAccountMapper creatorAccountMapper;

    @Override
    public TableDataInfo<CmAlertRule> queryRulePage(CmAlertRule query, PageQuery pageQuery) {
        LambdaQueryWrapper<CmAlertRule> lqw = Wrappers.lambdaQuery();
        lqw.like(query.getRuleName() != null, CmAlertRule::getRuleName, query.getRuleName());
        lqw.eq(query.getMetricType() != null, CmAlertRule::getMetricType, query.getMetricType());
        lqw.eq(query.getRuleType() != null, CmAlertRule::getRuleType, query.getRuleType());
        lqw.eq(query.getEnabled() != null, CmAlertRule::getEnabled, query.getEnabled());
        lqw.orderByDesc(CmAlertRule::getCreateTime);
        return TableDataInfo.build(alertRuleMapper.selectPage(pageQuery.build(), lqw));
    }

    @Override
    public List<CmAlertRule> queryEnabledRules() {
        LambdaQueryWrapper<CmAlertRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmAlertRule::getEnabled, true);
        lqw.orderByDesc(CmAlertRule::getCreateTime);
        return alertRuleMapper.selectList(lqw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmAlertRule saveRule(AlertRuleBo bo) {
        validateRule(bo);
        CmAlertRule rule = bo.getRuleId() == null ? new CmAlertRule() : alertRuleMapper.selectById(bo.getRuleId());
        if (rule == null) {
            throw new ServiceException("alert rule not found.");
        }
        BeanUtils.copyProperties(bo, rule);
        if (RULE_CUMULATIVE.equals(rule.getRuleType())) {
            rule.setWindowMinutes(null);
        }
        if ("all".equals(rule.getScopeType())) {
            rule.setScopeId(null);
        }
        if (rule.getCooldownMinutes() == null) {
            rule.setCooldownMinutes(120);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        if (rule.getRuleId() == null) {
            alertRuleMapper.insert(rule);
        } else {
            alertRuleMapper.updateById(rule);
        }
        return rule;
    }

    @Override
    public boolean deleteRules(Collection<Long> ruleIds) {
        return alertRuleMapper.deleteByIds(ruleIds) > 0;
    }

    @Override
    public TableDataInfo<CmAlertEvent> queryEventPage(CmAlertEvent query, PageQuery pageQuery) {
        LambdaQueryWrapper<CmAlertEvent> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getStatus() != null, CmAlertEvent::getStatus, query.getStatus());
        lqw.eq(query.getSeverity() != null, CmAlertEvent::getSeverity, query.getSeverity());
        lqw.eq(query.getMetricType() != null, CmAlertEvent::getMetricType, query.getMetricType());
        lqw.eq(query.getContentId() != null, CmAlertEvent::getContentId, query.getContentId());
        lqw.orderByDesc(CmAlertEvent::getLastTriggeredAt);
        return TableDataInfo.build(alertEventMapper.selectPage(pageQuery.build(), lqw));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CmAlertEvent handleEvent(Long eventId, AlertEventHandleBo bo) {
        if (!EVENT_STATUSES.contains(bo.getStatus())) {
            throw new ServiceException("unsupported alert event status.");
        }
        CmAlertEvent event = alertEventMapper.selectById(eventId);
        if (event == null) {
            throw new ServiceException("alert event not found.");
        }
        event.setStatus(bo.getStatus());
        event.setHandleNote(bo.getHandleNote());
        event.setHandledAt(new Date());
        event.setHandledBy(LoginHelper.getUserId());
        alertEventMapper.updateById(event);
        return event;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateContentSnapshot(CmContentPost content, CmContentSnapshot snapshot) {
        if (content == null || snapshot == null || snapshot.getSnapshotId() == null) {
            return;
        }
        for (CmAlertRule rule : applicableRules(content)) {
            Evaluation evaluation = evaluate(rule, content, snapshot);
            if (evaluation != null && evaluation.observed().compareTo(rule.getThresholdValue()) >= 0) {
                createOrUpdateEvent(rule, content, snapshot, evaluation);
            }
        }
    }

    private List<CmAlertRule> applicableRules(CmContentPost content) {
        LambdaQueryWrapper<CmAlertRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmAlertRule::getEnabled, true);
        lqw.and(scope -> scope
            .eq(CmAlertRule::getScopeType, "all")
            .or(item -> item.eq(CmAlertRule::getScopeType, "content")
                .eq(CmAlertRule::getScopeId, content.getContentId()))
            .or(item -> item.eq(CmAlertRule::getScopeType, "creator")
                .eq(CmAlertRule::getScopeId, content.getCreatorId())));
        return alertRuleMapper.selectList(lqw);
    }

    private Evaluation evaluate(CmAlertRule rule, CmContentPost content, CmContentSnapshot current) {
        BigInteger currentValue = metricValue(current, rule.getMetricType());
        if (currentValue == null) {
            return null;
        }
        if (RULE_CUMULATIVE.equals(rule.getRuleType())) {
            Date start = content.getPublishTime() == null ? content.getFirstSeenAt() : content.getPublishTime();
            return new Evaluation(currentValue, start, current.getCollectedAt());
        }
        Date windowStart = new Date(current.getCollectedAt().getTime() - rule.getWindowMinutes() * 60_000L);
        CmContentSnapshot baseline = baselineSnapshot(content.getContentId(), current.getSnapshotId(), windowStart, current.getCollectedAt());
        if (baseline == null) {
            return null;
        }
        BigInteger baselineValue = metricValue(baseline, rule.getMetricType());
        if (baselineValue == null) {
            return null;
        }
        return new Evaluation(currentValue.subtract(baselineValue).max(BigInteger.ZERO),
            baseline.getCollectedAt(), current.getCollectedAt());
    }

    private CmContentSnapshot baselineSnapshot(Long contentId, Long currentSnapshotId, Date windowStart, Date currentAt) {
        LambdaQueryWrapper<CmContentSnapshot> beforeWindow = Wrappers.lambdaQuery();
        beforeWindow.eq(CmContentSnapshot::getContentId, contentId);
        beforeWindow.le(CmContentSnapshot::getCollectedAt, windowStart);
        beforeWindow.ne(CmContentSnapshot::getSnapshotId, currentSnapshotId);
        beforeWindow.orderByDesc(CmContentSnapshot::getCollectedAt);
        beforeWindow.last("limit 1");
        CmContentSnapshot baseline = contentSnapshotMapper.selectOne(beforeWindow);
        if (baseline != null) {
            return baseline;
        }
        LambdaQueryWrapper<CmContentSnapshot> insideWindow = Wrappers.lambdaQuery();
        insideWindow.eq(CmContentSnapshot::getContentId, contentId);
        insideWindow.ge(CmContentSnapshot::getCollectedAt, windowStart);
        insideWindow.lt(CmContentSnapshot::getCollectedAt, currentAt);
        insideWindow.ne(CmContentSnapshot::getSnapshotId, currentSnapshotId);
        insideWindow.orderByAsc(CmContentSnapshot::getCollectedAt);
        insideWindow.last("limit 1");
        return contentSnapshotMapper.selectOne(insideWindow);
    }

    private void createOrUpdateEvent(CmAlertRule rule, CmContentPost content, CmContentSnapshot snapshot,
                                     Evaluation evaluation) {
        CmAlertEvent latest = latestEvent(rule.getRuleId(), content.getContentId());
        Date now = snapshot.getCollectedAt();
        if (latest != null && ("pending".equals(latest.getStatus()) || "tracking".equals(latest.getStatus()))) {
            latest.setObservedValue(evaluation.observed());
            latest.setSnapshotId(snapshot.getSnapshotId());
            latest.setTargetId(snapshot.getTargetId());
            latest.setWindowStartAt(evaluation.windowStart());
            latest.setWindowEndAt(evaluation.windowEnd());
            latest.setLastTriggeredAt(now);
            latest.setTriggerCount((latest.getTriggerCount() == null ? 0 : latest.getTriggerCount()) + 1);
            alertEventMapper.updateById(latest);
            return;
        }
        int cooldown = rule.getCooldownMinutes() == null ? 0 : rule.getCooldownMinutes();
        if (latest != null && latest.getLastTriggeredAt() != null
            && latest.getLastTriggeredAt().getTime() + cooldown * 60_000L > now.getTime()) {
            return;
        }
        CmAlertEvent event = new CmAlertEvent();
        event.setTenantId(content.getTenantId());
        event.setRuleId(rule.getRuleId());
        event.setContentId(content.getContentId());
        event.setCreatorId(content.getCreatorId());
        event.setTargetId(snapshot.getTargetId());
        event.setSnapshotId(snapshot.getSnapshotId());
        event.setEventTitle(rule.getRuleName());
        event.setContentTitle(content.getTitle());
        CmCreatorAccount creator = content.getCreatorId() == null ? null : creatorAccountMapper.selectById(content.getCreatorId());
        event.setCreatorNickname(creator == null ? null : creator.getNickname());
        event.setMetricType(rule.getMetricType());
        event.setRuleType(rule.getRuleType());
        event.setWindowMinutes(rule.getWindowMinutes());
        event.setThresholdValue(rule.getThresholdValue());
        event.setObservedValue(evaluation.observed());
        event.setWindowStartAt(evaluation.windowStart());
        event.setWindowEndAt(evaluation.windowEnd());
        event.setSeverity(rule.getSeverity());
        event.setStatus("pending");
        event.setTriggerCount(1);
        event.setFirstTriggeredAt(now);
        event.setLastTriggeredAt(now);
        alertEventMapper.insert(event);
    }

    private CmAlertEvent latestEvent(Long ruleId, Long contentId) {
        LambdaQueryWrapper<CmAlertEvent> lqw = Wrappers.lambdaQuery();
        lqw.eq(CmAlertEvent::getRuleId, ruleId);
        lqw.eq(CmAlertEvent::getContentId, contentId);
        lqw.orderByDesc(CmAlertEvent::getLastTriggeredAt);
        lqw.last("limit 1");
        return alertEventMapper.selectOne(lqw);
    }

    private BigInteger metricValue(CmContentSnapshot snapshot, String metricType) {
        return "like".equals(metricType) ? snapshot.getLikeCount() : snapshot.getCommentCount();
    }

    private void validateRule(AlertRuleBo bo) {
        if (!METRICS.contains(bo.getMetricType())) {
            throw new ServiceException("metricType must be like or comment.");
        }
        if (!RULE_TYPES.contains(bo.getRuleType())) {
            throw new ServiceException("unsupported ruleType.");
        }
        if (!SCOPE_TYPES.contains(bo.getScopeType())) {
            throw new ServiceException("unsupported scopeType.");
        }
        if (!SEVERITIES.contains(bo.getSeverity())) {
            throw new ServiceException("unsupported severity.");
        }
        if (RULE_WINDOW_GROWTH.equals(bo.getRuleType())
            && (bo.getWindowMinutes() == null || bo.getWindowMinutes() < 1)) {
            throw new ServiceException("windowMinutes must be greater than zero for a growth rule.");
        }
        if (!"all".equals(bo.getScopeType()) && bo.getScopeId() == null) {
            throw new ServiceException("scopeId is required for creator/content scope.");
        }
    }

    private record Evaluation(BigInteger observed, Date windowStart, Date windowEnd) {
    }
}
