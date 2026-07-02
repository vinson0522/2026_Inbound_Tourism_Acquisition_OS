package org.dromara.diagnostic.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.diagnostic.domain.bo.PlatformAdapterSaveBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterAdminVo;
import org.dromara.diagnostic.service.IPlatformAdapterSettingsService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台 Adapter 租户配置 — EPIC-11 FR-116
 */
@SaCheckLogin
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/settings/platform-adapters")
public class PlatformAdapterSettingsController extends BaseController {

    private final IPlatformAdapterSettingsService platformAdapterSettingsService;

    @GetMapping
    public R<List<PlatformAdapterAdminVo>> listAdapters() {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(platformAdapterSettingsService.listAdapters(tenantId));
    }

    @GetMapping("/{platform}")
    public R<PlatformAdapterAdminVo> getAdapter(@PathVariable String platform) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(platformAdapterSettingsService.getAdapter(tenantId, platform));
    }

    @Log(title = "平台Adapter", businessType = BusinessType.UPDATE)
    @PutMapping("/{platform}")
    public R<PlatformAdapterAdminVo> saveAdapter(
        @PathVariable String platform,
        @RequestBody PlatformAdapterSaveBo bo
    ) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        return R.ok(platformAdapterSettingsService.saveAdapter(tenantId, platform, bo));
    }
}
