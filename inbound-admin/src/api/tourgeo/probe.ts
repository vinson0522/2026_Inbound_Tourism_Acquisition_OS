import request from '@/utils/request';
import type { PlatformAdapterSaveForm, PlatformAdapterVo, ProbeNodeVo } from './types';

const BASE = '/api/v1/probe/nodes';
const ADAPTER_BASE = '/api/v1/settings/platform-adapters';

export async function listProbeNodes(): Promise<ProbeNodeVo[]> {
  const res = await request({
    url: BASE,
    method: 'get',
    silentError: true
  } as Parameters<typeof request>[0] & { silentError?: boolean });
  const rows = (res.data as ProbeNodeVo[] | undefined) ?? [];
  return rows.map(mapProbeNodeVo);
}

function mapProbeNodeVo(raw: ProbeNodeVo): ProbeNodeVo {
  return {
    id: Number(raw.id),
    nodeKey: String(raw.nodeKey ?? ''),
    region: raw.region ?? '',
    platforms: Array.isArray(raw.platforms) ? raw.platforms.map(String) : [],
    extensionVersion: raw.extensionVersion ?? '',
    status: String(raw.status ?? 'ACTIVE'),
    lastHeartbeatAt: raw.lastHeartbeatAt ?? undefined,
    online: Boolean(raw.online)
  };
}

export async function listPlatformAdapters(): Promise<PlatformAdapterVo[]> {
  const res = await request.get(ADAPTER_BASE);
  const rows = (res.data as PlatformAdapterVo[] | undefined) ?? [];
  return rows.map(mapPlatformAdapterVo);
}

export async function getPlatformAdapter(platform: string): Promise<PlatformAdapterVo> {
  const res = await request.get(`${ADAPTER_BASE}/${encodeURIComponent(platform)}`);
  return mapPlatformAdapterVo(res.data as PlatformAdapterVo);
}

export async function savePlatformAdapter(
  platform: string,
  form: PlatformAdapterSaveForm
): Promise<PlatformAdapterVo> {
  const res = await request.put(`${ADAPTER_BASE}/${encodeURIComponent(platform)}`, form);
  return mapPlatformAdapterVo(res.data as PlatformAdapterVo);
}

function mapPlatformAdapterVo(raw: PlatformAdapterVo): PlatformAdapterVo {
  return {
    id: Number(raw.id),
    platform: String(raw.platform ?? ''),
    version: String(raw.version ?? '1.0'),
    enabled: raw.enabled !== false,
    domSelectorsJson: (raw.domSelectorsJson as Record<string, unknown>) ?? {},
    apiPatternsJson: (raw.apiPatternsJson as Record<string, unknown>) ?? {},
    parseRulesJson: (raw.parseRulesJson as Record<string, unknown>) ?? {},
    updatedAt: raw.updatedAt ? String(raw.updatedAt) : undefined
  };
}
