import request from '@/utils/request';
import type { KeywordGenerateForm, KeywordGenerateResult, KeywordOpportunityQuery, KeywordOpportunityVo, PageResult } from './types';

const BASE = '/api/v1';

export async function listKeywords(
  projectId: number,
  query: KeywordOpportunityQuery
): Promise<PageResult<KeywordOpportunityVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/keywords`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      stage: query.stage || undefined,
      market: query.market || undefined,
      keyword: query.keyword || undefined,
      status: query.status || undefined
    }
  });
  const rows = (res.rows ?? []) as KeywordOpportunityVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function generateKeywords(
  projectId: number,
  data: KeywordGenerateForm
): Promise<KeywordGenerateResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/keywords/generate`,
    method: 'post',
    data
  });
  return res.data as KeywordGenerateResult;
}

export async function deleteKeyword(projectId: number, keywordId: number): Promise<void> {
  await request({
    url: `${BASE}/projects/${projectId}/keywords/${keywordId}`,
    method: 'delete'
  });
}
