package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbMapTypeHandler;
import org.dromara.project.support.PgSubscriptionStatusTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "subscription", autoResultMap = true)
public class Subscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("plan_code")
    private String planCode;

    @TableField(value = "quota_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> quotaJson;

    @TableField(value = "used_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> usedJson;

    @TableField("period_start")
    private LocalDate periodStart;

    @TableField("period_end")
    private LocalDate periodEnd;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgSubscriptionStatusTypeHandler.class)
    private String status;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
