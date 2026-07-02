package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.diagnostic.support.PgDiagnosticScheduleFrequencyTypeHandler;
import org.dromara.diagnostic.support.PgJsonbListTypeHandler;
import org.dromara.diagnostic.support.PgJsonbMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * GEO 诊断定时计划 diagnostic_schedule（FR-109）
 */
@Data
@TableName(value = "diagnostic_schedule", autoResultMap = true)
public class DiagnosticSchedule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    @TableField(value = "frequency", jdbcType = JdbcType.OTHER, typeHandler = PgDiagnosticScheduleFrequencyTypeHandler.class)
    private String frequency;

    private Boolean enabled;

    private String market;

    private String locale;

    private String region;

    @TableField(value = "probe_modes_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> probeModes;

    @TableField(value = "models_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> models;

    @TableField("sample_count")
    private Integer sampleCount;

    @TableField(value = "question_scope_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> questionScope;

    @TableField("calibration_ratio")
    private BigDecimal calibrationRatio;

    @TableField("next_run_at")
    private OffsetDateTime nextRunAt;

    @TableField("last_run_id")
    private Long lastRunId;

    @TableField("last_triggered_at")
    private OffsetDateTime lastTriggeredAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;
}
