/** PRD §20.3 — aligned with inbound-ai LANDING_MODULE_KEYS */
export const LANDING_MODULE_KEYS = [
  'hero',
  'why_this_trip',
  'itinerary',
  'what_we_provide',
  'traveler_reviews',
  'faq',
  'lead_form',
  'whatsapp_cta',
] as const;

export type LandingModuleKey = (typeof LANDING_MODULE_KEYS)[number];
