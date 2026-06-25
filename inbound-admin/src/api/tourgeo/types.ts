import type { DiagnosticRunStatus } from '@/constants/diagnostic';

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
  createdAt: string;
  finishedAt: string | null;
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
