import type { CreateDiagnosticForm, DashboardKpi, DashboardTask, DashboardAlert, DiagnosticRunQuery, DiagnosticRunVO, PageResult } from './types';
import { appendMockRun, getMockDashboard, getMockRuns, MOCK_SUGGESTION } from './mock-data';

const USE_MOCK = import.meta.env.VITE_TOURGEO_MOCK !== 'false';

export interface DashboardData {
  kpi: DashboardKpi;
  tasks: DashboardTask[];
  alerts: DashboardAlert[];
  recentRuns: DiagnosticRunVO[];
  suggestion: { summary: string };
}

export async function getDashboard(projectId: number): Promise<DashboardData> {
  if (USE_MOCK) {
    await delay(300);
    const { kpi, tasks, alerts, recentRuns } = getMockDashboard(projectId);
    return { kpi, tasks, alerts, recentRuns, suggestion: MOCK_SUGGESTION };
  }
  // TODO: aggregate API
  throw new Error('Dashboard API not implemented');
}

export async function listDiagnosticRuns(projectId: number, query: DiagnosticRunQuery): Promise<PageResult<DiagnosticRunVO>> {
  if (USE_MOCK) {
    await delay(350);
    let rows = getMockRuns(projectId);
    if (query.name) {
      rows = rows.filter((r) => r.name.includes(query.name!));
    }
    if (query.status) {
      rows = rows.filter((r) => r.status === query.status);
    }
    if (query.market) {
      rows = rows.filter((r) => r.market === query.market);
    }
    if (query.probeMode) {
      rows = rows.filter((r) => r.probeModes.includes(query.probeMode!));
    }
    const total = rows.length;
    const start = (query.pageNum - 1) * query.pageSize;
    rows = rows.slice(start, start + query.pageSize);
    return { rows, total };
  }
  // TODO: GET /api/v1/projects/{projectId}/diagnostics
  throw new Error('Diagnostic list API not implemented');
}

export async function createDiagnosticRun(projectId: number, form: CreateDiagnosticForm): Promise<DiagnosticRunVO> {
  if (USE_MOCK) {
    await delay(400);
    return appendMockRun(projectId, {
      name: form.name,
      market: form.market,
      locale: form.locale,
      region: form.region,
      probeModes: form.probeModes,
      models: form.models,
      sampleCount: form.sampleCount
    });
  }
  // TODO: POST /api/v1/projects/{projectId}/diagnostics
  throw new Error('Create diagnostic API not implemented');
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
