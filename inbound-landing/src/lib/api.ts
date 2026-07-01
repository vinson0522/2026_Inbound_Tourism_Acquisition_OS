import type { ApiEnvelope, PublicLandingPage } from '../types/landing';

const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e';

export function getApiBase(): string {
  return (import.meta.env.PUBLIC_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '');
}

export async function fetchPublicLandingPage(
  projectId: string,
  slug: string
): Promise<PublicLandingPage | null> {
  const base = getApiBase();
  const url = `${base}/api/v1/public/landing-pages/${encodeURIComponent(slug)}?projectId=${encodeURIComponent(projectId)}`;

  try {
    const res = await fetch(url, {
      headers: { clientid: CLIENT_ID, Accept: 'application/json' },
      signal: AbortSignal.timeout(8_000),
    });
    if (res.status === 404) return null;
    if (!res.ok) return null;

    const json = (await res.json()) as ApiEnvelope<PublicLandingPage>;
    if (json.code !== 200 || !json.data) return null;
    return json.data;
  } catch {
    return null;
  }
}
