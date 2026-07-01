package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.LeadQueryBo;
import org.dromara.project.domain.vo.LeadDetailVo;
import org.dromara.project.domain.vo.LeadVo;
import org.dromara.project.service.ILeadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 线索列表/详情 — EPIC-7 M1 FR-601
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
}
