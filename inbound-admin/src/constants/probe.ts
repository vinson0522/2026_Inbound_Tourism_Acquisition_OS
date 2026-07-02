/** EPIC-11 M1 — probe node UI (FR-113) */

export type ProbeNodeEntityStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ARCHIVED';

export const PROBE_PLATFORM_LABELS: Record<string, string> = {
  perplexity: 'Perplexity',
  chatgpt: 'ChatGPT',
  gemini: 'Gemini',
  openai: 'OpenAI',
  doubao: '豆包'
};

export const PROBE_NODE_STATUS_META: Record<
  ProbeNodeEntityStatus,
  { label: string; type: 'success' | 'info' | 'warning' | 'danger' }
> = {
  ACTIVE: { label: '启用', type: 'success' },
  INACTIVE: { label: '停用', type: 'info' },
  SUSPENDED: { label: '暂停', type: 'warning' },
  ARCHIVED: { label: '已归档', type: 'info' }
};

export const PROBE_ONLINE_META = {
  online: { label: '在线', dotClass: 'is-online' },
  offline: { label: '离线', dotClass: 'is-offline' },
  never: { label: '未连接', dotClass: 'is-never' }
} as const;

export function probePlatformLabel(slug?: string | null): string {
  if (!slug) return '—';
  return PROBE_PLATFORM_LABELS[slug.toLowerCase()] ?? slug;
}

export function probeNodeStatusMeta(status?: string | null) {
  if (!status) {
    return { label: '—', type: 'info' as const };
  }
  return (
    PROBE_NODE_STATUS_META[status as ProbeNodeEntityStatus] ?? {
      label: status,
      type: 'info' as const
    }
  );
}

export function probeOnlineMeta(row: { online?: boolean; lastHeartbeatAt?: string | null; status?: string }) {
  if (row.status && row.status !== 'ACTIVE') {
    return { kind: 'offline' as const, ...PROBE_ONLINE_META.offline };
  }
  if (!row.lastHeartbeatAt) {
    return { kind: 'never' as const, ...PROBE_ONLINE_META.never };
  }
  if (row.online) {
    return { kind: 'online' as const, ...PROBE_ONLINE_META.online };
  }
  return { kind: 'offline' as const, ...PROBE_ONLINE_META.offline };
}

export const PROBE_CONFIG_EXAMPLE = {
  PLASMO_PUBLIC_API_BASE: 'http://localhost:8080',
  PLASMO_PUBLIC_NODE_KEY: 'demo-probe-1'
};
