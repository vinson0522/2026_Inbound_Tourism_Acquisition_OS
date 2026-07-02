package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.LeadFollowupCreateBo;
import org.dromara.project.domain.bo.LeadQueryBo;
import org.dromara.project.domain.bo.LeadUpdateBo;
import org.dromara.project.domain.vo.LeadDetailVo;
import org.dromara.project.domain.vo.LeadFollowupVo;
import org.dromara.project.domain.vo.LeadVo;
import org.dromara.project.service.ILeadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin 线索列表/详情/CRM — EPIC-7 M1 FR-601 · M2 FR-605
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}/leads")
public class LeadController extends BaseController {

    private final ILeadService leadService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<LeadVo> list(
        @NotNull @PathVariable Long projectId,
        LeadQueryBo bo,
        PageQuery pageQuery
    ) {
        return leadService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @GetMapping("/{leadId}")
    public R<LeadDetailVo> get(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long leadId
    ) {
        return R.ok(leadService.queryById(projectId, leadId));
    }

    /** FR-605 更新线索状态 / 负责人 */
    @SaCheckLogin
    @Log(title = "线索", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PatchMapping("/{leadId}")
    public R<LeadDetailVo> patch(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long leadId,
        @RequestBody LeadUpdateBo bo
    ) {
        return R.ok(leadService.updateLead(projectId, leadId, bo));
    }

    /** FR-605 跟进记录列表（时间 ASC） */
    @SaCheckLogin
    @GetMapping("/{leadId}/followups")
    public R<List<LeadFollowupVo>> listFollowups(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long leadId
    ) {
        return R.ok(leadService.listFollowups(projectId, leadId));
    }

    /** FR-605 新增跟进记录 */
    @SaCheckLogin
    @Log(title = "线索跟进", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/{leadId}/followups")
    public R<LeadFollowupVo> createFollowup(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long leadId,
        @Validated @RequestBody LeadFollowupCreateBo bo
    ) {
        return R.ok(leadService.createFollowup(projectId, leadId, bo));
    }
}
