package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialCategory;
import org.dromara.creator.domain.bo.PtMaterialCategoryBo;
import org.dromara.creator.domain.vo.PtMaterialCategoryVo;
import org.dromara.creator.service.IPtMaterialCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/material/category")
public class PtMaterialCategoryController {

    private final IPtMaterialCategoryService materialCategoryService;

    @SaCheckPermission("parttime:material:category:list")
    @GetMapping("/list")
    public TableDataInfo<PtMaterialCategoryVo> list(PtMaterialCategory query, PageQuery pageQuery) {
        return materialCategoryService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:material:category:list")
    @GetMapping("/options")
    public R<List<PtMaterialCategoryVo>> options(@RequestParam String categoryType) {
        return R.ok(materialCategoryService.queryOptions(categoryType));
    }

    @SaCheckPermission("parttime:material:category:query")
    @GetMapping("/{categoryId}")
    public R<PtMaterialCategoryVo> getInfo(@NotNull(message = "categoryId is required") @PathVariable Long categoryId) {
        return R.ok(materialCategoryService.queryById(categoryId));
    }

    @SaCheckPermission("parttime:material:category:add")
    @PostMapping
    public R<PtMaterialCategoryVo> add(@Valid @RequestBody PtMaterialCategoryBo bo) {
        return R.ok(materialCategoryService.createCategory(bo));
    }

    @SaCheckPermission("parttime:material:category:edit")
    @PutMapping
    public R<PtMaterialCategoryVo> edit(@Valid @RequestBody PtMaterialCategoryBo bo) {
        return R.ok(materialCategoryService.updateCategory(bo));
    }

    @SaCheckPermission("parttime:material:category:remove")
    @DeleteMapping("/{categoryIds}")
    public R<Boolean> remove(@PathVariable Long[] categoryIds) {
        return R.ok(materialCategoryService.deleteByIds(categoryIds));
    }
}
