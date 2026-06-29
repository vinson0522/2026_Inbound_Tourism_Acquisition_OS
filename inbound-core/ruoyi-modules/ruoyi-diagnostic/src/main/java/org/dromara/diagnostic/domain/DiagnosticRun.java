package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.diagnostic.support.PgDiagnosticRunStatusTypeHandler;
import org.dromara.diagnostic.support.PgJsonbListTypeHandler;
import org.dromara.diagnostic.support.PgJsonbMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * GEO 诊断任务 diagnostic_run（001_schema.sql）
 */
@Data
@TableName(value = "diagnostic_run", autoResultMap = true)
public class DiagnosticRun implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    private String name;

    private String market;

    private String locale;

    private String region;

    @TableField(value = "probe_modes_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> probeModes;

    @TableField("calibration_ratio")
    private BigDecimal calibrationRatio;

    @TableField(value = "models_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> models;

    @TableField("sample_count")
    private Integer sampleCount;

    @TableField(value = "question_scope_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> questionScope;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgDiagnosticRunStatusTypeHandler.class)
    private String status;

    @TableField("geo_score")
    private BigDecimal geoScore;

    @TableField("started_at")
    private OffsetDateTime startedAt;

    @TableField("finished_at")
    private OffsetDateTime finishedAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
