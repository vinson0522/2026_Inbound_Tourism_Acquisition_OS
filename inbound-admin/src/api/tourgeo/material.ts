import request from '@/utils/request';
import type {
  MaterialAssetQuery,
  MaterialAssetVo,
  MaterialBreakdownTriggerResult,
  MaterialUploadForm,
  PageResult,
  VideoBreakdownVo
} from './types';

const BASE = '/api/v1';

export async function listMaterials(
  projectId: number,
  query: MaterialAssetQuery
): Promise<PageResult<MaterialAssetVo>> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/materials`,
    method: 'get',
    params: {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      type: query.type || undefined,
      copyrightStatus: query.copyrightStatus || undefined,
      breakdownStatus: query.breakdownStatus || undefined
    }
  });
  const rows = (res.rows ?? []) as MaterialAssetVo[];
  return { rows, total: res.total ?? rows.length };
}

export async function uploadMaterial(projectId: number, data: MaterialUploadForm): Promise<number> {
  const form = new FormData();
  form.append('file', data.file);
  if (data.type) form.append('type', data.type);
  if (data.copyrightStatus) form.append('copyrightStatus', data.copyrightStatus);
  if (data.source) form.append('source', data.source);

  const res = await request({
    url: `${BASE}/projects/${projectId}/materials`,
    method: 'post',
    data: form,
    timeout: 120000
  });
  return res.data as number;
}

export async function triggerMaterialBreakdown(
  projectId: number,
  materialId: number
): Promise<MaterialBreakdownTriggerResult> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/materials/${materialId}/breakdown`,
    method: 'post'
  });
  return res.data as MaterialBreakdownTriggerResult;
}

export async function getMaterialBreakdown(projectId: number, breakdownId: number): Promise<VideoBreakdownVo> {
  const res = await request({
    url: `${BASE}/projects/${projectId}/breakdowns/${breakdownId}`,
    method: 'get'
  });
  return res.data as VideoBreakdownVo;
}
