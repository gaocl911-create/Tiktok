package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialText;
import org.dromara.creator.domain.bo.PtMaterialTextBo;
import org.dromara.creator.domain.vo.PtMaterialTextVo;
import org.dromara.creator.service.IPtMaterialTextService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/material/text")
public class PtMaterialTextController {

    private final IPtMaterialTextService materialTextService;

    @SaCheckPermission("parttime:material:text:list")
    @GetMapping("/list")
    public TableDataInfo<PtMaterialTextVo> list(PtMaterialText query, PageQuery pageQuery) {
        return materialTextService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:material:text:query")
    @GetMapping("/{textId}")
    public R<PtMaterialTextVo> getInfo(@NotNull(message = "textId is required") @PathVariable Long textId) {
        return R.ok(materialTextService.queryById(textId));
    }

    @SaCheckPermission("parttime:material:text:add")
    @PostMapping
    public R<PtMaterialTextVo> add(@Valid @RequestBody PtMaterialTextBo bo) {
        return R.ok(materialTextService.createText(bo));
    }

    @SaCheckPermission("parttime:material:text:edit")
    @PutMapping
    public R<PtMaterialTextVo> edit(@Valid @RequestBody PtMaterialTextBo bo) {
        return R.ok(materialTextService.updateText(bo));
    }

    @SaCheckPermission("parttime:material:text:remove")
    @DeleteMapping("/{textIds}")
    public R<Boolean> remove(@PathVariable Long[] textIds) {
        return R.ok(materialTextService.deleteByIds(textIds));
    }
}
