import request from '@/utils/request';
import type { SubscriptionVo } from './types';

const BASE = '/api/v1/settings/billing';

export async function getCurrentSubscription(): Promise<SubscriptionVo> {
  const res = await request({
    url: BASE,
    method: 'get',
    silentError: true
  } as Parameters<typeof request>[0] & { silentError?: boolean });
  return res.data as SubscriptionVo;
}
