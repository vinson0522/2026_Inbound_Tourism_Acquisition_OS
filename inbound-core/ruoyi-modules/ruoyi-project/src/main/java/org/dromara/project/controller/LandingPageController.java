package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.LandingGenerateBo;
import org.dromara.project.domain.bo.LandingPageBo;
import org.dromara.project.domain.vo.LandingGenerateVo;
import org.dromara.project.domain.vo.LandingPageDetailVo;
import org.dromara.project.domain.vo.LandingPageVo;
import org.dromara.project.domain.vo.LandingPublishVo;
import org.dromara.project.service.ILandingPageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 落地页 API — EPIC-6 M1 FR-501~505
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/landing-pages")
public class LandingPageController extends BaseController {

    private final ILandingPageService landingPageService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<LandingPageVo> list(
        @NotNull @PathVariable Long projectId,
        LandingPageBo bo,
        PageQuery pageQuery
    ) {
        return landingPageService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{pageId}")
    public R<LandingPageDetailVo> get(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long pageId
    ) {
        return R.ok(landingPageService.queryById(projectId, pageId));
    }

    @SaCheckLogin
    @Log(title = "落地页", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody LandingPageBo bo
    ) {
        return R.ok(landingPageService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "落地页", businessType = BusinessType.DELETE)
    @DeleteMapping("/{pageId}")
    public R<Void> remove(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long pageId
    ) {
        return toAjax(landingPageService.deleteById(projectId, pageId));
    }

    /** FR-502~505 AI 生成落地页文案 */
    @SaCheckLogin
    @Log(title = "落地页", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{pageId}/generate")
    public R<LandingGenerateVo> generate(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long pageId,
        @RequestBody(required = false) LandingGenerateBo bo
    ) {
        return R.ok(landingPageService.generate(projectId, pageId, bo));
    }

    /** EPIC-6 M2 发布落地页 */
    @SaCheckLogin
    @Log(title = "落地页", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{pageId}/publish")
    public R<LandingPublishVo> publish(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long pageId
    ) {
        return R.ok(landingPageService.publish(projectId, pageId));
    }

    /** EPIC-6 M2 下线落地页 */
    @SaCheckLogin
    @Log(title = "落地页", businessType = BusinessType.UPDATE)
    @PostMapping("/{pageId}/unpublish")
    public R<LandingPublishVo> unpublish(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long pageId
    ) {
        return R.ok(landingPageService.unpublish(projectId, pageId));
    }
}
