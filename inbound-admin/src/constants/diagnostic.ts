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
