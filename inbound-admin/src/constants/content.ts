/** EPIC-4 M1 — content_task / generated_content UI 映射 */

export const CONTENT_PLATFORMS = [
  { value: 'tiktok', label: 'TikTok' },
  { value: 'youtube_shorts', label: 'YouTube Shorts' },
  { value: 'instagram', label: 'Instagram' },
  { value: 'youtube', label: 'YouTube' },
  { value: 'facebook', label: 'Facebook' },
  { value: 'x', label: 'X' },
  { value: 'rednote', label: 'RedNote' }
] as const;

export const CONTENT_FORMATS = [
  { value: 'short_video', label: '短视频' },
  { value: 'carousel', label: '图文轮播' },
  { value: 'long_video', label: '长视频' },
  { value: 'story', label: 'Stories' }
] as const;

export const CONTENT_TONES = [
  { value: 'friendly', label: '亲切' },
  { value: 'premium', label: '高端' },
  { value: 'youthful', label: '年轻化' },
  { value: 'official', label: '官方' },
  { value: 'family', label: '家庭友好' }
] as const;

export const CONTENT_DURATIONS = [15, 30, 60] as const;

export type ContentTaskStatus =
  | 'DRAFT'
  | 'GENERATING'
  | 'GENERATED'
  | 'ADOPTED'
  | 'DISCARDED'
  | 'FAILED';

export const CONTENT_TASK_STATUS_META: Record<
  ContentTaskStatus,
  { label: string; type: 'info' | 'primary' | 'success' | 'danger' | 'warning' }
> = {
  DRAFT: { label: '草稿', type: 'info' },
  GENERATING: { label: '生成中', type: 'primary' },
  GENERATED: { label: '已生成', type: 'success' },
  ADOPTED: { label: '已采纳', type: 'success' },
  DISCARDED: { label: '已废弃', type: 'info' },
  FAILED: { label: '失败', type: 'danger' }
};

export const NEEDS_REVIEW_TOOLTIP =
  'AI 生成内容默认需人工确认价格/签证/政策类信息（PRD 合规）';

export function platformLabel(value?: string): string {
  if (!value) return '—';
  return CONTENT_PLATFORMS.find((p) => p.value === value)?.label ?? value;
}

export function formatLabel(value?: string): string {
  if (!value) return '—';
  return CONTENT_FORMATS.find((f) => f.value === value)?.label ?? value;
}

export function toneLabel(value?: string): string {
  if (!value) return '—';
  return CONTENT_TONES.find((t) => t.value === value)?.label ?? value;
}

export function contentStatusMeta(status: string) {
  return (
    CONTENT_TASK_STATUS_META[status as ContentTaskStatus] ?? {
      label: status,
      type: 'info' as const
    }
  );
}
