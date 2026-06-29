package org.dromara.diagnostic.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.dromara.diagnostic.support.PgReportTypeTypeHandler;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@TableName(value = "report", autoResultMap = true)
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long projectId;

    @TableField(value = "type", jdbcType = JdbcType.OTHER, typeHandler = PgReportTypeTypeHandler.class)
    private String type;

    private String period;

    @TableField("file_url")
    private String fileUrl;

    private String summary;

    @TableField("template_id")
    private Long templateId;

    @TableField("created_at")
    private OffsetDateTime createdAt;

    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    @TableField("deleted_at")
    private OffsetDateTime deletedAt;

    @TableField("created_by")
    private Long createdBy;
}
