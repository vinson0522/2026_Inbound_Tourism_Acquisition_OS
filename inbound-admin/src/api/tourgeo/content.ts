import request from '@/utils/request';
import type {
  ContentGenerateForm,
  ContentGenerateResult,
  ContentTaskDetailVo,
  ContentTaskForm,
  ContentTaskQuery,
  ContentTaskVo,
  PageResult
} from './types';

const BASE = '/api/v1';

export async function listContentTasks(
  projectId: number,
  query: ContentTaskQuery
): Promise<PageResult<ContentTaskVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/content-tasks`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status || undefined,
      platform: query.platform || undefined
    }
  });
  const rows = (res.rows ?? []) as ContentTaskVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function getContentTask(projectId: number, taskId: number): Promise<ContentTaskDetailVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/content-tasks/${taskId}`,
    method: 'get'
  });
  return res.data as ContentTaskDetailVo;
}

export async function createContentTask(projectId: number, data: ContentTaskForm): Promise<number> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/content-tasks`,
    method: 'post',
    data
  });
  return res.data as number;
}

export async function deleteContentTask(projectId: number, taskId: number): Promise<void> {
  await request({
    url: `${BASE}/projects/${projectId}/content-tasks/${taskId}`,
    method: 'delete'
  });
}

export async function generateContentScript(
  projectId: number,
  taskId: number,
  data?: ContentGenerateForm
): Promise<ContentGenerateResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/content-tasks/${taskId}/generate`,
    method: 'post',
    data: data ?? {}
  });
  return res.data as ContentGenerateResult;
}
