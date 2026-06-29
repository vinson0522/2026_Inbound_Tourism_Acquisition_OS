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
import org.dromara.project.domain.bo.KnowledgeAssetBo;
import org.dromara.project.domain.vo.KnowledgeAssetVo;
import org.dromara.project.service.IKnowledgeAssetService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 知识库资产 API — EPIC-10 Phase 2 / FR-004
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/knowledge-assets")
public class KnowledgeAssetController extends BaseController {

    private final IKnowledgeAssetService knowledgeAssetService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<KnowledgeAssetVo> list(
        @NotNull @PathVariable Long projectId,
        PageQuery pageQuery
    ) {
        return knowledgeAssetService.queryPageList(projectId, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{assetId}")
    public R<KnowledgeAssetVo> getInfo(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long assetId
    ) {
        return R.ok(knowledgeAssetService.queryById(projectId, assetId));
    }

    @SaCheckLogin
    @Log(title = "知识库资产", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody KnowledgeAssetBo bo
    ) {
        bo.setProjectId(projectId);
        return R.ok(knowledgeAssetService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "知识库资产", businessType = BusinessType.UPDATE)
    @PostMapping("/{assetId}/reindex")
    public R<Void> reindex(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long assetId
    ) {
        knowledgeAssetService.triggerReindex(projectId, assetId);
        return R.ok();
    }
}
