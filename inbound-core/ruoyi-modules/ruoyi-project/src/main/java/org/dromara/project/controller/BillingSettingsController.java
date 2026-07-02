package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.SubscriptionUpdateBo;
import org.dromara.project.domain.vo.SubscriptionVo;
import org.dromara.project.service.ISubscriptionService;
import org.dromara.project.support.BusinessTenantHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 套餐与额度 — EPIC-9 M1 FR-804 · M2 套餐 CRUD
 */
@Validated
@SaCheckLogin
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/settings/billing")
public class BillingSettingsController extends BaseController {

    private final ISubscriptionService subscriptionService;

    @GetMapping
    public R<SubscriptionVo> getCurrentSubscription() {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(subscriptionService.getCurrentSubscription(tenantId));
    }

    @PutMapping("/subscription")
    public R<SubscriptionVo> updateSubscription(@Valid @RequestBody SubscriptionUpdateBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(subscriptionService.updateSubscription(tenantId, bo));
    }

    @PostMapping("/period-reset")
    public R<SubscriptionVo> resetPeriod() {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(subscriptionService.resetPeriod(tenantId));
    }
}
