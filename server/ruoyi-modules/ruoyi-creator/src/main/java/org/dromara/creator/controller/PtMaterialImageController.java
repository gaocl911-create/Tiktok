package org.dromara.creator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.creator.domain.PtMaterialImage;
import org.dromara.creator.domain.bo.PtMaterialImageBo;
import org.dromara.creator.domain.vo.PtMaterialImageVo;
import org.dromara.creator.service.IPtMaterialImageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/parttime/material/image")
public class PtMaterialImageController {

    private final IPtMaterialImageService materialImageService;

    @SaCheckPermission("parttime:material:image:list")
    @GetMapping("/list")
    public TableDataInfo<PtMaterialImageVo> list(PtMaterialImage query, PageQuery pageQuery) {
        return materialImageService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("parttime:material:image:query")
    @GetMapping("/{imageId}")
    public R<PtMaterialImageVo> getInfo(@NotNull(message = "imageId is required") @PathVariable Long imageId) {
        return R.ok(materialImageService.queryById(imageId));
    }

    @SaCheckPermission("parttime:material:image:add")
    @PostMapping
    public R<PtMaterialImageVo> add(@Valid @RequestBody PtMaterialImageBo bo) {
        return R.ok(materialImageService.createImage(bo));
    }

    @SaCheckPermission("parttime:material:image:edit")
    @PutMapping
    public R<PtMaterialImageVo> edit(@Valid @RequestBody PtMaterialImageBo bo) {
        return R.ok(materialImageService.updateImage(bo));
    }

    @SaCheckPermission("parttime:material:image:remove")
    @DeleteMapping("/{imageIds}")
    public R<Boolean> remove(@PathVariable Long[] imageIds) {
        return R.ok(materialImageService.deleteByIds(imageIds));
    }
}
