import request from '@/utils/request';
import type {
  LandingGenerateForm,
  LandingGenerateResult,
  LandingPageDetailVo,
  LandingPageForm,
  LandingPageQuery,
  LandingPageVo,
  PageResult
} from './types';

const BASE = '/api/v1';

export async function listLandingPages(
  projectId: number,
  query: LandingPageQuery
): Promise<PageResult<LandingPageVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/landing-pages`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status || undefined,
      templateType: query.templateType || undefined
    }
  });
  const rows = (res.rows ?? []) as LandingPageVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function getLandingPage(projectId: number, pageId: number): Promise<LandingPageDetailVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/landing-pages/${pageId}`,
    method: 'get'
  });
  return res.data as LandingPageDetailVo;
}

export async function createLandingPage(projectId: number, data: LandingPageForm): Promise<number> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/landing-pages`,
    method: 'post',
    data
  });
  return res.data as number;
}

export async function deleteLandingPage(projectId: number, pageId: number): Promise<void> {
  await request({
    url: `${BASE}/projects/${projectId}/landing-pages/${pageId}`,
    method: 'delete'
  });
}

export async function generateLandingPage(
  projectId: number,
  pageId: number,
  data?: LandingGenerateForm
): Promise<LandingGenerateResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/landing-pages/${pageId}/generate`,
    method: 'post',
    data: data ?? {}
  });
  return res.data as LandingGenerateResult;
}
