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
