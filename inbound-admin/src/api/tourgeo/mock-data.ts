import type { CustomerProject, DashboardKpi, DashboardTask, DashboardAlert, DiagnosticRunVO } from './types';

export const MOCK_PROJECTS: CustomerProject[] = [
  {
    id: 1,
    name: 'China Highlights US',
    brandName: 'China Highlights',
    targetMarkets: ['US', 'UK', 'AU'],
    defaultLocale: 'en-US'
  },
  {
    id: 2,
    name: 'Silk Road Adventures EU',
    brandName: 'Silk Road Adventures',
    targetMarkets: ['DE', 'FR', 'NL'],
    defaultLocale: 'en-GB'
  }
];

const RUNS_BY_PROJECT: Record<number, DiagnosticRunVO[]> = {
  1: [
    {
      id: 101,
      runId: 'DR-20260620-001',
      projectId: 1,
      name: 'Q2 US 入境 GEO 基线',
      market: 'US',
      locale: 'en-US',
      region: 'US',
      probeModes: ['grounded-api'],
      models: ['Perplexity', 'Gemini'],
      sampleCount: 3,
      status: 'SUCCESS',
      progress: 100,
      geoScore: 62.4,
      createdAt: '2026-06-20 09:30:00',
      finishedAt: '2026-06-20 10:15:00'
    },
    {
      id: 102,
      runId: 'DR-20260622-002',
      projectId: 1,
      name: 'UK 竞品对比采样',
      market: 'UK',
      locale: 'en-GB',
      region: 'UK',
      probeModes: ['grounded-api', 'browser-extension'],
      models: ['Perplexity', 'OpenAI'],
      sampleCount: 5,
      status: 'RUNNING',
      progress: 45,
      geoScore: null,
      createdAt: '2026-06-22 14:00:00',
      finishedAt: null
    },
    {
      id: 103,
      runId: 'DR-20260624-003',
      projectId: 1,
      name: '签证阶段长尾词',
      market: 'US',
      locale: 'en-US',
      region: 'US',
      probeModes: ['grounded-api'],
      models: ['Gemini'],
      sampleCount: 2,
      status: 'PARTIAL_FAILED',
      progress: 100,
      geoScore: 48.2,
      createdAt: '2026-06-24 11:20:00',
      finishedAt: '2026-06-24 12:05:00'
    },
    {
      id: 104,
      runId: 'DR-20260625-004',
      projectId: 1,
      name: 'AU 市场复测',
      market: 'AU',
      locale: 'en-AU',
      region: 'AU',
      probeModes: ['grounded-api'],
      models: ['Perplexity'],
      sampleCount: 3,
      status: 'PENDING',
      progress: null,
      geoScore: null,
      createdAt: '2026-06-25 08:00:00',
      finishedAt: null
    }
  ],
  2: [
    {
      id: 201,
      runId: 'DR-20260618-101',
      projectId: 2,
      name: 'EU DACH 首轮诊断',
      market: 'DE',
      locale: 'en-GB',
      region: 'EU',
      probeModes: ['grounded-api'],
      models: ['Perplexity', 'Gemini', 'OpenAI'],
      sampleCount: 3,
      status: 'FAILED',
      progress: 100,
      geoScore: null,
      createdAt: '2026-06-18 16:00:00',
      finishedAt: '2026-06-18 16:40:00'
    }
  ]
};

export function getMockRuns(projectId: number): DiagnosticRunVO[] {
  return RUNS_BY_PROJECT[projectId] ?? [];
}

export function getMockDashboard(projectId: number) {
  const runs = getMockRuns(projectId);
  const latestSuccess = runs.find((r) => r.status === 'SUCCESS' || r.status === 'PARTIAL_FAILED');
  const kpi: DashboardKpi = {
    geoScore: latestSuccess?.geoScore ?? null,
    geoScoreDate: latestSuccess?.finishedAt?.slice(0, 10) ?? null,
    contentCount: projectId === 1 ? 12 : 4,
    contentTrend: projectId === 1 ? 2 : -1,
    landingPageCount: projectId === 1 ? 5 : 2,
    landingTrend: projectId === 1 ? 1 : 0,
    leadCount: projectId === 1 ? 3 : 1,
    leadTrend: projectId === 1 ? 1 : 0
  };
  const tasks: DashboardTask[] =
    projectId === 1
      ? [
          { id: 't1', title: '审核脚本「Silk Road 10-day itinerary」', type: 'content' },
          { id: 't2', title: '确认落地页 FAQ 人工标记', type: 'landing' },
          { id: 't3', title: '跟进线索 #1024', type: 'lead' }
        ]
      : [];
  const alerts: DashboardAlert[] =
    projectId === 1
      ? [
          { id: 'a1', message: '竞品 Trip.com 在 US 市场可见率上升 12%', level: 'warning' },
          { id: 'a2', message: '本月 GEO 诊断额度已使用 80%', level: 'warning' }
        ]
      : [{ id: 'a3', message: '最近诊断任务失败，请查看日志', level: 'danger' }];
  return { kpi, tasks, alerts, recentRuns: runs.slice(0, 5) };
}

let nextRunId = 300;

export function appendMockRun(projectId: number, partial: Partial<DiagnosticRunVO>): DiagnosticRunVO {
  const run: DiagnosticRunVO = {
    id: nextRunId++,
    runId: `DR-${Date.now()}`,
    projectId,
    name: partial.name ?? '新诊断任务',
    market: partial.market ?? 'US',
    locale: partial.locale ?? 'en-US',
    region: partial.region ?? 'US',
    probeModes: partial.probeModes ?? ['grounded-api'],
    models: partial.models ?? ['Perplexity'],
    sampleCount: partial.sampleCount ?? 3,
    status: 'PENDING',
    progress: null,
    geoScore: null,
    createdAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
    finishedAt: null
  };
  if (!RUNS_BY_PROJECT[projectId]) {
    RUNS_BY_PROJECT[projectId] = [];
  }
  RUNS_BY_PROJECT[projectId].unshift(run);
  return run;
}

export const MOCK_SUGGESTION = {
  summary: '优先补「签证阶段」长尾问题库；建议新建 2 条内容任务覆盖 comparison 阶段关键词。'
};
