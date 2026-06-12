package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.CmAlertEvent;
import org.dromara.creator.domain.CmAlertRule;
import org.dromara.creator.domain.CmContentPost;
import org.dromara.creator.domain.CmContentSnapshot;
import org.dromara.creator.domain.bo.AlertEventHandleBo;
import org.dromara.creator.domain.bo.AlertRuleBo;

import java.util.Collection;
import java.util.List;

public interface ICreatorAlertService {

    TableDataInfo<CmAlertRule> queryRulePage(CmAlertRule query, PageQuery pageQuery);

    List<CmAlertRule> queryEnabledRules();

    CmAlertRule saveRule(AlertRuleBo bo);

    boolean deleteRules(Collection<Long> ruleIds);

    TableDataInfo<CmAlertEvent> queryEventPage(CmAlertEvent query, PageQuery pageQuery);

    CmAlertEvent handleEvent(Long eventId, AlertEventHandleBo bo);

    void evaluateContentSnapshot(CmContentPost content, CmContentSnapshot snapshot);
}
