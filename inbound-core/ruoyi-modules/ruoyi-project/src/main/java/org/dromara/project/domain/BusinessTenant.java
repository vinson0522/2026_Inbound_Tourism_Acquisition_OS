package org.dromara.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Business tenant row ({@code tenant} table) — FR-807 ruoyi mapping.
 */
@Data
@TableName("tenant")
public class BusinessTenant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String ruoyiTenantId;

    private String status;
}
