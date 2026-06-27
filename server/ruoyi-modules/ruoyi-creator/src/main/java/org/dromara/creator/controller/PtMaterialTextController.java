package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.core.ExcelResult;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialText;
import org.dromara.creator.domain.bo.PtMaterialTextBo;
import org.dromara.creator.domain.vo.PtMaterialTextImportVo;
import org.dromara.creator.domain.vo.PtMaterialTextVo;
import org.dromara.creator.service.IPtMaterialTextService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

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

    @SaCheckPermission("parttime:material:text:add")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(
        @RequestPart("file") MultipartFile file,
        @NotNull(message = "categoryId is required") @RequestParam Long categoryId
    ) throws Exception {
        ExcelResult<PtMaterialTextImportVo> result = ExcelUtil.importExcel(file.getInputStream(), PtMaterialTextImportVo.class, false);
        int count = materialTextService.importTexts(categoryId, result.getList());
        return R.ok("成功导入 " + count + " 条文案");
    }

    @SaCheckPermission("parttime:material:text:add")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "文案导入模板", PtMaterialTextImportVo.class, response);
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
