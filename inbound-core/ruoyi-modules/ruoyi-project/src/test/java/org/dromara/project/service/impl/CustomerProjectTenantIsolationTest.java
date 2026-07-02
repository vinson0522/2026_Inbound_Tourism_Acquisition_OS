package org.dromara.project.service.impl;

import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.service.IQuotaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * FR-807 — tenant A must not read tenant B project (403 / 404).
 */
@ExtendWith(MockitoExtension.class)
class CustomerProjectTenantIsolationTest {

    @Mock
    private CustomerProjectMapper baseMapper;

    @Mock
    private IQuotaService quotaService;

    @InjectMocks
    private CustomerProjectServiceImpl service;

    @Test
    void queryById_otherTenantProject_returns403() {
        CustomerProject tenantBProject = new CustomerProject();
        tenantBProject.setId(99L);
        tenantBProject.setTenantId(2L);
        when(baseMapper.selectById(99L)).thenReturn(tenantBProject);

        try (MockedStatic<BusinessTenantHelper> tenant = mockStatic(BusinessTenantHelper.class)) {
            tenant.when(BusinessTenantHelper::getBusinessTenantId).thenReturn(1L);

            assertThatThrownBy(() -> service.queryById(99L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ServiceException) ex).getCode())
                    .isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Test
    void queryById_missingProject_returns404() {
        when(baseMapper.selectById(404L)).thenReturn(null);

        try (MockedStatic<BusinessTenantHelper> tenant = mockStatic(BusinessTenantHelper.class)) {
            tenant.when(BusinessTenantHelper::getBusinessTenantId).thenReturn(1L);

            assertThatThrownBy(() -> service.queryById(404L))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((ServiceException) ex).getCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void queryById_sameTenantProject_ok() {
        CustomerProject owned = new CustomerProject();
        owned.setId(1L);
        owned.setTenantId(1L);
        when(baseMapper.selectById(1L)).thenReturn(owned);

        try (MockedStatic<BusinessTenantHelper> tenant = mockStatic(BusinessTenantHelper.class)) {
            tenant.when(BusinessTenantHelper::getBusinessTenantId).thenReturn(1L);
            service.queryById(1L);
        }
    }
}
