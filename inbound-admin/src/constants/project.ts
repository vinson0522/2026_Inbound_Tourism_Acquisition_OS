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

export const DESTINATION_OPTIONS = [
  'Beijing',
  'Shanghai',
  "Xi'an",
  'Chengdu',
  'Guilin',
  'Hangzhou',
  'Suzhou',
  'Lhasa',
  'Yunnan',
  'Chongqing',
  'Hong Kong',
  'Shenzhen'
] as const;

export const PROJECT_STATUS_OPTIONS = ENTITY_STATUS_OPTIONS.filter((o) => o.value !== '');

export type KnowledgeAssetType = 'DOCUMENT' | 'FAQ' | 'ROUTE' | 'POLICY' | 'WEB_PAGE' | 'OTHER';

export type VectorIndexStatus = 'PENDING' | 'INDEXING' | 'READY' | 'FAILED';

export const KNOWLEDGE_ASSET_TYPE_OPTIONS: { label: string; value: KnowledgeAssetType }[] = [
  { label: '文档', value: 'DOCUMENT' },
  { label: 'FAQ', value: 'FAQ' },
  { label: '路线说明', value: 'ROUTE' },
  { label: '政策/签证', value: 'POLICY' },
  { label: '网页', value: 'WEB_PAGE' },
  { label: '其他', value: 'OTHER' }
];

export const KNOWLEDGE_ASSET_TYPE_META: Record<string, string> = {
  DOCUMENT: '文档',
  FAQ: 'FAQ',
  ROUTE: '路线说明',
  POLICY: '政策/签证',
  WEB_PAGE: '网页',
  OTHER: '其他'
};

export const VECTOR_STATUS_OPTIONS: { label: string; value: VectorIndexStatus | '' }[] = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '向量化中', value: 'INDEXING' },
  { label: '可检索', value: 'READY' },
  { label: '失败', value: 'FAILED' }
];

export const VECTOR_STATUS_META: Record<string, { label: string; type: 'info' | 'primary' | 'success' | 'danger' }> = {
  PENDING: { label: '待处理', type: 'info' },
  INDEXING: { label: '向量化中', type: 'primary' },
  READY: { label: '可检索', type: 'success' },
  FAILED: { label: '失败', type: 'danger' }
};

export const SOCIAL_PLATFORMS = [
  { key: 'instagram', label: 'Instagram' },
  { key: 'facebook', label: 'Facebook' },
  { key: 'youtube', label: 'YouTube' },
  { key: 'tiktok', label: 'TikTok' }
] as const;
