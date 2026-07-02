package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "lead_channel_event", autoResultMap = true)
public class LeadChannelEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    @TableField("landing_page_id")
    private Long landingPageId;

    @TableField("event_type")
    private String eventType;

    @TableField(value = "utm_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> utmJson;

    private String device;

    @TableField("ip_hash")
    private String ipHash;

    @TableField("created_at")
    private OffsetDateTime createdAt;
}
