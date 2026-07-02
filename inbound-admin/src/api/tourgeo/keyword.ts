import request from '@/utils/request';
import type {
  KeywordGenerateForm,
  KeywordGenerateResult,
  KeywordOpportunityQuery,
  KeywordOpportunityVo,
  KeywordScoreBatchForm,
  KeywordScoreBatchResult,
  KeywordScoreResult,
  PageResult
} from './types';

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
      status: query.status || undefined,
      orderByColumn: query.orderByColumn || undefined,
      isAsc: query.isAsc || undefined
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

/** FR-203 单条关键词机会评分 */
export async function scoreKeyword(
  projectId: number,
  keywordId: number,
  options?: { useRag?: boolean }
): Promise<KeywordScoreResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/keywords/${keywordId}/score`,
    method: 'post',
    params: { useRag: options?.useRag },
    data: {}
  });
  return res.data as KeywordScoreResult;
}

/** FR-203 批量评分（空 keywordIds = 项目 ACTIVE 最多 50 条） */
export async function scoreKeywordsBatch(
  projectId: number,
  data?: KeywordScoreBatchForm
): Promise<KeywordScoreBatchResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/keywords/score-batch`,
    method: 'post',
    data: data ?? {}
  });
  return res.data as KeywordScoreBatchResult;
}
