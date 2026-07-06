/// <reference path="../.astro/types.d.ts" />

interface ImportMetaEnv {
  readonly PUBLIC_API_BASE_URL: string;
  readonly PUBLIC_TURNSTILE_SITE_KEY: string;
  readonly PUBLIC_LANDING_BASE_URL: string;
  readonly PUBLIC_SITE_URL: string;
  readonly PUBLIC_ADMIN_URL: string;
  readonly PUBLIC_DEMO_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
