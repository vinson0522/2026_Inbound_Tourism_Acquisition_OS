/**
 * Perplexity platform adapter — web UI fetch/SSE hook (FR-112 / EPIC-11).
 * Supports legacy /api/chat JSON, /rest/sse/perplexity_ask stream chunks, search_results.
 */

import type { PlatformAdapterConfig, ProbeCaptureResult, ProbeCitation } from "~lib/types"

export const DEFAULT_PERPLEXITY_ADAPTER: PlatformAdapterConfig = {
  platform: "perplexity",
  version: "1.0",
  domSelectors: {
    input: "textarea, [contenteditable='true'], #ask-input",
    submit: "button[type=submit], button[aria-label='Submit']"
  },
  apiPatterns: {
    chatApi: "/rest/sse/perplexity_ask",
    sseApi: "/rest/sse/perplexity_ask"
  },
  parseRules: {
    citationsPath: "citations",
    answerPath: "answer"
  }
}

/** Known Perplexity web endpoints (internal UI, not public API). */
export const PERPLEXITY_HOOK_URL_MARKERS = [
  "/rest/sse/perplexity_ask",
  "/api/chat",
  "/rest/thread",
  "/search/stream",
  "/socket.io"
] as const

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
    const url = raw.trim()
    if (!url.startsWith("http")) {
      return null
    }
    return { url, domain: domainFromUrl(url), rank }
  }
  if (raw && typeof raw === "object") {
    const row = raw as Record<string, unknown>
    const url = String(row.url ?? row.link ?? row.href ?? row.source ?? "")
    if (!url.startsWith("http")) {
      return null
    }
    return {
      url,
      title:
        row.title != null
          ? String(row.title)
          : row.name != null
            ? String(row.name)
            : undefined,
      domain: row.domain != null ? String(row.domain) : domainFromUrl(url),
      rank: typeof row.rank === "number" ? row.rank : rank
    }
  }
  return null
}

function collectCitationSources(body: Record<string, unknown>, citationsPath: string): unknown[] {
  const paths = [
    citationsPath,
    "citations",
    "sources",
    "web_results",
    "search_results",
    "related_web_results",
    "hidden_params.citations",
    "metadata.citations",
    "message.metadata.citations"
  ]
  for (const path of paths) {
    const raw = getByPath(body, path)
    if (Array.isArray(raw) && raw.length > 0) {
      return raw
    }
  }
  return []
}

function extractAnswer(body: Record<string, unknown>, answerPath: string): string {
  const direct = getByPath(body, answerPath)
  if (typeof direct === "string" && direct.trim()) {
    return direct.trim()
  }

  const candidates = [
    getByPath(body, "text"),
    getByPath(body, "displayed_text"),
    getByPath(body, "output"),
    getByPath(body, "delta"),
    getByPath(body, "message.content"),
    getByPath(body, "choices.0.message.content"),
    getByPath(body, "choices.0.delta.content"),
    getByPath(body, "answer"),
    getByPath(body, "final"),
    getByPath(body, "data.text")
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
        if (b && typeof b === "object") {
          const row = b as Record<string, unknown>
          if (typeof row.text === "string") {
            return row.text
          }
          if (typeof row.markdown_block === "string") {
            return row.markdown_block
          }
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
  const sources = collectCitationSources(body, citationsPath)
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
    capture_method: "browser-extension-hook"
  }
}

/** Merge streaming / multi-chunk payloads into one capture (SSE accumulates text + citations). */
export function mergePerplexityCapture(
  current: ProbeCaptureResult | null,
  chunk: ProbeCaptureResult
): ProbeCaptureResult {
  if (!current) {
    return chunk
  }

  const mergedCitations = new Map<string, ProbeCitation>()
  for (const c of [...current.citations, ...chunk.citations]) {
    mergedCitations.set(c.url, c)
  }
  const citations = [...mergedCitations.values()].map((c, idx) => ({ ...c, rank: idx + 1 }))

  const currentAnswer = current.answer_text === "(no answer text captured)" ? "" : current.answer_text
  const chunkAnswer = chunk.answer_text === "(no answer text captured)" ? "" : chunk.answer_text
  let answer_text = currentAnswer
  if (chunkAnswer) {
    if (!answer_text) {
      answer_text = chunkAnswer
    } else if (chunkAnswer.startsWith(answer_text)) {
      answer_text = chunkAnswer
    } else if (!answer_text.includes(chunkAnswer)) {
      answer_text = `${answer_text}${chunkAnswer}`
    }
  }

  return {
    ...current,
    answer_text: answer_text || "(no answer text captured)",
    citations,
    raw_response_json: {
      ...(typeof current.raw_response_json === "object" && current.raw_response_json
        ? (current.raw_response_json as Record<string, unknown>)
        : {}),
      ...(typeof chunk.raw_response_json === "object" && chunk.raw_response_json
        ? (chunk.raw_response_json as Record<string, unknown>)
        : {})
    },
    capture_method: "browser-extension-hook"
  }
}

export function isCaptureReady(result: ProbeCaptureResult | null): boolean {
  if (!result) {
    return false
  }
  const hasAnswer =
    Boolean(result.answer_text) && result.answer_text !== "(no answer text captured)" && result.answer_text.length > 10
  return result.citations.length >= 1 && hasAnswer
}

/** Parse SSE / NDJSON text from Perplexity fetch stream. */
export function parsePerplexityStreamText(
  text: string,
  adapter: PlatformAdapterConfig,
  onChunk: (chunk: ProbeCaptureResult) => void
): ProbeCaptureResult | null {
  let merged: ProbeCaptureResult | null = null
  const lines = text.split(/\r?\n/)
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed || trimmed === "data: [DONE]") {
      continue
    }
    const payloadText = trimmed.startsWith("data:") ? trimmed.slice(5).trim() : trimmed
    if (!payloadText.startsWith("{")) {
      continue
    }
    try {
      const payload = JSON.parse(payloadText) as unknown
      const parsed = parsePerplexityPayload(payload, adapter)
      if (parsed) {
        merged = mergePerplexityCapture(merged, parsed)
        onChunk(parsed)
      }
    } catch {
      /* ignore partial json lines */
    }
  }
  return merged
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

export function urlMatchesChatApi(url: string, adapter: PlatformAdapterConfig): boolean {
  const chatApi = adapter.apiPatterns?.chatApi ?? DEFAULT_PERPLEXITY_ADAPTER.apiPatterns!.chatApi!
  const sseApi = adapter.apiPatterns?.sseApi ?? chatApi
  const markers = new Set<string>([chatApi, sseApi, ...PERPLEXITY_HOOK_URL_MARKERS])
  for (const marker of markers) {
    if (marker && url.includes(marker)) {
      return true
    }
  }
  return false
}
