package org.dromara.project.support;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.project.mapper.BusinessTenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessTenantLookupImplTest {

    @Mock
    private BusinessTenantMapper businessTenantMapper;

    @InjectMocks
    private BusinessTenantLookupImpl lookup;

    @BeforeEach
    void setUp() {
        lookup = new BusinessTenantLookupImpl(businessTenantMapper);
    }

    @Test
    void resolve_mapsRuoyiTenantId000000_toBusinessTenant1() {
        when(businessTenantMapper.selectIdByRuoyiTenantId("000000")).thenReturn(1L);
        assertThat(lookup.resolve("000000")).isEqualTo(1L);
    }

    @Test
    void resolve_mapsRuoyiTenantId000001_toBusinessTenant2() {
        when(businessTenantMapper.selectIdByRuoyiTenantId("000001")).thenReturn(2L);
        assertThat(lookup.resolve("000001")).isEqualTo(2L);
    }

    @Test
    void resolve_numericFallback_whenNoMappingRow() {
        when(businessTenantMapper.selectIdByRuoyiTenantId("2")).thenReturn(null);
        when(businessTenantMapper.countActiveById(2L)).thenReturn(1);
        assertThat(lookup.resolve("2")).isEqualTo(2L);
    }

    @Test
    void resolve_unknownTenant_throws403() {
        when(businessTenantMapper.selectIdByRuoyiTenantId("999999")).thenReturn(null);
        when(businessTenantMapper.countActiveById(999999L)).thenReturn(0);
        assertThatThrownBy(() -> lookup.resolve("999999"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无效租户");
    }

    @Test
    void resolve_blankTenant_throws401() {
        assertThatThrownBy(() -> lookup.resolve(" "))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未登录");
    }
}
