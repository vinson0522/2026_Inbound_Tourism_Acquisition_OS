package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbListMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "generated_content", autoResultMap = true)
public class GeneratedContent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    @TableField("task_id")
    private Long taskId;

    private String title;

    private String hook;

    @TableField("target_audience")
    private String targetAudience;

    private String script;

    @TableField(value = "storyboard_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListMapTypeHandler.class)
    private List<Map<String, Object>> storyboardJson;

    private String voiceover;

    @TableField("on_screen_text")
    private String onScreenText;

    private String hashtags;

    private String cta;

    @TableField("landing_page_suggestion")
    private String landingPageSuggestion;

    @TableField("needs_human_review")
    private Boolean needsHumanReview;

    private Integer version;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
