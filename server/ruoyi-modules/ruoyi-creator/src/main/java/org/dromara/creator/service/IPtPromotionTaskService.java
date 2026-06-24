package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtPromotionTask;
import org.dromara.creator.domain.bo.PtPromotionTaskBo;
import org.dromara.creator.domain.vo.PtPromotionTaskVo;

public interface IPtPromotionTaskService {

    TableDataInfo<PtPromotionTaskVo> queryTaskPage(PtPromotionTask query, PageQuery pageQuery);

    PtPromotionTaskVo queryById(Long taskId);

    PtPromotionTaskVo createTask(PtPromotionTaskBo bo);

    PtPromotionTaskVo updateTask(PtPromotionTaskBo bo);

    PtPromotionTaskVo publish(Long taskId);

    PtPromotionTaskVo pause(Long taskId);

    PtPromotionTaskVo finish(Long taskId);
}
