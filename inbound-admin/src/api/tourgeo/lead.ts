import request from '@/utils/request';
import type { LeadDetailVo, LeadQuery, LeadVo, PageResult } from './types';

const BASE = '/api/v1';

export async function listLeads(projectId: number, query: LeadQuery): Promise<PageResult<LeadVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      email: query.email || undefined,
      phone: query.phone || undefined,
      source: query.source || undefined,
      status: query.status || undefined
    }
  });
  const rows = (res.rows ?? []) as LeadVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function getLead(projectId: number, leadId: number): Promise<LeadDetailVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads/${leadId}`,
    method: 'get'
  });
  return res.data as LeadDetailVo;
}
