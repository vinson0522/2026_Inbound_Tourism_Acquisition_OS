import request from '@/utils/request';
import type { SubscriptionUpdateDto, SubscriptionVo } from './types';

const BASE = '/api/v1/settings/billing';

export async function getCurrentSubscription(): Promise<SubscriptionVo> {
  const res = await request({
    url: BASE,
    method: 'get',
    silentError: true
  } as Parameters<typeof request>[0] & { silentError?: boolean });
  return res.data as SubscriptionVo;
}

export async function updateSubscription(body: SubscriptionUpdateDto): Promise<SubscriptionVo> {
  const res = await request({
    url: `${BASE}/subscription`,
    method: 'put',
    data: body
  });
  return res.data as SubscriptionVo;
}

export async function resetBillingPeriod(): Promise<SubscriptionVo> {
  const res = await request({
    url: `${BASE}/period-reset`,
    method: 'post'
  });
  return res.data as SubscriptionVo;
}
