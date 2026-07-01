package org.dromara.diagnostic.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.diagnostic.domain.bo.ReportQueryBo;
import org.dromara.diagnostic.domain.bo.WeeklyReportBo;
import org.dromara.diagnostic.domain.vo.ReportDetailVo;
import org.dromara.diagnostic.domain.vo.ReportVo;
import org.dromara.diagnostic.report.DiagnosticReportFile;

/**
 * 报告中心 — EPIC-8 FR-701/702
 */
public interface IReportService {

    TableDataInfo<ReportVo> queryPageList(Long projectId, ReportQueryBo bo, PageQuery pageQuery);

    ReportDetailVo queryById(Long projectId, Long reportId);

    Long createWeeklyReport(Long projectId, WeeklyReportBo bo);

    DiagnosticReportFile exportReport(Long projectId, Long reportId, String format);
}
