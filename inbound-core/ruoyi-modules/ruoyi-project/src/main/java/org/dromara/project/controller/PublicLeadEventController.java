package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.PublicLeadEventBo;
import org.dromara.project.domain.vo.PublicLeadEventVo;
import org.dromara.project.service.ILeadService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开渠道事件 beacon — EPIC-7 M3 FR-602
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/lead-events")
public class PublicLeadEventController extends BaseController {

    private final ILeadService leadService;

    @RateLimiter(time = 60, count = 30, limitType = LimitType.IP, key = "#bo.projectId")
    @PostMapping
    public R<PublicLeadEventVo> record(@Valid @RequestBody PublicLeadEventBo bo) {
        return R.ok(leadService.recordPublicEvent(bo));
    }
}
