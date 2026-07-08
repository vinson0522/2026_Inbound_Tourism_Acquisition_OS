package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.PublicMarketingContactBo;
import org.dromara.project.domain.vo.PublicLeadSubmitVo;
import org.dromara.project.service.ILeadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 营销门户联系表单 — 咨询 TourGEO 产品（非客户落地页询盘）
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/marketing-contact")
public class PublicMarketingContactController extends BaseController {

    private final ILeadService leadService;

    @RateLimiter(time = 60, count = 5, limitType = LimitType.IP)
    @PostMapping
    public R<PublicLeadSubmitVo> submit(
        @Valid @RequestBody PublicMarketingContactBo bo,
        @RequestHeader(value = "X-Turnstile-Token", required = false) String turnstileToken
    ) {
        return R.ok(leadService.submitMarketingContact(bo, turnstileToken));
    }
}
