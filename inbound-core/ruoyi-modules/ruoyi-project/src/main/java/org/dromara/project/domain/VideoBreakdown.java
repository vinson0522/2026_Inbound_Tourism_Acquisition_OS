package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbListMapTypeHandler;
import org.dromara.project.support.PgJsonbMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "video_breakdown", autoResultMap = true)
public class VideoBreakdown implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("project_id")
    private Long projectId;

    @TableField("source_url")
    private String sourceUrl;

    @TableField(value = "frames_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListMapTypeHandler.class)
    private List<Map<String, Object>> framesJson;

    @TableField(value = "dimensions_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> dimensionsJson;

    @TableField("reusable_structure")
    private String reusableStructure;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
