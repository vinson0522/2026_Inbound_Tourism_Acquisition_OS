package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbListTypeHandler;
import org.dromara.project.support.PgMaterialAssetTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@TableName(value = "material_asset", autoResultMap = true)
public class MaterialAsset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("project_id")
    private Long projectId;

    @TableField(value = "type", jdbcType = JdbcType.OTHER, typeHandler = PgMaterialAssetTypeHandler.class)
    private String type;

    private String url;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField(value = "tags_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> tags;

    @TableField("copyright_status")
    private String copyrightStatus;

    private String source;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
