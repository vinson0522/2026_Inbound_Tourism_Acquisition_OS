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

        Long mapped = businessTenantMapper.selectIdByRuoyiTenantId(ruoyiTenantId.trim());
        if (mapped != null) {
            return mapped;
        }

        if (StringUtils.isNumeric(ruoyiTenantId)) {
            Long numericId = Long.parseLong(ruoyiTenantId);
            if (businessTenantMapper.countActiveById(numericId) > 0) {
                return numericId;
            }
        }

        throw new ServiceException("无效租户或无权访问", HttpStatus.FORBIDDEN);
    }
}
