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
import org.dromara.project.domain.bo.ContentGenerateBo;
import org.dromara.project.domain.bo.ContentTaskBo;
import org.dromara.project.domain.vo.ContentGenerateVo;
import org.dromara.project.domain.vo.ContentTaskDetailVo;
import org.dromara.project.domain.vo.ContentTaskVo;
import org.dromara.project.service.IContentTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 内容任务 API — EPIC-4 M1 FR-301/302
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/content-tasks")
public class ContentTaskController extends BaseController {

    private final IContentTaskService contentTaskService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<ContentTaskVo> list(
        @NotNull @PathVariable Long projectId,
        ContentTaskBo bo,
        PageQuery pageQuery
    ) {
        return contentTaskService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{taskId}")
    public R<ContentTaskDetailVo> get(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long taskId
    ) {
        return R.ok(contentTaskService.queryById(projectId, taskId));
    }

    @SaCheckLogin
    @Log(title = "内容任务", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(
        @NotNull @PathVariable Long projectId,
        @Validated(AddGroup.class) @RequestBody ContentTaskBo bo
    ) {
        return R.ok(contentTaskService.insertByBo(projectId, bo));
    }

    @SaCheckLogin
    @Log(title = "内容任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskId}")
    public R<Void> remove(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long taskId
    ) {
        return toAjax(contentTaskService.deleteById(projectId, taskId));
    }

    /** FR-301/302 AI 生成脚本 */
    @SaCheckLogin
    @Log(title = "内容任务", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{taskId}/generate")
    public R<ContentGenerateVo> generate(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long taskId,
        @RequestBody(required = false) ContentGenerateBo bo
    ) {
        return R.ok(contentTaskService.generate(projectId, taskId, bo));
    }
}
