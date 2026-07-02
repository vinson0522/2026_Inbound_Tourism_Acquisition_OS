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
  /** 0–1 小数，如 0.1 = 10% 校准抽样 */
  calibrationRatio: number;
  status: DiagnosticRunStatus;
  progress: number | null;
  geoScore: number | null;
  startedAt: string | null;
  createdAt: string;
  finishedAt: string | null;
}

/** FR-115 校准对比单侧摘要 */
export interface DiagnosticCalibrationSideVO {
  resultId?: number;
  probeNodeKey?: string;
  answerPreview?: string;
  brandMentioned?: boolean;
  rank?: number | null;
  citationCount?: number;
}

/** FR-115 校准 question × platform 配对 */
export interface DiagnosticCalibrationPairVO {
  questionId: number;
  question?: string;
  stage?: string;
  platform: string;
  brandMatch?: boolean;
  similarityScore?: number;
  deviationScore?: number;
  groundedApi?: DiagnosticCalibrationSideVO;
  browserExtension?: DiagnosticCalibrationSideVO;
}

export interface DiagnosticCalibrationVO {
  deviationRate?: number | null;
  brandMentionAgreementRate?: number | null;
  sampleCount?: number;
  pairedCount?: number;
  pairs: DiagnosticCalibrationPairVO[];
}

/** FR-116 平台 Adapter 配置 */
export interface PlatformAdapterVo {
  id: number;
  platform: string;
  version: string;
  enabled: boolean;
  domSelectorsJson?: Record<string, unknown>;
  apiPatternsJson?: Record<string, unknown>;
  parseRulesJson?: Record<string, unknown>;
  updatedAt?: string;
}

export interface PlatformAdapterSaveForm {
  version?: string;
  enabled?: boolean;
  domSelectorsJson: Record<string, unknown>;
  apiPatternsJson: Record<string, unknown>;
  parseRulesJson: Record<string, unknown>;
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
  orderByColumn?: string;
  isAsc?: string;
}

export interface KeywordScoreResult {
  keywordId: number;
  keyword?: string;
  score?: number | null;
  scoreDetailJson?: Record<string, unknown>;
}

export interface KeywordScoreBatchForm {
  keywordIds?: number[];
  useRag?: boolean;
}

export interface KeywordScoreBatchResult {
  scoredCount: number;
  results?: KeywordScoreResult[];
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

/** FR-501~505 落地页 */
export type LandingPageStatus = 'DRAFT' | 'EDITING' | 'READY' | 'PUBLISHED' | 'ARCHIVED';

export interface LandingPageVo {
  id: number;
  projectId: number;
  keywordId?: number;
  keywordText?: string;
  templateType: string;
  title: string;
  slug: string;
  status: LandingPageStatus;
  moduleCount?: number;
  publishedUrl?: string;
  publishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LandingPageDetailVo extends LandingPageVo {
  contentJson?: Record<string, unknown>;
  seoMetaJson?: Record<string, unknown>;
  formConfigJson?: Record<string, unknown>;
  whatsappLink?: string;
}

export interface LandingPageQuery {
  pageNum: number;
  pageSize: number;
  status?: LandingPageStatus | '';
  templateType?: string;
  title?: string;
  slug?: string;
  keyword?: string;
}

export interface LandingPageForm {
  keywordId: number;
  templateType: string;
  title?: string;
  slug?: string;
  language?: string;
  targetMarket?: string;
}

export interface LandingGenerateForm {
  useRag?: boolean;
}

export interface LandingGenerateResult {
  pageId: number;
  needsHumanReview?: boolean;
  captureMethod?: string;
  moduleCount?: number;
}

export interface LandingPublishResult {
  pageId: number;
  status: LandingPageStatus;
  publishedUrl?: string;
  publishedAt?: string;
}

export interface LandingModuleItem {
  key?: string;
  type?: string;
  content?: Record<string, unknown>;
  [key: string]: unknown;
}

/** FR-601 询盘线索 */
export type LeadStatus = 'NEW' | 'FOLLOWING' | 'QUOTED' | 'WON' | 'LOST';

export interface LeadVo {
  id: number;
  projectId: number;
  landingPageId?: number;
  keywordId?: number;
  name?: string;
  email?: string;
  phone?: string;
  source?: string;
  status: LeadStatus;
  assigneeId?: number | null;
  assigneeName?: string | null;
  landingPageTitle?: string;
  landingPageSlug?: string;
  keywordText?: string;
  keywordMarket?: string;
  createdAt?: string;
}

export interface LeadDetailVo extends LeadVo {
  travelDate?: string;
  partySize?: number;
  budget?: string;
  message?: string;
  utm?: Record<string, string>;
  device?: string;
  followups?: LeadFollowupVo[];
}

export interface LeadFollowupVo {
  id: number;
  leadId?: number;
  content: string;
  channel?: string | null;
  suggestion?: string | null;
  operatorId?: number | null;
  operatorName?: string | null;
  createdAt?: string;
}

export interface LeadUpdateForm {
  status?: LeadStatus;
  assigneeId?: number | null;
}

export interface LeadFollowupCreateForm {
  content: string;
  channel?: string;
}

export interface LeadQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  email?: string;
  phone?: string;
  source?: string;
  status?: LeadStatus | '';
}

/** FR-701/702 报告中心 */
export type ReportType = 'DIAGNOSTIC' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';

export interface ReportSummary {
  runId?: number;
  geoScore?: number;
  periodStart?: string;
  periodEnd?: string;
  periodPrevStart?: string;
  periodPrevEnd?: string;
  geo?: {
    runs?: number;
    latestScore?: number;
    delta?: number;
    momDelta?: number;
    prevScore?: number;
  };
  keywords?: { newCount?: number; avgScore?: number; byStage?: Record<string, number> };
  content?: { tasksCreated?: number; generated?: number };
  landing?: { draftCount?: number; publishedCount?: number };
  leads?: { newCount?: number; wonCount?: number; byStatus?: Record<string, number> };
  recommendations?: string[];
  templateSnapshot?: { companyName?: string; primaryColor?: string; coverTitle?: string };
  probe_mode?: string;
  sampled_at?: string;
  region?: string;
  platforms?: string;
}

export interface ReportVo {
  id: number;
  projectId: number;
  type: ReportType;
  period?: string;
  summary?: string | ReportSummary;
  summaryPreview?: string;
  createdAt?: string;
}

export interface ReportDetailVo extends ReportVo {
  summary?: string | ReportSummary;
}

export interface ReportQuery {
  pageNum: number;
  pageSize: number;
  type?: ReportType | '';
  period?: string;
}

export interface WeeklyReportForm {
  periodStart: string;
  periodEnd: string;
}

export interface MonthlyReportForm {
  year: number;
  month: number;
}

export type ReportSectionKey =
  | 'geo'
  | 'keywords'
  | 'content'
  | 'landing'
  | 'leads'
  | 'recommendations';

export interface ReportTemplateVo {
  templateId?: number | null;
  logoUrl?: string;
  coverTitle?: string;
  companyName?: string;
  primaryColor?: string;
  footerText?: string;
  sections?: ReportSectionKey[];
  configJson?: Record<string, unknown>;
}

export interface ReportTemplateSaveForm {
  logoUrl?: string;
  coverTitle: string;
  companyName: string;
  primaryColor?: string;
  footerText?: string;
  sections: ReportSectionKey[];
}

/** EPIC-9 M1 — 套餐与额度 (FR-804) */
export type QuotaItemStatus = 'normal' | 'warning' | 'overage';

export interface QuotaItemVo {
  key: string;
  label: string;
  used: number;
  limit: number;
  unit: string;
  period: 'total' | 'monthly' | string;
  percentage?: number;
  status?: QuotaItemStatus;
}

export interface SubscriptionVo {
  planCode: string;
  planName: string;
  status: string;
  periodStart?: string;
  periodEnd?: string;
  daysRemaining?: number;
  quotas: QuotaItemVo[];
  hasOverage?: boolean;
  hasWarning?: boolean;
  overageKeys?: string[];
}

/** EPIC-11 M1 — 探针节点 (FR-113) */
export interface ProbeNodeVo {
  id: number;
  nodeKey: string;
  region?: string;
  platforms: string[];
  extensionVersion?: string;
  status: string;
  lastHeartbeatAt?: string;
  online: boolean;
}
