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
import org.dromara.project.domain.bo.KeywordGenerateBo;
import org.dromara.project.domain.bo.KeywordOpportunityBo;
import org.dromara.project.domain.bo.KeywordScoreBatchBo;
import org.dromara.project.domain.vo.KeywordGenerateVo;
import org.dromara.project.domain.vo.KeywordOpportunityVo;
import org.dromara.project.domain.vo.KeywordScoreBatchVo;
import org.dromara.project.domain.vo.KeywordScoreVo;
import org.dromara.project.service.IKeywordOpportunityService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 关键词机会词 API — EPIC-3 M1 FR-201/202
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/keywords")
public class KeywordOpportunityController extends BaseController {

    private final IKeywordOpportunityService keywordOpportunityService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<KeywordOpportunityVo> list(
        @NotNull @PathVariable Long projectId,
        KeywordOpportunityBo bo,
        PageQuery pageQuery
    ) {
        return keywordOpportunityService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @Log(title = "关键词机会词", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody KeywordOpportunityBo bo
    ) {
        return R.ok(keywordOpportunityService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "关键词机会词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{keywordId}")
    public R<Void> remove(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long keywordId
    ) {
        return toAjax(keywordOpportunityService.deleteById(projectId, keywordId));
    }

    /** FR-201 AI 生成机会词 */
    @SaCheckLogin
    @Log(title = "关键词机会词", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/generate")
    public R<KeywordGenerateVo> generate(
        @NotNull @PathVariable Long projectId,
        @Validated @RequestBody KeywordGenerateBo bo
    ) {
        return R.ok(keywordOpportunityService.generateKeywords(projectId, bo));
    }

    /** FR-203 单条关键词机会评分 */
    @SaCheckLogin
    @Log(title = "关键词机会词", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{keywordId}/score")
    public R<KeywordScoreVo> score(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long keywordId,
        @RequestParam(required = false) Boolean useRag
    ) {
        return R.ok(keywordOpportunityService.scoreKeyword(projectId, keywordId, useRag));
    }

    /** FR-203 批量关键词机会评分（最多 50 条；空 keywordIds = ACTIVE 全量） */
    @SaCheckLogin
    @Log(title = "关键词机会词", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/score-batch")
    public R<KeywordScoreBatchVo> scoreBatch(
        @NotNull @PathVariable Long projectId,
        @RequestBody(required = false) KeywordScoreBatchBo bo
    ) {
        return R.ok(keywordOpportunityService.scoreBatch(projectId, bo != null ? bo : new KeywordScoreBatchBo()));
    }
}
