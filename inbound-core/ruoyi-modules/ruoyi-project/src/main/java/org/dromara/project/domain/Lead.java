package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.project.support.PgJsonbMapTypeHandler;
import org.dromara.project.support.PgLeadStatusTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "lead", autoResultMap = true)
public class Lead implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    @TableField("landing_page_id")
    private Long landingPageId;

    @TableField("keyword_id")
    private Long keywordId;

    private String name;

    private String email;

    private String phone;

    @TableField("travel_date")
    private LocalDate travelDate;

    @TableField("party_size")
    private Integer partySize;

    private String budget;

    private String message;

    private String source;

    @TableField(value = "utm_json", jdbcType = JdbcType.OTHER, typeHandler = PgJsonbMapTypeHandler.class)
    private Map<String, Object> utmJson;

    private String device;

    @TableField(value = "status", jdbcType = JdbcType.OTHER, typeHandler = PgLeadStatusTypeHandler.class)
    private String status;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
