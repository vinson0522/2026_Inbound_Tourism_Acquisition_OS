package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.diagnostic.support.PgProbeTaskStatusTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * GEO 探针子任务 probe_task（001_schema.sql）
 */
@Data
@TableName(value = "probe_task", autoResultMap = true)
public class ProbeTask implements Serializable {

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

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgProbeTaskStatusTypeHandler.class)
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("error_message")
    private String errorMessage;

    @TableField("dispatched_at")
    private OffsetDateTime dispatchedAt;

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
