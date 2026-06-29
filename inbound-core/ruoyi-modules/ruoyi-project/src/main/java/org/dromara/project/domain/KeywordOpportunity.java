package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgEntityStatusTypeHandler;
import org.dromara.project.support.PgJsonbMapTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "keyword_opportunity", autoResultMap = true)
public class KeywordOpportunity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    private String keyword;

    @TableField("keyword_en")
    private String keywordEn;

    @TableField("keyword_cn")
    private String keywordCn;

    private String intent;

    private String market;

    private String stage;

    private BigDecimal score;

    @TableField(value = "score_detail_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> scoreDetailJson;

    private String channel;

    @TableField(value = "source_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> sourceJson;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgEntityStatusTypeHandler.class)
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
