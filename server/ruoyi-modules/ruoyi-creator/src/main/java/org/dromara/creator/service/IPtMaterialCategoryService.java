package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialCategory;
import org.dromara.creator.domain.bo.PtMaterialCategoryBo;
import org.dromara.creator.domain.vo.PtMaterialCategoryVo;

import java.util.List;

public interface IPtMaterialCategoryService {

    TableDataInfo<PtMaterialCategoryVo> queryPage(PtMaterialCategory query, PageQuery pageQuery);

    List<PtMaterialCategoryVo> queryOptions(String categoryType);

    PtMaterialCategoryVo queryById(Long categoryId);

    PtMaterialCategoryVo createCategory(PtMaterialCategoryBo bo);

    PtMaterialCategoryVo updateCategory(PtMaterialCategoryBo bo);

    Boolean deleteByIds(Long[] categoryIds);
}
