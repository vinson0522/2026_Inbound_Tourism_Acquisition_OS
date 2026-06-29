import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { CustomerProjectForm, CustomerProjectQuery, CustomerProjectVo, PageResult } from './types';

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
