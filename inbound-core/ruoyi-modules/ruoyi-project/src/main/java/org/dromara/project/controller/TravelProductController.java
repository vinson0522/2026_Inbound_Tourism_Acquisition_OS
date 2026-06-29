package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.TravelProductBo;
import org.dromara.project.domain.vo.TravelProductVo;
import org.dromara.project.service.ITravelProductService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品路线 API — FR-003
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/products")
public class TravelProductController extends BaseController {

    private final ITravelProductService travelProductService;

    @SaCheckLogin
    @GetMapping
    public R<List<TravelProductVo>> list(@NotNull @PathVariable Long projectId) {
        return R.ok(travelProductService.queryList(projectId));
    }

    @SaCheckLogin
    @GetMapping("/{productId}")
    public R<TravelProductVo> getInfo(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long productId
    ) {
        return R.ok(travelProductService.queryById(projectId, productId));
    }

    @SaCheckLogin
    @Log(title = "产品路线", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody TravelProductBo bo
    ) {
        return R.ok(travelProductService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "产品路线", businessType = BusinessType.UPDATE)
    @PutMapping("/{productId}")
    public R<Void> edit(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long productId,
        @Validated(EditGroup.class) @RequestBody TravelProductBo bo
    ) {
        bo.setId(productId);
        return toAjax(travelProductService.updateByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "产品路线", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productId}")
    public R<Void> remove(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long productId
    ) {
        return toAjax(travelProductService.deleteById(projectId, productId));
    }
}
