package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbListTypeHandler;
import org.dromara.project.support.PgKnowledgeAssetTypeHandler;
import org.dromara.project.support.PgVectorIndexStatusTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "knowledge_asset", autoResultMap = true)
public class KnowledgeAsset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    @TableField(value = "type", jdbcType = JdbcType.OTHER, typeHandler = PgKnowledgeAssetTypeHandler.class)
    private String type;

    private String title;

    private String content;

    @TableField("file_url")
    private String fileUrl;

    @TableField(value = "tags_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> tags;

    @TableField(value = "vector_status", jdbcType = JdbcType.OTHER, typeHandler = PgVectorIndexStatusTypeHandler.class)
    private String vectorStatus;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
