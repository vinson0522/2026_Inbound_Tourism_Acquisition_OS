import request from '@/utils/request';
import type {
  CreateDiagnosticForm,
  DashboardData,
  DiagnosticCitationVO,
  DiagnosticMetrics,
  DiagnosticResultVO,
  DiagnosticRunQuery,
  DiagnosticRunVO,
  PageResult,
  ProbeTaskVO
} from './types';

export type { DashboardData } from './types';

const BASE = '/api/v1';

export async function listDiagnosticRuns(
  projectId: number,
  query: DiagnosticRunQuery
): Promise<PageResult<DiagnosticRunVO>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/diagnostics`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      status: query.status || undefined,
      market: query.market || undefined
    }
  });
  const rows = (res.rows || []).map(mapRunVo);
  return { rows, total: res.total ?? rows.length };
}

export async function createDiagnosticRun(
  projectId: number,
  form: CreateDiagnosticForm
): Promise<DiagnosticRunVO> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/diagnostics`,
    method: 'post',
    data: {
      name: form.name,
      market: form.market,
      locale: form.locale,
      region: form.region,
      probeModes: form.probeModes,
      models: form.models?.length ? form.models : ['Gemini'],
      sampleCount: form.sampleCount ?? 1,
      calibrationRatio: form.calibrationRatio ?? 0,
      questionScope: form.questionScope ?? 'all'
    }
  });
  const runId = res.data;
  return getDiagnosticRun(runId);
}

export async function getDiagnosticRun(runId: number): Promise<DiagnosticRunVO> {
  const res = await request({
    url: `${BASE}/diagnostics/${runId}`,
    method: 'get'
  });
  return mapRunVo(res.data);
}

export async function getDiagnosticResults(runId: number): Promise<DiagnosticResultVO[]> {
  const res = await request.get(`${BASE}/diagnostics/${runId}/results`);
  const list = res.data ?? [];
  return list.map(mapResultVo);
}

export async function getProbeTasks(runId: number): Promise<ProbeTaskVO[]> {
  const res = await request.get(`${BASE}/diagnostics/${runId}/probe-tasks`);
  const list = res.data ?? [];
  return list.map(mapProbeTaskVo);
}

/** 工作台：聚合诊断列表 → KPI（最新 geo_score）+ 最近 5 条 run（FR-006 MVP） */
export async function getDashboard(projectId: number): Promise<DashboardData> {
  const { rows } = await listDiagnosticRuns(projectId, { pageNum: 1, pageSize: 5 });
  const recentRuns = rows.slice(0, 5);

  const scoredRuns = rows
    .filter((r) => r.geoScore != null)
    .sort((a, b) => runTimestamp(b) - runTimestamp(a));
  const latestScored = scoredRuns[0];

  return {
    kpi: {
      geoScore: latestScored?.geoScore ?? null,
      geoScoreDate: latestScored?.finishedAt ?? latestScored?.createdAt ?? null,
      contentCount: 0,
      contentTrend: 0,
      landingPageCount: 0,
      landingTrend: 0,
      leadCount: 0,
      leadTrend: 0
    },
    tasks: [],
    alerts: [],
    suggestion: {
      summary:
        recentRuns.length > 0
          ? '基于最近 GEO 诊断结果，建议优先优化低分问题并补充英文落地页与社媒内容。'
          : '暂无诊断数据。发起首次 GEO 诊断以获取可见率分数与优化建议。'
    },
    recentRuns
  };
}

function runTimestamp(run: DiagnosticRunVO): number {
  const raw = run.finishedAt ?? run.createdAt;
  const ms = Date.parse(raw);
  return Number.isNaN(ms) ? 0 : ms;
}

/** 从结果行聚合 MVP 分项指标（后端未返回 scoreJson 时前端估算） */
export function computeMetricsFromResults(results: DiagnosticResultVO[]): DiagnosticMetrics | null {
  if (!results.length) return null;
  const n = results.length;
  const brandHits = results.filter((r) => r.mentionedBrands.length > 0).length;
  const top3Hits = results.filter((r) => r.rank != null && r.rank <= 3).length;
  const citationHits = results.filter((r) => r.citations.length > 0).length;
  const competitorDominance =
    results.filter((r) => r.competitors.length > 0 && r.mentionedBrands.length === 0).length / n;
  return {
    brandMentionRate: brandHits / n,
    top3Rate: top3Hits / n,
    competitorSuppression: competitorDominance,
    citationCoverage: citationHits / n,
    longtailCoverage: 0,
    assetCompleteness: 1
  };
}

function mapRunVo(raw: Record<string, unknown>): DiagnosticRunVO {
  return {
    id: Number(raw.id),
    runId: String(raw.id),
    projectId: Number(raw.projectId),
    name: String(raw.name ?? ''),
    market: String(raw.market ?? ''),
    locale: String(raw.locale ?? ''),
    region: String(raw.region ?? ''),
    probeModes: (raw.probeModes as string[]) ?? [],
    models: (raw.models as string[]) ?? [],
    sampleCount: Number(raw.sampleCount ?? 1),
    status: raw.status as DiagnosticRunVO['status'],
    progress: raw.progress != null ? Number(raw.progress) : null,
    geoScore: raw.geoScore != null ? Number(raw.geoScore) : null,
    startedAt: raw.startedAt ? String(raw.startedAt) : null,
    createdAt: String(raw.createdAt ?? ''),
    finishedAt: raw.finishedAt ? String(raw.finishedAt) : null
  };
}

function mapResultVo(raw: Record<string, unknown>): DiagnosticResultVO {
  return {
    id: Number(raw.id),
    runId: Number(raw.runId),
    questionId: Number(raw.questionId),
    platform: String(raw.platform ?? ''),
    probeMode: String(raw.probeMode ?? ''),
    model: raw.model ? String(raw.model) : undefined,
    answerText: raw.answerText ? String(raw.answerText) : undefined,
    mentionedBrands: (raw.mentionedBrands as string[]) ?? [],
    competitors: (raw.competitors as string[]) ?? [],
    links: (raw.links as string[]) ?? [],
    citations: parseCitations(raw.citationsJson),
    rank: raw.rank != null ? Number(raw.rank) : null,
    captureMethod: raw.captureMethod ? String(raw.captureMethod) : undefined,
    humanCorrected: Boolean(raw.humanCorrected),
    sampledAt: raw.sampledAt ? String(raw.sampledAt) : null
  };
}

function mapProbeTaskVo(raw: Record<string, unknown>): ProbeTaskVO {
  return {
    id: Number(raw.id),
    runId: Number(raw.runId),
    questionId: Number(raw.questionId),
    platform: String(raw.platform ?? ''),
    probeMode: String(raw.probeMode ?? ''),
    status: String(raw.status ?? ''),
    retryCount: Number(raw.retryCount ?? 0),
    errorMessage: raw.errorMessage ? String(raw.errorMessage) : undefined,
    dispatchedAt: raw.dispatchedAt ? String(raw.dispatchedAt) : null,
    finishedAt: raw.finishedAt ? String(raw.finishedAt) : null
  };
}

function parseCitations(raw: unknown): DiagnosticCitationVO[] {
  if (!raw) return [];
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
    if (!Array.isArray(parsed)) return [];
    return parsed.map((c: Record<string, unknown>, idx: number) => ({
      url: String(c.url ?? ''),
      title: c.title ? String(c.title) : undefined,
      domain: c.domain ? String(c.domain) : undefined,
      rank: c.rank != null ? Number(c.rank) : idx + 1,
      isCustomer: Boolean(c.is_customer ?? c.isCustomer),
      isCompetitor: Boolean(c.is_competitor ?? c.isCompetitor)
    }));
  } catch {
    return [];
  }
}
