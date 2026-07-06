package org.dromara.project.support;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.BusinessTenantLookup;
import org.dromara.project.mapper.BusinessTenantMapper;
import org.springframework.stereotype.Service;

/**
 * Maps RuoYi {@code sys_tenant.tenant_id} → business {@code tenant.id} (FR-807).
 */
@Service
@RequiredArgsConstructor
public class BusinessTenantLookupImpl implements BusinessTenantLookup {

    private final BusinessTenantMapper businessTenantMapper;

    @Override
    public Long resolve(String ruoyiTenantId) {
        if (StringUtils.isBlank(ruoyiTenantId)) {
            throw new ServiceException("未登录或缺少租户上下文", HttpStatus.UNAUTHORIZED);
        }
        ruoyiTenantId = ruoyiTenantId.trim();

        Long mapped = businessTenantMapper.selectIdByRuoyiTenantId(ruoyiTenantId);
        if (mapped != null) {
            return mapped;
        }

        if (StringUtils.isNumeric(ruoyiTenantId)) {
            long numeric = Long.parseLong(ruoyiTenantId);
            String padded = String.format("%06d", numeric);
            if (!padded.equals(ruoyiTenantId)) {
                mapped = businessTenantMapper.selectIdByRuoyiTenantId(padded);
                if (mapped != null) {
                    return mapped;
                }
            }
            // Only treat plain numeric ids (e.g. "2") as business tenant primary keys.
            if (ruoyiTenantId.equals(Long.toString(numeric))
                && businessTenantMapper.countActiveById(numeric) > 0) {
                return numeric;
            }
        }

        throw new ServiceException("无效租户或无权访问", HttpStatus.FORBIDDEN);
    }
}
