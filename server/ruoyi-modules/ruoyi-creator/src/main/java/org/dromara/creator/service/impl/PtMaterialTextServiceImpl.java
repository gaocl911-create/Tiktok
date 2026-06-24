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
import org.dromara.creator.domain.PtMaterialText;
import org.dromara.creator.domain.bo.PtMaterialTextBo;
import org.dromara.creator.domain.vo.PtMaterialTextVo;
import org.dromara.creator.mapper.PtMaterialCategoryMapper;
import org.dromara.creator.mapper.PtMaterialTextMapper;
import org.dromara.creator.service.IPtMaterialTextService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PtMaterialTextServiceImpl implements IPtMaterialTextService {

    private static final String TYPE_TEXT = "text";
    private static final String STATUS_ENABLED = "0";

    private final PtMaterialTextMapper ptMaterialTextMapper;
    private final PtMaterialCategoryMapper ptMaterialCategoryMapper;

    @Override
    public TableDataInfo<PtMaterialTextVo> queryPage(PtMaterialText query, PageQuery pageQuery) {
        LambdaQueryWrapper<PtMaterialText> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getCategoryId() != null, PtMaterialText::getCategoryId, query.getCategoryId());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), PtMaterialText::getStatus, query.getStatus());
        lqw.like(StringUtils.isNotBlank(query.getContent()), PtMaterialText::getContent, query.getContent());
        lqw.orderByAsc(PtMaterialText::getSort, PtMaterialText::getTextId);
        Page<PtMaterialText> page = ptMaterialTextMapper.selectPage(pageQuery.build(), lqw);
        List<PtMaterialTextVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public PtMaterialTextVo queryById(Long textId) {
        PtMaterialText text = ptMaterialTextMapper.selectById(textId);
        if (text == null) {
            throw new ServiceException("文案素材不存在");
        }
        return toVo(text);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialTextVo createText(PtMaterialTextBo bo) {
        ensureTextCategory(bo.getCategoryId());
        PtMaterialText text = new PtMaterialText();
        copyBo(text, bo);
        ptMaterialTextMapper.insert(text);
        return toVo(text);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PtMaterialTextVo updateText(PtMaterialTextBo bo) {
        if (bo.getTextId() == null) {
            throw new ServiceException("文案素材ID不能为空");
        }
        ensureTextCategory(bo.getCategoryId());
        PtMaterialText text = ptMaterialTextMapper.selectById(bo.getTextId());
        if (text == null) {
            throw new ServiceException("文案素材不存在");
        }
        copyBo(text, bo);
        ptMaterialTextMapper.updateById(text);
        return toVo(text);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(Long[] textIds) {
        return ptMaterialTextMapper.deleteBatchIds(Arrays.asList(textIds)) > 0;
    }

    private void copyBo(PtMaterialText text, PtMaterialTextBo bo) {
        text.setCategoryId(bo.getCategoryId());
        text.setContent(bo.getContent());
        text.setSort(bo.getSort() == null ? 0 : bo.getSort());
        text.setStatus(StringUtils.isBlank(bo.getStatus()) ? STATUS_ENABLED : bo.getStatus());
        text.setRemark(bo.getRemark());
    }

    private void ensureTextCategory(Long categoryId) {
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(categoryId);
        if (category == null || !TYPE_TEXT.equals(category.getCategoryType())) {
            throw new ServiceException("请选择有效的文案分类");
        }
    }

    private PtMaterialTextVo toVo(PtMaterialText text) {
        PtMaterialTextVo vo = new PtMaterialTextVo();
        BeanUtils.copyProperties(text, vo);
        PtMaterialCategory category = ptMaterialCategoryMapper.selectById(text.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
        }
        return vo;
    }
}
