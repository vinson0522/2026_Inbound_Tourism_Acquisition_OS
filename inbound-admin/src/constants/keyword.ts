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

/** M1 机会分色阶 — docs/design/tokens.md §1.4 */
export function scoreColorClass(score: number | null | undefined): string {
  if (score == null || Number.isNaN(Number(score))) return '';
  const n = Number(score);
  if (n >= 80) return 'score-high';
  if (n >= 50) return 'score-mid';
  return 'score-low';
}
