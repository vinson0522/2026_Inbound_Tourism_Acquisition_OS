/** EPIC-8 M1 — report_type UI 映射 (FR-701/702) */

export type ReportType = 'DIAGNOSTIC' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';

export const REPORT_TYPE_META: Record<
  ReportType,
  { label: string; type: 'primary' | 'success' | 'warning' | 'info' }
> = {
  DIAGNOSTIC: { label: '诊断报告', type: 'primary' },
  WEEKLY: { label: '增长周报', type: 'success' },
  MONTHLY: { label: '月报', type: 'warning' },
  CUSTOM: { label: '自定义', type: 'info' }
};

export const REPORT_TYPE_OPTIONS = Object.entries(REPORT_TYPE_META).map(([value, meta]) => ({
  value,
  label: meta.label
}));

export const WEEKLY_REPORT_DISCLAIMER =
  '免责声明：数据来自系统统计区间，不构成 AI 推荐排名承诺。';

export function reportTypeMeta(type: string) {
  return (
    REPORT_TYPE_META[type as ReportType] ?? {
      label: type,
      type: 'info' as const
    }
  );
}

/** ISO week label YYYY-Www from a Date (UTC-based, Monday week). */
export function isoWeekLabel(date: Date): string {
  const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const day = d.getUTCDay() || 7;
  d.setUTCDate(d.getUTCDate() + 4 - day);
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const week = Math.ceil(((d.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(week).padStart(2, '0')}`;
}

export function formatYmd(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function defaultWeeklyRange(): [string, string] {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 6);
  return [formatYmd(start), formatYmd(end)];
}
