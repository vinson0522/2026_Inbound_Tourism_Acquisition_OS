package org.dromara.common.tenant.helper;

/**
 * Resolves RuoYi login tenant id → business {@code tenant.id} (FR-807).
 */
public interface BusinessTenantLookup {

    /**
     * @param ruoyiTenantId from {@link TenantHelper#getTenantId()}
     * @return business tenant primary key
     */
    Long resolve(String ruoyiTenantId);
}
