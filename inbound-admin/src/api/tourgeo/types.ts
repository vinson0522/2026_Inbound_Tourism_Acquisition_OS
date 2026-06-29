import type { DiagnosticRunStatus } from '@/constants/diagnostic';

export type EntityStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ARCHIVED';

export interface CustomerProjectVo {
  id: number;
  name: string;
  brandName: string;
  website?: string;
  industry?: string;
  targetMarkets: string[];
  languages: string[];
  status: EntityStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface CustomerProjectForm {
  name: string;
  brandName: string;
  website?: string;
  industry?: string;
  targetMarkets: string[];
  languages: string[];
  status?: EntityStatus;
}

export interface CustomerProjectQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  brandName?: string;
  status?: EntityStatus | '';
  market?: string;
  params?: {
    beginCreateTime?: string;
    endCreateTime?: string;
  };
}

/** @deprecated 兼容旧 mock 引用 */
export interface CustomerProject {
  id: number;
  name: string;
  brandName: string;
  targetMarkets: string[];
  defaultLocale: string;
}

export interface DashboardKpi {
  geoScore: number | null;
  geoScoreDate: string | null;
  contentCount: number;
  contentTrend: number;
  landingPageCount: number;
  landingTrend: number;
  leadCount: number;
  leadTrend: number;
}

export interface DashboardTask {
  id: string;
  title: string;
  type: 'content' | 'landing' | 'lead';
}

export interface DashboardAlert {
  id: string;
  message: string;
  level: 'warning' | 'danger';
}

export interface DashboardSuggestion {
  summary: string;
}

/** 工作台聚合数据（前端 listDiagnosticRuns 组装，FR-006 MVP） */
export interface DashboardData {
  kpi: DashboardKpi;
  tasks: DashboardTask[];
  alerts: DashboardAlert[];
  suggestion: DashboardSuggestion;
  recentRuns: DiagnosticRunVO[];
}

export interface DiagnosticRunVO {
  id: number;
  runId: string;
  projectId: number;
  name: string;
  market: string;
  locale: string;
  region: string;
  probeModes: string[];
  models: string[];
  sampleCount: number;
  status: DiagnosticRunStatus;
  progress: number | null;
  geoScore: number | null;
  startedAt: string | null;
  createdAt: string;
  finishedAt: string | null;
}

export interface DiagnosticCitationVO {
  url: string;
  title?: string;
  domain?: string;
  rank?: number;
  isCustomer?: boolean;
  isCompetitor?: boolean;
}

export interface DiagnosticResultVO {
  id: number;
  runId: number;
  questionId: number;
  platform: string;
  probeMode: string;
  model?: string;
  answerText?: string;
  mentionedBrands: string[];
  competitors: string[];
  links: string[];
  citations: DiagnosticCitationVO[];
  rank: number | null;
  captureMethod?: string;
  humanCorrected: boolean;
  sampledAt: string | null;
}

export interface ProbeTaskVO {
  id: number;
  runId: number;
  questionId: number;
  platform: string;
  probeMode: string;
  status: string;
  retryCount: number;
  errorMessage?: string;
  dispatchedAt: string | null;
  finishedAt: string | null;
}

export interface DiagnosticMetrics {
  brandMentionRate: number;
  top3Rate: number;
  competitorSuppression: number;
  citationCoverage: number;
  longtailCoverage: number;
  assetCompleteness: number;
}

export interface DiagnosticRunQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  status?: DiagnosticRunStatus | '';
  market?: string;
  probeMode?: string;
  createdAt?: string[];
}

export interface CreateDiagnosticForm {
  name: string;
  market: string;
  locale: string;
  region: string;
  questionScope: 'all' | 'stage' | 'custom';
  probeModes: string[];
  models: string[];
  sampleCount: number;
  calibrationRatio: number;
}

export interface PageResult<T> {
  rows: T[];
  total: number;
}
