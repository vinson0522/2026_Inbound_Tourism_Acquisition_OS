import request from '@/utils/request';
import type {
  LeadAiSuggestionVo,
  LeadDetailVo,
  LeadFollowupCreateForm,
  LeadFollowupVo,
  LeadQuery,
  LeadUpdateForm,
  LeadVo,
  PageResult
} from './types';

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

/** FR-605 更新线索状态 / 负责人 */
export async function patchLead(projectId: number, leadId: number, data: LeadUpdateForm): Promise<LeadDetailVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads/${leadId}`,
    method: 'patch',
    data
  });
  return res.data as LeadDetailVo;
}

/** FR-605 跟进记录列表（时间 ASC） */
export async function listFollowups(projectId: number, leadId: number): Promise<LeadFollowupVo[]> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads/${leadId}/followups`,
    method: 'get'
  });
  return (res.data ?? []) as LeadFollowupVo[];
}

/** FR-605 新增跟进记录 */
export async function createFollowup(
  projectId: number,
  leadId: number,
  data: LeadFollowupCreateForm
): Promise<LeadFollowupVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads/${leadId}/followups`,
    method: 'post',
    data
  });
  return res.data as LeadFollowupVo;
}

/** FR-603 AI 跟进话术建议 */
export async function generateLeadAiSuggestion(projectId: number, leadId: number): Promise<LeadAiSuggestionVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/leads/${leadId}/ai-suggestion`,
    method: 'post',
    data: {}
  });
  return res.data as LeadAiSuggestionVo;
}
