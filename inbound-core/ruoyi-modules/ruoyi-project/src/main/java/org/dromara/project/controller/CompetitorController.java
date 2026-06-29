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
import org.dromara.project.domain.bo.CompetitorBo;
import org.dromara.project.domain.vo.CompetitorVo;
import org.dromara.project.service.ICompetitorService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 竞品 API — FR-002
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/competitors")
public class CompetitorController extends BaseController {

    private final ICompetitorService competitorService;

    @SaCheckLogin
    @GetMapping
    public R<List<CompetitorVo>> list(@NotNull @PathVariable Long projectId) {
        return R.ok(competitorService.queryList(projectId));
    }

    @SaCheckLogin
    @GetMapping("/{competitorId}")
    public R<CompetitorVo> getInfo(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long competitorId
    ) {
        return R.ok(competitorService.queryById(projectId, competitorId));
    }

    @SaCheckLogin
    @Log(title = "竞品", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody CompetitorBo bo
    ) {
        return R.ok(competitorService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "竞品", businessType = BusinessType.UPDATE)
    @PutMapping("/{competitorId}")
    public R<Void> edit(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long competitorId,
        @Validated(EditGroup.class) @RequestBody CompetitorBo bo
    ) {
        bo.setId(competitorId);
        return toAjax(competitorService.updateByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "竞品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{competitorId}")
    public R<Void> remove(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long competitorId
    ) {
        return toAjax(competitorService.deleteById(projectId, competitorId));
    }
}
