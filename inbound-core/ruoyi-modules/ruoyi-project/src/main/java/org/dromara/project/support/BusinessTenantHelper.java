package org.dromara.project.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;

/**
 * 若依租户 ID（String，如 000000）→ 业务库 tenant.id（BIGINT）桥接。
 * MVP：默认映射 demo 租户 id=1；纯数字则直接解析。
 */
public final class BusinessTenantHelper {

    private static final String DEFAULT_RUOYI_TENANT = "000000";
    private static final long DEFAULT_BUSINESS_TENANT_ID = 1L;

    private BusinessTenantHelper() {
    }

    public static Long getBusinessTenantId() {
        String ruoyiTenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(ruoyiTenantId) || DEFAULT_RUOYI_TENANT.equals(ruoyiTenantId)) {
            return DEFAULT_BUSINESS_TENANT_ID;
        }
        if (StringUtils.isNumeric(ruoyiTenantId)) {
            return Long.parseLong(ruoyiTenantId);
        }
        return DEFAULT_BUSINESS_TENANT_ID;
    }
}
