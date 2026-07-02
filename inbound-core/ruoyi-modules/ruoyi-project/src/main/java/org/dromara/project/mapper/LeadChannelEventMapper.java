package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.LeadChannelEvent;
import org.dromara.project.domain.vo.WhatsappClickStatsVo;

public interface LeadChannelEventMapper extends BaseMapperPlus<LeadChannelEvent, LeadChannelEvent> {

    @Select("""
        SELECT COUNT(*)::bigint AS click_count, MAX(created_at) AS last_click_at
        FROM lead_channel_event
        WHERE project_id = #{projectId}
          AND landing_page_id = #{landingPageId}
          AND event_type = 'whatsapp_click'
        """)
    WhatsappClickStatsVo selectWhatsappClickStats(
        @Param("projectId") Long projectId,
        @Param("landingPageId") Long landingPageId
    );
}
