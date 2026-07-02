/** Supported browser-extension probe platforms (EPIC-11 M2). */
export const PROBE_PLATFORMS = ["perplexity", "chatgpt"] as const

export type ProbePlatform = (typeof PROBE_PLATFORMS)[number]

export const PLATFORM_HOME_URL: Record<ProbePlatform, string> = {
  perplexity: "https://www.perplexity.ai/",
  chatgpt: "https://chatgpt.com/"
}

export const PLATFORM_TAB_PATTERN: Record<ProbePlatform, string> = {
  perplexity: "https://www.perplexity.ai/*",
  chatgpt: "https://chatgpt.com/*"
}

export function isProbePlatform(value: string): value is ProbePlatform {
  return (PROBE_PLATFORMS as readonly string[]).includes(value)
}
