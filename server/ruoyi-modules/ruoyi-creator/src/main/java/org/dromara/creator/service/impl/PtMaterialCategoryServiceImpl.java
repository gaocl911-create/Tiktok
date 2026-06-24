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
import org.dromara.creator.domain.bo.PtMaterialCategoryBo;
import org.dromara.creator.domain.vo.PtMaterialCategoryVo;
import org.dromara.creator.mapper.PtMaterialCategoryMapper;
import org.dromara.creator.service.IPtMaterialCategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PtMaterialCategoryServiceImpl implements IPtMaterialCategoryService {

    private static final String TYPE_TEXT = "text";
    private static final String TYPE_IMAGE = "image";
    private static final String STATUS_ENABLED = "0";

    private final PtMaterialCategoryMapper ptMaterialCategoryMapper;

    @Override
    public TableDataInfo<PtMaterialCategoryVo> queryPage(PtMaterialCategory query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtMaterialCategory> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(query.getCategoryType()), PtMaterialCategory::getCategoryType, query.getCategoryType());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), PtMaterialCategory::getStatus, query.getStatus());
        lqw.like(StringUtils.isNotBlank(query.getCategoryName()), PtMaterialCategory::getCategoryName, query.getCategoryName());
        lqw.orderByAsc(PtMaterialCategory::getCategoryType, PtMaterialCategory::getSort, PtMaterialCategory::getCategoryId);
        Page<PtMaterialCategory> page = ptMaterialCategoryMapper.selectPage(pageQuery.build(), lqw);
        List<PtMaterialCategoryVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public List<PtMaterialCategoryVo> queryOptions(String categoryType) {
        validateType(categoryType);
        LambdaQueryWrapper<PtMaterialCategory> lqw = Wrappers.lambdaQuery();
        lqw.eq(PtMaterialCategory::getCategoryType, categoryType);
        lqw.eq(PtMaterialCategory::getStatus, STATUS_ENABLED);
        lqw.orderByAsc(PtMaterialCategory::getSort, PtMaterialCategory::getCategoryId);
        return ptMaterialCategoryMapper.selectList(lqw).stream().map(this::toVo).toList();
    }

    @Override
    public PtMaterialCategoryVo queryById(Long categoryId) {
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("素材分类不存在");
        }
        return toVo(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialCategoryVo createCategory(PtMaterialCategoryBo bo) {
        validateType(bo.getCategoryType());
        PtMaterialCategory category = new PtMaterialCategory();
        copyBo(category, bo);
        ptMaterialCategoryMapper.insert(category);
        return toVo(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialCategoryVo updateCategory(PtMaterialCategoryBo bo) {
        if (bo.getCategoryId() == null) {
            throw new ServiceException("素材分类ID不能为空");
        }
        validateType(bo.getCategoryType());
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(bo.getCategoryId());
        if (category == null) {
            throw new ServiceException("素材分类不存在");
        }
        copyBo(category, bo);
        ptMaterialCategoryMapper.updateById(category);
        return toVo(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Long[] categoryIds) {
        return ptMaterialCategoryMapper.deleteBatchIds(Arrays.asList(categoryIds)) > 0;
    }

    private void copyBo(PtMaterialCategory category, PtMaterialCategoryBo bo) {
        category.setCategoryType(bo.getCategoryType());
        category.setCategoryName(bo.getCategoryName());
        category.setSort(bo.getSort() == null ? 0 : bo.getSort());
        category.setStatus(StringUtils.isBlank(bo.getStatus()) ? STATUS_ENABLED : bo.getStatus());
        category.setRemark(bo.getRemark());
    }

    private void validateType(String categoryType) {
        if (!TYPE_TEXT.equals(categoryType) && !TYPE_IMAGE.equals(categoryType)) {
            throw new ServiceException("素材分类类型必须是 text 或 image");
        }
    }

    private PtMaterialCategoryVo toVo(PtMaterialCategory category) {
        PtMaterialCategoryVo vo = new PtMaterialCategoryVo();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
