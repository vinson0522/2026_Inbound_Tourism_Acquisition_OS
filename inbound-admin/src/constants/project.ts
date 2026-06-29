export const ENTITY_STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
  { label: '暂停', value: 'SUSPENDED' },
  { label: '已归档', value: 'ARCHIVED' }
] as const;

export const ENTITY_STATUS_META: Record<string, { label: string; type: 'success' | 'info' | 'warning' }> = {
  ACTIVE: { label: '正常', type: 'success' },
  INACTIVE: { label: '停用', type: 'info' },
  SUSPENDED: { label: '暂停', type: 'warning' },
  ARCHIVED: { label: '已归档', type: 'info' }
};

export const MARKET_OPTIONS = ['US', 'UK', 'AU', 'DE', 'FR', 'JP', 'KR', 'SEA'] as const;

export const LANGUAGE_OPTIONS = ['en', 'zh', 'de', 'fr', 'ja', 'ko'] as const;

export const INDUSTRY_OPTIONS = [
  { label: '入境游', value: 'inbound_tourism' },
  { label: '文旅', value: 'culture_tourism' },
  { label: '酒店', value: 'hotel' },
  { label: '其他', value: 'other' }
] as const;
