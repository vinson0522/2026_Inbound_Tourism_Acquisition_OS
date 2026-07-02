/** EPIC-9 M1 — subscription & quota UI (FR-804) */

export type SubscriptionStatus = 'ACTIVE' | 'TRIAL' | 'EXPIRED' | 'CANCELLED';

export type QuotaItemStatus = 'normal' | 'warning' | 'overage';

export const QUOTA_EXCEEDED_CODE = 40201;

export const PLAN_LABELS: Record<string, string> = {
  diagnostic_report: '诊断报告版',
  basic_saas: '基础 SaaS 版',
  growth_service: '增长服务版',
  oem_private: 'OEM/私有化版',
  trial: '试用版'
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
