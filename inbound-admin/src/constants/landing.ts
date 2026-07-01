/** EPIC-6 M1 — landing_page UI 映射 (FR-501~505) */

export const LANDING_TEMPLATE_TYPES = [
  { value: 'destination', label: '目的地页' },
  { value: 'route', label: '路线页' },
  { value: 'theme', label: '主题游' },
  { value: 'visa', label: '签证政策' },
  { value: 'event', label: '活动页' }
] as const;

export type LandingPageStatus = 'DRAFT' | 'EDITING' | 'READY' | 'PUBLISHED' | 'ARCHIVED';

export const LANDING_PAGE_STATUS_META: Record<
  LandingPageStatus,
  { label: string; type: 'info' | 'primary' | 'success' | 'danger' | 'warning' }
> = {
  DRAFT: { label: '草稿', type: 'info' },
  EDITING: { label: '生成/编辑中', type: 'primary' },
  READY: { label: '待发布', type: 'success' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ARCHIVED: { label: '已归档', type: 'info' }
};

/** PRD §20.3 模块 key（与 inbound-ai LANDING_MODULE_KEYS 对齐） */
export const LANDING_MODULE_KEYS = [
  'hero',
  'why_this_trip',
  'itinerary',
  'what_we_provide',
  'traveler_reviews',
  'faq',
  'lead_form',
  'whatsapp_cta'
] as const;

export const LANDING_MODULE_LABELS: Record<string, string> = {
  hero: 'Hero',
  why_this_trip: 'Why This Trip',
  itinerary: 'Itinerary',
  what_we_provide: 'What We Provide',
  traveler_reviews: 'Traveler Reviews',
  reviews: 'Traveler Reviews',
  faq: 'FAQ',
  lead_form: 'Lead Form',
  whatsapp_cta: 'WhatsApp CTA'
};

export const LANDING_FORM_FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  email: '邮箱',
  phone: '电话',
  travel_date: '出发日期',
  party_size: '人数',
  pax: '人数',
  budget: '预算',
  message: '备注',
  notes: '备注'
};

export const NEEDS_REVIEW_TOOLTIP =
  'AI 生成落地页默认需人工确认价格/签证/政策类信息（PRD 合规）';

export const PUBLISH_DISABLED_TOOLTIP = '请先完成 AI 生成并确认内容';

/** Astro 公网预览根 URL（与 Java inbound.landing.public-base-url 对齐） */
export const LANDING_PUBLIC_BASE_URL = (
  import.meta.env.VITE_LANDING_PUBLIC_BASE_URL ?? 'http://localhost:4321'
).replace(/\/+$/, '');

export function buildLandingPublicUrl(projectId: number, slug: string): string {
  return `${LANDING_PUBLIC_BASE_URL}/p/${projectId}/${slug}`;
}

export function resolvePublishedUrl(row: {
  status?: string;
  publishedUrl?: string;
  projectId?: number;
  slug?: string;
}): string {
  if (row.status !== 'PUBLISHED' || !row.slug) return '';
  if (row.publishedUrl) return row.publishedUrl;
  if (row.projectId) return buildLandingPublicUrl(row.projectId, row.slug);
  return '';
}

export function templateTypeLabel(value?: string): string {
  if (!value) return '—';
  return LANDING_TEMPLATE_TYPES.find((t) => t.value === value)?.label ?? value;
}

export function landingStatusMeta(status: string) {
  return (
    LANDING_PAGE_STATUS_META[status as LandingPageStatus] ?? {
      label: status,
      type: 'info' as const
    }
  );
}

export function slugifyTitle(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 200);
}

export function isValidSlug(slug: string): boolean {
  if (!slug || slug.length < 2 || slug.length > 200) return false;
  return /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug);
}
