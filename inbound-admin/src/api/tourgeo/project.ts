import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type {
  CompetitorForm,
  CompetitorVo,
  CustomerProjectForm,
  CustomerProjectQuery,
  CustomerProjectVo,
  KnowledgeAssetForm,
  KnowledgeAssetQuery,
  KnowledgeAssetVo,
  KnowledgeRagSearchResult,
  PageResult,
  TravelProductForm,
  TravelProductVo
} from './types';

/** 分页列表 */
export function listProjects(query: CustomerProjectQuery): AxiosPromise<PageResult<CustomerProjectVo>> {
  return request({
    url: '/api/v1/projects',
    method: 'get',
    params: query
  });
}

/** 下拉选项（全量，当前租户） */
export function listProjectOptions(): AxiosPromise<CustomerProjectVo[]> {
  return request({
    url: '/api/v1/projects/options',
    method: 'get'
  });
}

export function getProject(id: number): AxiosPromise<CustomerProjectVo> {
  return request({
    url: `/api/v1/projects/${id}`,
    method: 'get'
  });
}

export function createProject(data: CustomerProjectForm): AxiosPromise<number> {
  return request({
    url: '/api/v1/projects',
    method: 'post',
    data
  });
}

export function updateProject(id: number, data: CustomerProjectForm): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${id}`,
    method: 'put',
    data
  });
}

export function deleteProject(id: number): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${id}`,
    method: 'delete'
  });
}

/** FR-003 产品路线 */
export async function listTravelProducts(projectId: number): Promise<TravelProductVo[]> {
  const res = await request({
    url: `/api/v1/projects/${projectId}/products`,
    method: 'get'
  });
  return res.data ?? [];
}

export function createTravelProduct(projectId: number, data: TravelProductForm): AxiosPromise<number> {
  return request({
    url: `/api/v1/projects/${projectId}/products`,
    method: 'post',
    data
  });
}

export function updateTravelProduct(projectId: number, productId: number, data: TravelProductForm): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/products/${productId}`,
    method: 'put',
    data
  });
}

export function deleteTravelProduct(projectId: number, productId: number): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/products/${productId}`,
    method: 'delete'
  });
}

/** FR-002 竞品 */
export async function listCompetitors(projectId: number): Promise<CompetitorVo[]> {
  const res = await request({
    url: `/api/v1/projects/${projectId}/competitors`,
    method: 'get'
  });
  return res.data ?? [];
}

export function createCompetitor(projectId: number, data: CompetitorForm): AxiosPromise<number> {
  return request({
    url: `/api/v1/projects/${projectId}/competitors`,
    method: 'post',
    data
  });
}

export function updateCompetitor(projectId: number, competitorId: number, data: CompetitorForm): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/competitors/${competitorId}`,
    method: 'put',
    data
  });
}

export function deleteCompetitor(projectId: number, competitorId: number): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/competitors/${competitorId}`,
    method: 'delete'
  });
}

/** FR-004 知识库 */
export function listKnowledgeAssets(projectId: number, query: KnowledgeAssetQuery): AxiosPromise<PageResult<KnowledgeAssetVo>> {
  return request({
    url: `/api/v1/projects/${projectId}/knowledge-assets`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      title: query.title || undefined,
      type: query.type || undefined,
      vectorStatus: query.vectorStatus || undefined
    }
  });
}

export function createKnowledgeAsset(projectId: number, data: KnowledgeAssetForm): AxiosPromise<number> {
  return request({
    url: `/api/v1/projects/${projectId}/knowledge-assets`,
    method: 'post',
    data
  });
}

export function updateKnowledgeAsset(projectId: number, assetId: number, data: KnowledgeAssetForm): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/knowledge-assets/${assetId}`,
    method: 'put',
    data
  });
}

export function deleteKnowledgeAsset(projectId: number, assetId: number): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/knowledge-assets/${assetId}`,
    method: 'delete'
  });
}

export function reindexKnowledgeAsset(projectId: number, assetId: number): AxiosPromise<void> {
  return request({
    url: `/api/v1/projects/${projectId}/knowledge-assets/${assetId}/reindex`,
    method: 'post'
  });
}

/** FR-005 知识库 RAG 检索预览 */
export async function searchKnowledgeRag(
  projectId: number,
  query: string,
  topK = 3
): Promise<KnowledgeRagSearchResult> {
  const res = await request({
    url: `/api/v1/projects/${projectId}/knowledge-assets/search`,
    method: 'post',
    data: { query, topK }
  });
  const hits = (res.data?.hits ?? []).map((raw: Record<string, unknown>) => ({
    chunkId: Number(raw.chunkId),
    assetId: Number(raw.assetId),
    chunkIndex: Number(raw.chunkIndex ?? 0),
    chunkText: String(raw.chunkText ?? ''),
    score: Number(raw.score ?? 0)
  }));
  return { hits };
}
