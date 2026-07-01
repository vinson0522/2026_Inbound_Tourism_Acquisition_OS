export interface LandingModule {
  key: string;
  content?: Record<string, unknown>;
}

export interface PublicLandingPage {
  id: number;
  projectId: number;
  title: string;
  slug: string;
  contentJson?: {
    modules?: LandingModule[];
  };
  seoMetaJson?: SeoMetaJson;
  formConfigJson?: FormConfigJson;
  whatsappLink?: string | null;
  turnstileSiteKey?: string | null;
}

export interface SeoMetaJson {
  title?: string;
  description?: string;
  h1?: string;
  faq_schema?: unknown[];
}

export interface FormConfigJson {
  fields?: string[];
  submit_label?: string;
  submitLabel?: string;
  whatsapp_link?: string;
  whatsappLink?: string;
  whatsapp_label?: string;
  whatsappLabel?: string;
}

export interface ApiEnvelope<T> {
  code?: number;
  msg?: string;
  message?: string;
  data?: T;
}
