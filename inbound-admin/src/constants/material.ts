/** EPIC-5 M1 — material_asset / video_breakdown UI 映射 */

export type MaterialAssetType = 'VIDEO' | 'IMAGE' | 'AUDIO' | 'OTHER';

export type MaterialCopyrightStatus = 'unknown' | 'licensed' | 'external' | 'owned';

export type MaterialBreakdownStatus = 'NONE' | 'PROCESSING' | 'SUCCESS' | 'FAILED';

export const MATERIAL_TYPES = [
  { value: 'VIDEO', label: '视频' },
  { value: 'IMAGE', label: '图片' }
] as const;

export const MATERIAL_COPYRIGHT_OPTIONS = [
  { value: 'external', label: '外部参考' },
  { value: 'unknown', label: '未知' },
  { value: 'licensed', label: '已授权' },
  { value: 'owned', label: '自有素材' }
] as const;

export const MATERIAL_BREAKDOWN_STATUS_META: Record<
  MaterialBreakdownStatus,
  { label: string; type: 'info' | 'primary' | 'success' | 'danger' }
> = {
  NONE: { label: '未拆解', type: 'info' },
  PROCESSING: { label: '拆解中', type: 'primary' },
  SUCCESS: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'danger' }
};

export const MATERIAL_TYPE_META: Record<
  MaterialAssetType,
  { label: string; type: 'primary' | 'success' | 'info' }
> = {
  VIDEO: { label: '视频', type: 'primary' },
  IMAGE: { label: '图片', type: 'success' },
  AUDIO: { label: '音频', type: 'info' },
  OTHER: { label: '其他', type: 'info' }
};

export const MATERIAL_COPYRIGHT_META: Record<
  MaterialCopyrightStatus,
  { label: string; type: 'info' | 'success' | 'warning' }
> = {
  unknown: { label: '未知', type: 'info' },
  licensed: { label: '已授权', type: 'success' },
  external: { label: '外部参考', type: 'warning' },
  owned: { label: '自有素材', type: 'success' }
};

export const BREAKDOWN_DIMENSIONS = [
  { key: 'theme', label: '主题' },
  { key: 'hook', label: '钩子' },
  { key: 'shot', label: '镜头' },
  { key: 'subtitle', label: '字幕' },
  { key: 'emotion', label: '情绪' },
  { key: 'psychology', label: '心理' },
  { key: 'reusable', label: '可复用结构' }
] as const;

export const MATERIAL_UPLOAD_ACCEPT = '.mp4,.mov,.webm,.jpg,.jpeg,.png,.webp';

export const MATERIAL_MAX_FILE_MB = 200;

export const COPYRIGHT_FOOTNOTE =
  '外部爆款素材仅供团队内部结构学习，请勿直接搬运发布。用于客户交付或对外导出前请确认版权归属。（FR-405 · M1 提示不拦截）';

export const NEEDS_REVIEW_TOOLTIP = 'AI 拆解结果需人工确认后再用于客户交付';

export function materialTypeMeta(type?: string) {
  return (
    MATERIAL_TYPE_META[type as MaterialAssetType] ?? {
      label: type || '—',
      type: 'info' as const
    }
  );
}

export function materialCopyrightMeta(status?: string) {
  return (
    MATERIAL_COPYRIGHT_META[status as MaterialCopyrightStatus] ?? {
      label: status || '—',
      type: 'info' as const
    }
  );
}

export function breakdownStatusMeta(status?: string) {
  return (
    MATERIAL_BREAKDOWN_STATUS_META[status as MaterialBreakdownStatus] ?? {
      label: status || '—',
      type: 'info' as const
    }
  );
}

export function inferMaterialType(filename: string, mime?: string): MaterialAssetType {
  const lower = filename.toLowerCase();
  if (/\.(mp4|mov|webm)$/.test(lower) || mime?.startsWith('video/')) return 'VIDEO';
  if (/\.(jpg|jpeg|png|webp|gif)$/.test(lower) || mime?.startsWith('image/')) return 'IMAGE';
  return 'OTHER';
}
