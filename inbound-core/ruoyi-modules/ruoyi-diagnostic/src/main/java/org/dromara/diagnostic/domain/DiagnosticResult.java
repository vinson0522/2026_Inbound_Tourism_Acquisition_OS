package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.diagnostic.support.PgJsonbListTypeHandler;
import org.dromara.diagnostic.support.PgJsonbMapTypeHandler;
import org.dromara.diagnostic.support.PgJsonbRawTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * GEO 诊断结果 diagnostic_result（001_schema.sql）
 */
@Data
@TableName(value = "diagnostic_result", autoResultMap = true)
public class DiagnosticResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    @TableField("run_id")
    private Long runId;

    @TableField("question_id")
    private Long questionId;

    private String platform;

    @TableField("probe_mode")
    private String probeMode;

    @TableField("probe_node_id")
    private Long probeNodeId;

    private String model;

    @TableField("answer_text")
    private String answerText;

    @TableField(value = "mentioned_brands_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> mentionedBrands;

    @TableField(value = "competitors_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> competitors;

    @TableField(value = "links_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> links;

    @TableField(value = "citations_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbRawTypeHandler.class)
    private String citationsJson;

    @TableField("capture_method")
    private String captureMethod;

    @TableField(value = "raw_response_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> rawResponseJson;

    @TableField("screenshot_url")
    private String screenshotUrl;

    private Integer rank;

    @TableField(value = "score_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> scoreJson;

    @TableField("human_corrected")
    private Boolean humanCorrected;

    @TableField("sampled_at")
    private OffsetDateTime sampledAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
