package org.dromara.diagnostic.service;

import org.dromara.diagnostic.domain.bo.DiagnosticScheduleUpsertBo;
import org.dromara.diagnostic.domain.vo.DiagnosticScheduleVo;

public interface IDiagnosticScheduleService {

    DiagnosticScheduleVo getByProject(Long projectId);

    DiagnosticScheduleVo upsert(Long projectId, DiagnosticScheduleUpsertBo bo);

    /** 每小时 Job：处理所有到期且启用的计划 */
    void processDueSchedules();

    /**
     * 内网/smoke 触发单项目计划。
     *
     * @param force true 时忽略 next_run_at 检查
     * @return 创建的 runId；额度不足或未启用时返回 null
     */
    Long triggerForProject(Long projectId, boolean force);
}
