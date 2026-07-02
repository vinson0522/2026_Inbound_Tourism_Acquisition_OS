/** FR-202 用户生命周期八阶段 — 与 PRD §10.4、diagnostic.ts 一致 */
export const LIFECYCLE_STAGE_KEYS = [
  'inspiration',
  'planting',
  'comparison',
  'visa',
  'planning',
  'trust',
  'decision',
  'repurchase'
] as const;

export type LifecycleStage = (typeof LIFECYCLE_STAGE_KEYS)[number];

export const LIFECYCLE_STAGE_LABELS: Record<LifecycleStage, string> = {
  inspiration: '灵感',
  planting: '种草',
  comparison: '比较',
  visa: '签证',
  planning: '规划',
  trust: '信任',
  decision: '决策',
  repurchase: '复购'
};

export const LIFECYCLE_STAGE_TOOLTIPS: Record<LifecycleStage, string> = {
  inspiration: 'China is becoming popular, where should I go?',
  planting: 'I saw Chongqing on TikTok, is it worth visiting?',
  comparison: 'Private tour or group tour in China?',
  visa: 'Can I visit China visa-free?',
  planning: '10-day China itinerary for first timers.',
  trust: 'Best China travel agency for foreigners.',
  decision: 'How much does a private China tour cost?',
  repurchase: 'Where to go after Beijing and Shanghai?'
};

export const KEYWORD_STAGE_TABS: { label: string; value: LifecycleStage | '' }[] = [
  { label: '全部', value: '' },
  ...LIFECYCLE_STAGE_KEYS.map((key) => ({
    label: LIFECYCLE_STAGE_LABELS[key],
    value: key
  }))
];

/** FR-203 五维分项 — ADR-19 keyword_score_v1 */
export const SCORE_DIMENSION_KEYS = [
  'relevance',
  'long_tail_value',
  'producibility',
  'landing_value',
  'competitive_pressure'
] as const;

export const SCORE_DIMENSION_LABELS: Record<(typeof SCORE_DIMENSION_KEYS)[number], string> = {
  relevance: '相关性',
  long_tail_value: '长尾价值',
  producibility: '可生产性',
  landing_value: '落地页价值',
  competitive_pressure: '竞品强度'
};

/** 机会分色阶 — docs/design/tokens.md §1.4 */
export function scoreColorClass(score: number | string | null | undefined): string {
  if (score == null || score === '' || Number.isNaN(Number(score))) return '';
  const n = Number(score);
  if (n >= 80) return 'score-high';
  if (n >= 50) return 'score-mid';
  return 'score-low';
}

export function hasKeywordScore(score: number | string | null | undefined): boolean {
  return score != null && score !== '' && !Number.isNaN(Number(score));
}

export function formatScoreDetailTooltip(detail?: Record<string, unknown> | null): string {
  if (!detail) return '暂无分项明细';
  const parts = SCORE_DIMENSION_KEYS.map((key) => {
    const raw = detail[key];
    if (raw == null || Number.isNaN(Number(raw))) return null;
    return `${SCORE_DIMENSION_LABELS[key]} ${Number(raw).toFixed(1)}`;
  }).filter(Boolean) as string[];
  if (parts.length === 0) return '暂无分项明细';
  const version = detail.weights_version ?? detail.weightsVersion;
  return version ? `${parts.join(' · ')}\n权重模板：${version}` : parts.join(' · ');
}
