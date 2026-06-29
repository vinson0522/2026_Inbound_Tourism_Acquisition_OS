package org.dromara.project.domain;

import org.apache.ibatis.type.JdbcType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.project.support.PgEntityStatusTypeHandler;
import org.dromara.project.support.PgJsonbListTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 客户项目 customer_project（001_schema.sql）
 */
@Data
@TableName(value = "customer_project", autoResultMap = true)
public class CustomerProject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    @TableField("brand_name")
    private String brandName;

    private String website;

    private String industry;

    @TableField(value = "target_markets_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> targetMarkets;

    @TableField(value = "languages_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbListTypeHandler.class)
    private List<String> languages;

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
