package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgEntityStatusTypeHandler;
import org.dromara.project.support.PgJsonbListTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "probe_node", autoResultMap = true)
public class ProbeNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("node_key")
    private String nodeKey;

    private String region;

    @TableField(value = "platforms_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> platforms;

    @TableField("extension_version")
    private String extensionVersion;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgEntityStatusTypeHandler.class)
    private String status;

    @TableField(value = "rate_limit_json", jdbcType = JdbcType.OTHER, typeHandler = org.dromara.diagnostic.support.PgJsonbMapTypeHandler.class)
    private Map<String, Object> rateLimitJson;

    @TableField("last_heartbeat_at")
    private OffsetDateTime lastHeartbeatAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
