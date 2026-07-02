/**
 * Perplexity platform adapter — aligned with platform_adapter seed (v1.0).
 * parse_rules_json: {"citationsPath": "citations"}
 * api_patterns_json: {"chatApi": "/api/chat"}
 * dom_selectors_json: {"input": "textarea", "submit": "button[type=submit]"}
 */

import type { PlatformAdapterConfig, ProbeCaptureResult, ProbeCitation } from "~lib/types"

export const DEFAULT_PERPLEXITY_ADAPTER: PlatformAdapterConfig = {
  platform: "perplexity",
  version: "1.0",
  domSelectors: {
    input: "textarea",
    submit: "button[type=submit]"
  },
  apiPatterns: {
    chatApi: "/api/chat"
  },
  parseRules: {
    citationsPath: "citations",
    answerPath: "answer"
  }
}

export function mergeAdapter(remote?: PlatformAdapterConfig): PlatformAdapterConfig {
  if (!remote) {
    return DEFAULT_PERPLEXITY_ADAPTER
  }
  return {
    platform: remote.platform ?? DEFAULT_PERPLEXITY_ADAPTER.platform,
    version: remote.version ?? DEFAULT_PERPLEXITY_ADAPTER.version,
    domSelectors: { ...DEFAULT_PERPLEXITY_ADAPTER.domSelectors, ...remote.domSelectors },
    apiPatterns: { ...DEFAULT_PERPLEXITY_ADAPTER.apiPatterns, ...remote.apiPatterns },
    parseRules: { ...DEFAULT_PERPLEXITY_ADAPTER.parseRules, ...remote.parseRules }
  }
}

function getByPath(obj: unknown, path: string): unknown {
  if (!path || obj == null) {
    return undefined
  }
  const parts = path.split(".")
  let cur: unknown = obj
  for (const part of parts) {
    if (cur == null || typeof cur !== "object") {
      return undefined
    }
    cur = (cur as Record<string, unknown>)[part]
  }
  return cur
}

function domainFromUrl(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "")
  } catch {
    return url
  }
}

function normalizeCitation(raw: unknown, rank: number): ProbeCitation | null {
  if (typeof raw === "string") {
    return { url: raw, domain: domainFromUrl(raw), rank }
  }
  if (raw && typeof raw === "object") {
    const row = raw as Record<string, unknown>
    const url = String(row.url ?? row.link ?? row.href ?? "")
    if (!url) {
      return null
    }
    return {
      url,
      title: row.title != null ? String(row.title) : row.name != null ? String(row.name) : undefined,
      domain: row.domain != null ? String(row.domain) : domainFromUrl(url),
      rank: typeof row.rank === "number" ? row.rank : rank
    }
  }
  return null
}

function extractAnswer(body: Record<string, unknown>, answerPath: string): string {
  const direct = getByPath(body, answerPath)
  if (typeof direct === "string" && direct.trim()) {
    return direct.trim()
  }

  const candidates = [
    getByPath(body, "text"),
    getByPath(body, "message.content"),
    getByPath(body, "choices.0.message.content"),
    getByPath(body, "output"),
    getByPath(body, "answer")
  ]
  for (const c of candidates) {
    if (typeof c === "string" && c.trim()) {
      return c.trim()
    }
  }

  const blocks = getByPath(body, "blocks")
  if (Array.isArray(blocks)) {
    const texts = blocks
      .map((b) => {
        if (b && typeof b === "object" && typeof (b as Record<string, unknown>).text === "string") {
          return (b as Record<string, string>).text
        }
        return ""
      })
      .filter(Boolean)
    if (texts.length) {
      return texts.join("\n").trim()
    }
  }

  return ""
}

function extractCitations(body: Record<string, unknown>, citationsPath: string): ProbeCitation[] {
  const rawList = getByPath(body, citationsPath)
  const sources = rawList ?? getByPath(body, "sources") ?? getByPath(body, "web_results")
  if (!Array.isArray(sources)) {
    return []
  }
  const out: ProbeCitation[] = []
  sources.forEach((item, idx) => {
    const citation = normalizeCitation(item, idx + 1)
    if (citation) {
      out.push(citation)
    }
  })
  return out
}

export function parsePerplexityPayload(
  payload: unknown,
  adapter: PlatformAdapterConfig
): ProbeCaptureResult | null {
  if (!payload || typeof payload !== "object") {
    return null
  }
  const body = payload as Record<string, unknown>
  const rules = adapter.parseRules ?? DEFAULT_PERPLEXITY_ADAPTER.parseRules!
  const citationsPath = rules.citationsPath ?? "citations"
  const answerPath = rules.answerPath ?? "answer"

  const answer_text = extractAnswer(body, answerPath)
  const citations = extractCitations(body, citationsPath)

  if (!answer_text && citations.length === 0) {
    return null
  }

  return {
    probe_mode: "browser-extension",
    platform: "perplexity",
    answer_text: answer_text || "(no answer text captured)",
    citations,
    mentioned_brands: [],
    raw_response_json: body,
    capture_method: "browser-extension"
  }
}

export function buildMockCapture(question: string): ProbeCaptureResult {
  return {
    probe_mode: "browser-extension",
    platform: "perplexity",
    answer_text: `[mock] Answer for: ${question.slice(0, 120)}`,
    citations: [
      {
        url: "https://example.com/china-tour",
        title: "China Private Tours",
        domain: "example.com",
        rank: 1
      }
    ],
    mentioned_brands: ["Dragon Journey Travel"],
    raw_response_json: { mock: true },
    capture_method: "browser-extension-mock"
  }
}

export function urlMatchesChatApi(url: string, chatApiPattern: string): boolean {
  if (!chatApiPattern) {
    return false
  }
  return url.includes(chatApiPattern)
}
