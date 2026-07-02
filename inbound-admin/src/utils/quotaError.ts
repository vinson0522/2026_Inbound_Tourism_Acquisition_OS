import { QUOTA_EXCEEDED_CODE } from '@/constants/billing';

let lastToastAt = 0;
let lastToastMsg = '';

export class QuotaExceededError extends Error {
  readonly code = QUOTA_EXCEEDED_CODE;

  constructor(message: string) {
    super(message);
    this.name = 'QuotaExceededError';
  }
}

export function extractQuotaMessage(data: unknown): string {
  if (!data || typeof data !== 'object') {
    return '套餐额度不足，请升级';
  }
  const payload = data as { msg?: string; message?: string };
  return payload.msg || payload.message || '套餐额度不足，请升级';
}

export function isQuotaExceededPayload(data: unknown): boolean {
  if (!data || typeof data !== 'object') return false;
  return (data as { code?: number }).code === QUOTA_EXCEEDED_CODE;
}

/** 402 全局提示 — 3s 内同文案不重复弹 */
export function showQuotaExceededMessage(message?: string, duration = 5000): void {
  const msg = message?.trim() || '套餐额度不足，请升级';
  const now = Date.now();
  if (now - lastToastAt < 3000 && lastToastMsg === msg) {
    return;
  }
  lastToastAt = now;
  lastToastMsg = msg;
  ElMessage.error({ message: msg, duration, showClose: true });
}

export function isQuotaExceededError(error: unknown): error is QuotaExceededError {
  return error instanceof QuotaExceededError;
}
