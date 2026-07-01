package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbMapTypeHandler;
import org.dromara.project.support.PgLandingPageStatusTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "landing_page", autoResultMap = true)
public class LandingPage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    private Long keywordId;

    @TableField("template_type")
    private String templateType;

    private String title;

    private String slug;

    @TableField(value = "content_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> contentJson;

    @TableField(value = "seo_meta_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> seoMetaJson;

    @TableField(value = "form_config_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> formConfigJson;

    @TableField("whatsapp_link")
    private String whatsappLink;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgLandingPageStatusTypeHandler.class)
    private String status;

    @TableField("published_url")
    private String publishedUrl;

    @TableField("published_at")
    private OffsetDateTime publishedAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
