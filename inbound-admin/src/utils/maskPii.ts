/** EPIC-7 M1 — 列表脱敏（详情页展示全量） */

export function maskEmail(email?: string | null): string {
  if (!email?.trim()) return '—';
  const value = email.trim();
  const at = value.indexOf('@');
  if (at <= 0) return '***';
  const local = value.slice(0, at);
  const domain = value.slice(at);
  if (local.length <= 1) return `*${domain}`;
  return `${local[0]}***${domain}`;
}

export function maskPhone(phone?: string | null): string {
  if (!phone?.trim()) return '—';
  const value = phone.trim();
  const digits = value.replace(/\D/g, '');
  if (digits.length <= 4) return '••••';
  const last4 = digits.slice(-4);
  if (value.startsWith('+')) {
    const prefixMatch = value.match(/^\+[\d]{1,3}/);
    const prefix = prefixMatch ? prefixMatch[0] : '+';
    return `${prefix}•••${last4}`;
  }
  return `•••${last4}`;
}
