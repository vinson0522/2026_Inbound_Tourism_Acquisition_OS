package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.KeywordOpportunity;
import org.dromara.project.domain.vo.KeywordOpportunityVo;

import java.math.BigDecimal;

public interface KeywordOpportunityMapper extends BaseMapperPlus<KeywordOpportunity, KeywordOpportunityVo> {

    @Select("""
        SELECT geo_score FROM diagnostic_run
        WHERE tenant_id = #{tenantId}
          AND project_id = #{projectId}
          AND deleted_at IS NULL
          AND status IN ('SUCCESS'::diagnostic_run_status, 'PARTIAL_FAILED'::diagnostic_run_status)
          AND geo_score IS NOT NULL
        ORDER BY created_at DESC
        LIMIT 1
        """)
    BigDecimal selectLatestGeoScore(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
}
