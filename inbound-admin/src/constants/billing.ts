/** EPIC-9 M1/M2 — subscription & quota UI (FR-804) */

export type SubscriptionStatus = 'ACTIVE' | 'TRIAL' | 'EXPIRED' | 'CANCELLED';

export type QuotaItemStatus = 'normal' | 'warning' | 'overage';

export const QUOTA_EXCEEDED_CODE = 40201;

export const PLAN_LABELS: Record<string, string> = {
  diagnostic_report: '诊断报告版',
  basic_saas: '基础 SaaS 版',
  growth_service: '增长服务版',
  oem_private: 'OEM/私有化版',
  trial: '试用版',
  starter: '入门版',
  enterprise: '企业版'
};

export const PLAN_OPTIONS = Object.entries(PLAN_LABELS).map(([value, label]) => ({ value, label }));

/** quota_json keys — aligned with Java QuotaType */
export const QUOTA_FORM_FIELDS: Array<{ key: string; label: string; monthly: boolean }> = [
  { key: 'projects', label: '客户项目数', monthly: false },
  { key: 'diagnostics_per_month', label: 'GEO 诊断（月）', monthly: true },
  { key: 'keywords_per_month', label: '关键词 AI 生成（月）', monthly: true },
  { key: 'content_per_month', label: '内容 AI 生成（月）', monthly: true },
  { key: 'landing_pages_per_month', label: '落地页 AI 生成（月）', monthly: true },
  { key: 'reports_per_month', label: '报告生成（月）', monthly: true }
];

export type QuotaJson = Record<(typeof QUOTA_FORM_FIELDS)[number]['key'], number>;

export const PLAN_QUOTA_PRESETS: Record<string, QuotaJson> = {
  diagnostic_report: {
    projects: 1,
    diagnostics_per_month: 1,
    keywords_per_month: 50,
    content_per_month: 0,
    landing_pages_per_month: 0,
    reports_per_month: 2
  },
  basic_saas: {
    projects: 3,
    diagnostics_per_month: 2,
    keywords_per_month: 200,
    content_per_month: 30,
    landing_pages_per_month: 10,
    reports_per_month: 4
  },
  growth_service: {
    projects: 5,
    diagnostics_per_month: 4,
    keywords_per_month: 500,
    content_per_month: 100,
    landing_pages_per_month: 20,
    reports_per_month: 8
  },
  trial: {
    projects: 1,
    diagnostics_per_month: 1,
    keywords_per_month: 20,
    content_per_month: 5,
    landing_pages_per_month: 2,
    reports_per_month: 1
  },
  oem_private: {
    projects: 50,
    diagnostics_per_month: 100,
    keywords_per_month: 5000,
    content_per_month: 500,
    landing_pages_per_month: 100,
    reports_per_month: 50
  },
  starter: {
    projects: 3,
    diagnostics_per_month: 2,
    keywords_per_month: 200,
    content_per_month: 30,
    landing_pages_per_month: 10,
    reports_per_month: 4
  },
  enterprise: {
    projects: 30,
    diagnostics_per_month: 50,
    keywords_per_month: 3000,
    content_per_month: 300,
    landing_pages_per_month: 60,
    reports_per_month: 30
  }
};

export const SUBSCRIPTION_STATUS_META: Record<
  SubscriptionStatus,
  { label: string; type: 'success' | 'primary' | 'danger' | 'info' }
> = {
  ACTIVE: { label: '生效中', type: 'success' },
  TRIAL: { label: '试用中', type: 'primary' },
  EXPIRED: { label: '已过期', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' }
};

export function planLabel(planCode?: string | null): string {
  if (!planCode) return '—';
  return PLAN_LABELS[planCode] ?? planCode;
}

export function subscriptionStatusMeta(status?: string | null) {
  if (!status) {
    return { label: '—', type: 'info' as const };
  }
  return (
    SUBSCRIPTION_STATUS_META[status as SubscriptionStatus] ?? {
      label: status,
      type: 'info' as const
    }
  );
}

export function quotaProgressStatus(status?: QuotaItemStatus): '' | 'success' | 'warning' | 'exception' {
  if (status === 'overage') return 'exception';
  if (status === 'warning') return 'warning';
  return 'success';
}

export function isInactiveSubscription(status?: string | null): boolean {
  return status === 'EXPIRED' || status === 'CANCELLED';
}

export function quotasToJson(quotas: Array<{ key: string; limit: number }>): QuotaJson {
  const json = {} as QuotaJson;
  for (const field of QUOTA_FORM_FIELDS) {
    const item = quotas.find((q) => q.key === field.key);
    json[field.key as keyof QuotaJson] = item?.limit ?? 0;
  }
  return json;
}

export function emptyQuotaJson(): QuotaJson {
  return PLAN_QUOTA_PRESETS.growth_service;
}
