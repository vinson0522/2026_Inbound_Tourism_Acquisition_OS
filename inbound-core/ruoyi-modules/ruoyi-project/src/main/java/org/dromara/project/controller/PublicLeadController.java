package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.PublicLeadSubmitBo;
import org.dromara.project.domain.vo.PublicLeadSubmitVo;
import org.dromara.project.service.ILeadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开询盘表单 — EPIC-7 M1 FR-601
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/leads")
public class PublicLeadController extends BaseController {

    private final ILeadService leadService;

    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP, key = "#bo.landingPageId")
    @PostMapping
    public R<PublicLeadSubmitVo> submit(
        @Valid @RequestBody PublicLeadSubmitBo bo,
        @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken
    ) {
        return R.ok(leadService.submitPublic(bo, turnstileToken));
    }
}
