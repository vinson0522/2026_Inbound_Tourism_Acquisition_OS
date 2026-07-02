/** EPIC-7 M1 — lead 状态与来源 UI 映射 (FR-601) */

export type LeadStatus = 'NEW' | 'FOLLOWING' | 'QUOTED' | 'WON' | 'LOST';

export type LeadSource = 'form' | 'whatsapp' | 'manual';

export const LEAD_STATUS_META: Record<
  LeadStatus,
  { label: string; type: 'danger' | 'primary' | 'warning' | 'success' | 'info' }
> = {
  NEW: { label: '新线索', type: 'danger' },
  FOLLOWING: { label: '跟进中', type: 'primary' },
  QUOTED: { label: '已报价', type: 'warning' },
  WON: { label: '已成交', type: 'success' },
  LOST: { label: '已流失', type: 'info' }
};

export const LEAD_SOURCE_META: Record<
  LeadSource,
  { label: string; type: 'primary' | 'success' | 'info' }
> = {
  form: { label: '表单', type: 'primary' },
  whatsapp: { label: 'WhatsApp', type: 'success' },
  manual: { label: '手工录入', type: 'info' }
};

export const LEAD_STATUS_OPTIONS = Object.entries(LEAD_STATUS_META).map(([value, meta]) => ({
  value,
  label: meta.label
}));

export const LEAD_SOURCE_OPTIONS = Object.entries(LEAD_SOURCE_META).map(([value, meta]) => ({
  value,
  label: meta.label
}));

export function leadStatusMeta(status: string) {
  return (
    LEAD_STATUS_META[status as LeadStatus] ?? {
      label: status,
      type: 'info' as const
    }
  );
}

export function leadSourceMeta(source: string) {
  return (
    LEAD_SOURCE_META[source as LeadSource] ?? {
      label: source,
      type: 'info' as const
    }
  );
}

const TERMINAL_STATUSES: LeadStatus[] = ['WON', 'LOST'];

export function isTerminalLeadStatus(status: string): boolean {
  return TERMINAL_STATUSES.includes(status as LeadStatus);
}

/** M2 状态机 — 与 Java LeadStatusTransition / ADR-20 一致 */
export function getAllowedNextStatuses(current: LeadStatus): LeadStatus[] {
  if (isTerminalLeadStatus(current)) {
    return [current];
  }
  const options = new Set<LeadStatus>([current]);
  switch (current) {
    case 'NEW':
      options.add('FOLLOWING');
      options.add('LOST');
      break;
    case 'FOLLOWING':
      options.add('QUOTED');
      options.add('LOST');
      break;
    case 'QUOTED':
      options.add('WON');
      options.add('LOST');
      break;
    default:
      break;
  }
  return Array.from(options);
}

export const LEAD_FOLLOWUP_CHANNEL_OPTIONS = [
  { value: '', label: '未指定' },
  { value: 'email', label: '邮件' },
  { value: 'phone', label: '电话' },
  { value: 'whatsapp', label: 'WhatsApp' },
  { value: 'meeting', label: '会议/视频' }
] as const;

export function leadFollowupChannelLabel(channel?: string | null): string {
  if (!channel) return '';
  return LEAD_FOLLOWUP_CHANNEL_OPTIONS.find((opt) => opt.value === channel)?.label ?? channel;
}
