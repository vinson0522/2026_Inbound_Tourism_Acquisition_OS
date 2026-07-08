export interface SiteUrls {
  siteUrl: string;
  adminUrl: string;
  apiBase: string;
  turnstileSiteKey: string;
}

export function getSiteUrls(): SiteUrls {
  const siteUrl = (
    import.meta.env.PUBLIC_SITE_URL ??
    import.meta.env.PUBLIC_LANDING_BASE_URL ??
    'http://localhost:4321'
  ).replace(/\/+$/, '');

  const adminUrl = (import.meta.env.PUBLIC_ADMIN_URL ?? '#').replace(/\/+$/, '');
  const apiBase = (import.meta.env.PUBLIC_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '');
  const turnstileSiteKey = import.meta.env.PUBLIC_TURNSTILE_SITE_KEY ?? '';

  return { siteUrl, adminUrl, apiBase, turnstileSiteKey };
}
