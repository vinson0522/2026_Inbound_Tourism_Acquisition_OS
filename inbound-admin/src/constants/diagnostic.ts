/** diagnostic_run.status — 与 database/ddl 及 docs/design/tokens.md 一致 */
export type DiagnosticRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'PARTIAL_FAILED'
  | 'FAILED'
  | 'CANCELLED';

export const DIAGNOSTIC_STATUS_OPTIONS: { label: string; value: DiagnosticRunStatus | '' }[] = [
  { label: '全部', value: '' },
  { label: '待执行', value: 'PENDING' },
  { label: '执行中', value: 'RUNNING' },
  { label: '已完成', value: 'SUCCESS' },
  { label: '部分失败', value: 'PARTIAL_FAILED' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' }
];

export const DIAGNOSTIC_STATUS_META: Record<
  DiagnosticRunStatus,
  { label: string; type: 'info' | 'primary' | 'success' | 'warning' | 'danger' }
> = {
  PENDING: { label: '待执行', type: 'info' },
  RUNNING: { label: '执行中', type: 'primary' },
  SUCCESS: { label: '已完成', type: 'success' },
  PARTIAL_FAILED: { label: '部分失败', type: 'warning' },
  FAILED: { label: '失败', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
};

export const PROBE_MODE_OPTIONS = [
  { label: '全部', value: '' },
  { label: 'Grounded API', value: 'grounded-api' },
  { label: '浏览器扩展', value: 'browser-extension' },
  { label: '无头自动化', value: 'headless-automation' }
];

export const AI_PLATFORM_OPTIONS = ['Perplexity', 'Gemini', 'OpenAI'] as const;

/** PRD 用户生命周期八阶段 */
export const LIFECYCLE_STAGE_LABELS: Record<string, string> = {
  inspiration: '灵感',
  planting: '种草',
  comparison: '比较',
  visa: '签证',
  planning: '规划',
  trust: '信任',
  decision: '决策',
  repurchase: '复购'
};

export const PROBE_TASK_STATUS_META: Record<string, { label: string; type: 'info' | 'primary' | 'success' | 'warning' | 'danger' }> = {
  PENDING: { label: '待执行', type: 'info' },
  DISPATCHED: { label: '已派发', type: 'primary' },
  RUNNING: { label: '执行中', type: 'primary' },
  SUCCESS: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
  RETRY: { label: '重试中', type: 'warning' }
};

/** GEO 分项指标权重（PRD §10，展示用小字） */
export const METRIC_WEIGHT_LABELS: Record<string, string> = {
  brandMentionRate: '25%',
  top3Rate: '20%',
  competitorSuppression: '15%',
  citationCoverage: '15%',
  longtailCoverage: '15%',
  assetCompleteness: '10%'
};

/** FR-115 校准偏差率阈值（ADR-20260709-22） */
export function calibrationDeviationMeta(rate: number | null | undefined): {
  label: string;
  type: 'success' | 'warning' | 'danger' | 'info';
} {
  if (rate == null || Number.isNaN(rate)) {
    return { label: '—', type: 'info' };
  }
  if (rate <= 0.15) {
    return { label: '低偏差', type: 'success' };
  }
  if (rate <= 0.3) {
    return { label: '中偏差', type: 'warning' };
  }
  return { label: '高偏差', type: 'danger' };
}

export function formatCalibrationPct(ratio: number | null | undefined): string {
  if (ratio == null || Number.isNaN(ratio)) return '0%';
  return `${(ratio * 100).toFixed(0)}%`;
}
