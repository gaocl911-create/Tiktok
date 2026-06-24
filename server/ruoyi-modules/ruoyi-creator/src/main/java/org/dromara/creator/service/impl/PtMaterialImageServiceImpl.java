package org.dromara.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialCategory;
import org.dromara.creator.domain.PtMaterialImage;
import org.dromara.creator.domain.bo.PtMaterialImageBo;
import org.dromara.creator.domain.vo.PtMaterialImageVo;
import org.dromara.creator.mapper.PtMaterialCategoryMapper;
import org.dromara.creator.mapper.PtMaterialImageMapper;
import org.dromara.creator.service.IPtMaterialImageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PtMaterialImageServiceImpl implements IPtMaterialImageService {

    private static final String TYPE_IMAGE = "image";
    private static final String STATUS_ENABLED = "0";

    private final PtMaterialImageMapper ptMaterialImageMapper;
    private final PtMaterialCategoryMapper ptMaterialCategoryMapper;

    @Override
    public TableDataInfo<PtMaterialImageVo> queryPage(PtMaterialImage query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtMaterialImage> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getCategoryId() != null, PtMaterialImage::getCategoryId, query.getCategoryId());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), PtMaterialImage::getStatus, query.getStatus());
        lqw.like(StringUtils.isNotBlank(query.getImageName()), PtMaterialImage::getImageName, query.getImageName());
        lqw.orderByAsc(PtMaterialImage::getSort, PtMaterialImage::getImageId);
        Page<PtMaterialImage> page = ptMaterialImageMapper.selectPage(pageQuery.build(), lqw);
        List<PtMaterialImageVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PtMaterialImageVo queryById(Long imageId) {
        PtMaterialImage image = ptMaterialImageMapper.selectById(imageId);
        if (image == null) {
            throw new ServiceException("图片素材不存在");
        }
        return toVo(image);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialImageVo createImage(PtMaterialImageBo bo) {
        ensureImageCategory(bo.getCategoryId());
        PtMaterialImage image = new PtMaterialImage();
        copyBo(image, bo);
        ptMaterialImageMapper.insert(image);
        return toVo(image);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialImageVo updateImage(PtMaterialImageBo bo) {
        if (bo.getImageId() == null) {
            throw new ServiceException("图片素材ID不能为空");
        }
        ensureImageCategory(bo.getCategoryId());
        PtMaterialImage image = ptMaterialImageMapper.selectById(bo.getImageId());
        if (image == null) {
            throw new ServiceException("图片素材不存在");
        }
        copyBo(image, bo);
        ptMaterialImageMapper.updateById(image);
        return toVo(image);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Long[] imageIds) {
        return ptMaterialImageMapper.deleteBatchIds(Arrays.asList(imageIds)) > 0;
    }

    private void copyBo(PtMaterialImage image, PtMaterialImageBo bo) {
        image.setCategoryId(bo.getCategoryId());
        image.setImageUrl(bo.getImageUrl());
        image.setImageName(bo.getImageName());
        image.setImageSize(bo.getImageSize());
        image.setSort(bo.getSort() == null ? 0 : bo.getSort());
        image.setStatus(StringUtils.isBlank(bo.getStatus()) ? STATUS_ENABLED : bo.getStatus());
        image.setRemark(bo.getRemark());
    }

    private void ensureImageCategory(Long categoryId) {
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(categoryId);
        if (category == null || !TYPE_IMAGE.equals(category.getCategoryType())) {
            throw new ServiceException("请选择有效的图片分类");
        }
    }

    private PtMaterialImageVo toVo(PtMaterialImage image) {
        PtMaterialImageVo vo = new PtMaterialImageVo();
        BeanUtils.copyProperties(image, vo);
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(image.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
        }
        return vo;
    }
}
