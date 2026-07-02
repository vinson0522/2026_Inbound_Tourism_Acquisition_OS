import request from '@/utils/request';
import type { ProbeNodeVo } from './types';

const BASE = '/api/v1/probe/nodes';

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
