package org.dromara.diagnostic.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.diagnostic.domain.bo.CreateDiagnosticBo;
import org.dromara.diagnostic.domain.bo.ProbeCallbackBo;
import org.dromara.diagnostic.domain.vo.DiagnosticCalibrationVo;
import org.dromara.diagnostic.domain.vo.DiagnosticResultVo;
import org.dromara.diagnostic.domain.vo.DiagnosticRunVo;
import org.dromara.diagnostic.domain.vo.DiagnosticTrendsVo;
import org.dromara.diagnostic.domain.vo.ProbeTaskVo;

import java.util.List;

public interface IDiagnosticRunService {

    Long createRun(Long projectId, CreateDiagnosticBo bo);

    void handleProbeCallback(ProbeCallbackBo bo);

    TableDataInfo<DiagnosticRunVo> queryPageList(Long projectId, PageQuery pageQuery);

    DiagnosticRunVo queryById(Long runId);

    List<DiagnosticResultVo> queryResults(Long runId);

    List<ProbeTaskVo> queryProbeTasks(Long runId);

    /** FR-108 诊断趋势序列（SUCCESS/PARTIAL_FAILED + geo_score 非空） */
    DiagnosticTrendsVo queryTrends(Long projectId, int limit, String market);

    /** FR-115 API vs browser-extension 校准对比 */
    DiagnosticCalibrationVo queryCalibration(Long projectId, Long runId);
}
