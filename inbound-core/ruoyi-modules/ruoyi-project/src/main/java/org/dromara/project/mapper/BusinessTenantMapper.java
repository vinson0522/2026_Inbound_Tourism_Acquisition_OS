package org.dromara.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.dromara.project.domain.BusinessTenant;

public interface BusinessTenantMapper extends BaseMapper<BusinessTenant> {

    @Select("""
        SELECT id FROM tenant
        WHERE ruoyi_tenant_id = #{ruoyiTenantId}
          AND status = 'ACTIVE'
          AND deleted_at IS NULL
        LIMIT 1
        """)
    Long selectIdByRuoyiTenantId(String ruoyiTenantId);

    @Select("""
        SELECT COUNT(1) FROM tenant
        WHERE id = #{id}
          AND status = 'ACTIVE'
          AND deleted_at IS NULL
        """)
    int countActiveById(Long id);
}
