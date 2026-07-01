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

/** FR-108 趋势 API 单点 */
export interface DiagnosticTrendPointVO {
  runId: number;
  name: string;
  geoScore: number;
  finishedAt: string;
  market: string;
  status: string;
  metrics: DiagnosticMetrics;
}

export interface DiagnosticTrendsData {
  runs: DiagnosticTrendPointVO[];
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

export interface TravelProductVo {
  id: number;
  projectId: number;
  name: string;
  destinations: string[];
  days?: number | null;
  priceRange?: string;
  suitableFor?: string;
  highlights?: string;
  inclusions?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TravelProductForm {
  name: string;
  destinations: string[];
  days?: number | null;
  priceRange?: string;
  suitableFor?: string;
  highlights?: string;
  inclusions?: string;
}

export interface CompetitorVo {
  id: number;
  projectId: number;
  name: string;
  website?: string;
  socialLinks?: Record<string, string>;
  mainProducts?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CompetitorForm {
  name: string;
  website?: string;
  socialLinks?: Record<string, string>;
  mainProducts?: string;
  notes?: string;
}

export type KnowledgeAssetType = 'DOCUMENT' | 'FAQ' | 'ROUTE' | 'POLICY' | 'WEB_PAGE' | 'OTHER';

export type VectorIndexStatus = 'PENDING' | 'INDEXING' | 'READY' | 'FAILED';

export interface KnowledgeAssetVo {
  id: number;
  projectId: number;
  type: KnowledgeAssetType;
  title: string;
  content?: string;
  fileUrl?: string;
  tags: string[];
  vectorStatus: VectorIndexStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface KnowledgeAssetForm {
  title: string;
  type?: KnowledgeAssetType;
  content?: string;
  fileUrl?: string;
  tags?: string[];
}

export interface KnowledgeAssetQuery {
  pageNum: number;
  pageSize: number;
  title?: string;
  type?: KnowledgeAssetType | '';
  vectorStatus?: VectorIndexStatus | '';
}

/** FR-005 RAG 检索预览 */
export interface KnowledgeRagHit {
  chunkId: number;
  assetId: number;
  chunkIndex: number;
  chunkText: string;
  score: number;
}

export interface KnowledgeRagSearchResult {
  hits: KnowledgeRagHit[];
}

/** FR-201/202 关键词机会词 */
export interface KeywordOpportunityVo {
  id: number;
  projectId: number;
  keyword: string;
  keywordEn?: string;
  keywordCn?: string;
  intent?: string;
  market: string;
  stage: string;
  score?: number | null;
  scoreDetailJson?: Record<string, unknown>;
  channel?: string;
  sourceJson?: Record<string, unknown>;
  status: EntityStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface KeywordOpportunityQuery {
  pageNum: number;
  pageSize: number;
  stage?: string;
  market?: string;
  keyword?: string;
  status?: EntityStatus | '';
}

export interface KeywordGenerateForm {
  market: string;
  locale?: string;
  stages?: string[];
  wordsPerStage?: number;
  useRag?: boolean;
}

export interface KeywordGenerateResult {
  insertedCount: number;
  needsHumanReview?: boolean;
  captureMethod?: string;
}

/** FR-301/302 内容任务 */
export type ContentTaskStatus =
  | 'DRAFT'
  | 'GENERATING'
  | 'GENERATED'
  | 'ADOPTED'
  | 'DISCARDED'
  | 'FAILED';

export interface StoryboardSceneVo {
  scene?: number;
  duration?: number;
  visual?: string;
  note?: string;
}

export interface GeneratedContentVo {
  id: number;
  taskId: number;
  title?: string;
  hook?: string;
  targetAudience?: string;
  script?: string;
  storyboardJson?: StoryboardSceneVo[];
  voiceover?: string;
  onScreenText?: string;
  hashtags?: string;
  cta?: string;
  landingPageSuggestion?: string;
  needsHumanReview?: boolean;
  version?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ContentTaskVo {
  id: number;
  projectId: number;
  keywordId?: number;
  keywordText?: string;
  platform: string;
  format?: string;
  duration?: number;
  tone?: string;
  language?: string;
  targetMarket?: string;
  status: ContentTaskStatus;
  needsHumanReview?: boolean | null;
  contentTitle?: string;
  contentVersion?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ContentTaskDetailVo extends ContentTaskVo {
  generatedContent?: GeneratedContentVo;
}

export interface ContentTaskQuery {
  pageNum: number;
  pageSize: number;
  status?: ContentTaskStatus | '';
  platform?: string;
  format?: string;
  keyword?: string;
  needsHumanReview?: boolean | '';
}

export interface ContentTaskForm {
  keywordId: number;
  platform: string;
  format?: string;
  duration?: number;
  tone?: string;
  language?: string;
  targetMarket?: string;
}

export interface ContentGenerateForm {
  useRag?: boolean;
}

export interface ContentGenerateResult {
  contentId: number;
  version?: number;
  needsHumanReview?: boolean;
  captureMethod?: string;
}
