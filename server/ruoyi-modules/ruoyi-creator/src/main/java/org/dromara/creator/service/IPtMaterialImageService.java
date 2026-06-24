package org.dromara.creator.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialImage;
import org.dromara.creator.domain.bo.PtMaterialImageBo;
import org.dromara.creator.domain.vo.PtMaterialImageVo;

public interface IPtMaterialImageService {

    TableDataInfo<PtMaterialImageVo> queryPage(PtMaterialImage query, PageQuery pageQuery);

    PtMaterialImageVo queryById(Long imageId);

    PtMaterialImageVo createImage(PtMaterialImageBo bo);

    PtMaterialImageVo updateImage(PtMaterialImageBo bo);

    Boolean deleteByIds(Long[] imageIds);
}
