/** Normalize AI / wireframe field name variants */
export function pickHeading(content: Record<string, unknown> | undefined, fallback = ''): string {
  if (!content) return fallback;
  const v =
    content.headline ??
    content.heading ??
    content.title ??
    content.h1 ??
    fallback;
  return String(v);
}

export function pickSubtitle(content: Record<string, unknown> | undefined): string {
  if (!content) return '';
  const v = content.subtitle ?? content.subheading ?? content.subheadline ?? '';
  return String(v);
}

export function pickCtaLabel(content: Record<string, unknown> | undefined, fallback = 'Get started'): string {
  if (!content) return fallback;
  const v = content.cta_text ?? content.ctaLabel ?? content.button_label ?? content.label ?? fallback;
  return String(v);
}

export function asStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map((v) => String(v));
}

export function asRecordArray(value: unknown): Record<string, unknown>[] {
  if (!Array.isArray(value)) return [];
  return value.filter((v) => v && typeof v === 'object') as Record<string, unknown>[];
}
