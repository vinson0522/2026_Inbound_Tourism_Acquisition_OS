import request from '@/utils/request';
import { blobValidate } from '@/utils/ruoyi';
import FileSaver from 'file-saver';
import type {
  MonthlyReportForm,
  PageResult,
  ReportDetailVo,
  ReportQuery,
  ReportTemplateSaveForm,
  ReportTemplateVo,
  ReportVo,
  WeeklyReportForm
} from './types';

const BASE = '/api/v1';

export async function listReports(projectId: number, query: ReportQuery): Promise<PageResult<ReportVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/reports`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      type: query.type || undefined,
      period: query.period || undefined
    }
  });
  const rows = (res.rows ?? []) as ReportVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function getReport(projectId: number, reportId: number): Promise<ReportDetailVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/reports/${reportId}`,
    method: 'get'
  });
  return res.data as ReportDetailVo;
}

export async function createWeeklyReport(projectId: number, data: WeeklyReportForm): Promise<number> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/reports/weekly`,
    method: 'post',
    data
  });
  const payload = res.data;
  if (typeof payload === 'number') return payload;
  if (payload && typeof payload === 'object' && 'reportId' in payload) {
    return Number((payload as { reportId: number }).reportId);
  }
  return Number(payload);
}

export async function createMonthlyReport(projectId: number, data: MonthlyReportForm): Promise<number> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/reports/monthly`,
    method: 'post',
    data
  });
  const payload = res.data;
  if (typeof payload === 'number') return payload;
  if (payload && typeof payload === 'object' && 'reportId' in payload) {
    return Number((payload as { reportId: number }).reportId);
  }
  return Number(payload);
}

const TEMPLATE_BASE = `${BASE}/settings/report-template`;

export async function getReportTemplate(): Promise<ReportTemplateVo> {
  const res = await request({
    url: TEMPLATE_BASE,
    method: 'get'
  });
  return res.data as ReportTemplateVo;
}

export async function saveReportTemplate(data: ReportTemplateSaveForm): Promise<ReportTemplateVo> {
  const res = await request({
    url: TEMPLATE_BASE,
    method: 'put',
    data
  });
  return res.data as ReportTemplateVo;
}

export async function downloadReport(
  projectId: number,
  reportId: number,
  format: 'docx' | 'pdf',
  filename: string
): Promise<void> {
  const blob = await request({
    url: `${BASE}/projects/${projectId}/reports/${reportId}/export`,
    method: 'get',
    params: { format },
    responseType: 'blob'
  });
  if (!blobValidate(blob)) {
    const text = await blob.text();
    try {
      const rsp = JSON.parse(text) as { msg?: string; message?: string };
      throw new Error(rsp.msg || rsp.message || 'export failed');
    } catch (e) {
      if (e instanceof Error && e.message !== 'export failed') {
        throw e;
      }
      throw new Error(format === 'pdf' ? 'PDF 服务不可用，请下载 DOCX 或联系管理员' : 'export failed');
    }
  }
  FileSaver.saveAs(blob, filename);
}
