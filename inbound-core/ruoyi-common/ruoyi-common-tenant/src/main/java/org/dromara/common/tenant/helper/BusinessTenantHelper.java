package org.dromara.common.tenant.helper;

import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;

/**
 * RuoYi tenant id (String, e.g. {@code 000000}) → business {@code tenant.id} (BIGINT).
 * <p>Mapping is loaded from {@code tenant.ruoyi_tenant_id} via {@link BusinessTenantLookup}.</p>
 */
public final class BusinessTenantHelper {

    private BusinessTenantHelper() {
    }

    public static Long getBusinessTenantId() {
        String ruoyiTenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(ruoyiTenantId)) {
            throw new ServiceException("未登录或缺少租户上下文", HttpStatus.UNAUTHORIZED);
        }
        return SpringUtils.getBean(BusinessTenantLookup.class).resolve(ruoyiTenantId);
    }
}
