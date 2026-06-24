package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialText;
import org.dromara.creator.domain.bo.PtMaterialTextBo;
import org.dromara.creator.domain.vo.PtMaterialTextVo;

public interface IPtMaterialTextService {

    TableDataInfo<PtMaterialTextVo> queryPage(PtMaterialText query, PageQuery pageQuery);

    PtMaterialTextVo queryById(Long textId);

    PtMaterialTextVo createText(PtMaterialTextBo bo);

    PtMaterialTextVo updateText(PtMaterialTextBo bo);

    Boolean deleteByIds(Long[] textIds);
}
