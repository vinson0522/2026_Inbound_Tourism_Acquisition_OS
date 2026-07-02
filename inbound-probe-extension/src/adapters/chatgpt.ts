/**
 * ChatGPT platform adapter — aligned with platform_adapter seed (v1.0).
 * parse_rules_json: {"citationsPath": "message.metadata.citations", "answerPath": "message.content.parts"}
 * api_patterns_json: {"chatApi": "/backend-api/conversation", "sseApi": "/backend-api/conversation"}
 * dom_selectors_json: {"input": "#prompt-textarea", "submit": "button[data-testid=send-button]"}
 */

import type { PlatformAdapterConfig, ProbeCaptureResult, ProbeCitation } from "~lib/types"

export const DEFAULT_CHATGPT_ADAPTER: PlatformAdapterConfig = {
  platform: "chatgpt",
  version: "1.0",
  domSelectors: {
    input: "#prompt-textarea",
    submit: "button[data-testid=send-button]"
  },
  apiPatterns: {
    chatApi: "/backend-api/conversation",
    sseApi: "/backend-api/conversation"
  },
  parseRules: {
    citationsPath: "message.metadata.citations",
    answerPath: "message.content.parts"
  }
}

export function mergeAdapter(remote?: PlatformAdapterConfig): PlatformAdapterConfig {
  if (!remote) {
    return DEFAULT_CHATGPT_ADAPTER
  }
  return {
    platform: remote.platform ?? DEFAULT_CHATGPT_ADAPTER.platform,
    version: remote.version ?? DEFAULT_CHATGPT_ADAPTER.version,
    domSelectors: { ...DEFAULT_CHATGPT_ADAPTER.domSelectors, ...remote.domSelectors },
    apiPatterns: { ...DEFAULT_CHATGPT_ADAPTER.apiPatterns, ...remote.apiPatterns },
    parseRules: { ...DEFAULT_CHATGPT_ADAPTER.parseRules, ...remote.parseRules }
  }
}

function getByPath(obj: unknown, path: string): unknown {
  if (!path || obj == null) {
    return undefined
  }
  let cur: unknown = obj
  for (const part of path.split(".")) {
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

function partsToText(parts: unknown): string {
  if (typeof parts === "string" && parts.trim()) {
    return parts.trim()
  }
  if (!Array.isArray(parts)) {
    return ""
  }
  return parts
    .map((part) => {
      if (typeof part === "string") {
        return part
      }
      if (part && typeof part === "object") {
        const row = part as Record<string, unknown>
        if (typeof row.text === "string") {
          return row.text
        }
        if (typeof row.content === "string") {
          return row.content
        }
      }
      return ""
    })
    .join("")
    .trim()
}

function extractAnswer(body: Record<string, unknown>, answerPath: string): string {
  const direct = getByPath(body, answerPath)
  const fromPath = partsToText(direct)
  if (fromPath) {
    return fromPath
  }

  const candidates = [
    getByPath(body, "message.content.parts"),
    getByPath(body, "message.content"),
    getByPath(body, "content.parts"),
    getByPath(body, "v"),
    getByPath(body, "text")
  ]
  for (const candidate of candidates) {
    const text = partsToText(candidate)
    if (text) {
      return text
    }
    if (typeof candidate === "string" && candidate.trim()) {
      return candidate.trim()
    }
  }

  return ""
}

function extractCitations(body: Record<string, unknown>, citationsPath: string): ProbeCitation[] {
  const rawList =
    getByPath(body, citationsPath) ??
    getByPath(body, "message.metadata.citations") ??
    getByPath(body, "metadata.citations") ??
    getByPath(body, "citations")
  if (!Array.isArray(rawList)) {
    return []
  }
  const out: ProbeCitation[] = []
  rawList.forEach((item, idx) => {
    const citation = normalizeCitation(item, idx + 1)
    if (citation) {
      out.push(citation)
    }
  })
  return out
}

export function parseChatgptPayload(
  payload: unknown,
  adapter: PlatformAdapterConfig
): ProbeCaptureResult | null {
  if (!payload || typeof payload !== "object") {
    return null
  }
  const body = payload as Record<string, unknown>
  const rules = adapter.parseRules ?? DEFAULT_CHATGPT_ADAPTER.parseRules!
  const citationsPath = rules.citationsPath ?? "message.metadata.citations"
  const answerPath = rules.answerPath ?? "message.content.parts"

  const answer_text = extractAnswer(body, answerPath)
  const citations = extractCitations(body, citationsPath)

  if (!answer_text && citations.length === 0) {
    return null
  }

  return {
    probe_mode: "browser-extension",
    platform: "chatgpt",
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
    platform: "chatgpt",
    answer_text: `[mock chatgpt] Answer for: ${question.slice(0, 120)}`,
    citations: [
      {
        url: "https://example.com/chatgpt-citation",
        title: "ChatGPT Mock Citation",
        domain: "example.com",
        rank: 1
      }
    ],
    mentioned_brands: ["Dragon Journey Travel"],
    raw_response_json: { mock: true, platform: "chatgpt" },
    capture_method: "browser-extension-mock"
  }
}

export function urlMatchesChatApi(url: string, adapter: PlatformAdapterConfig): boolean {
  const chatApi = adapter.apiPatterns?.chatApi ?? DEFAULT_CHATGPT_ADAPTER.apiPatterns!.chatApi!
  const sseApi = adapter.apiPatterns?.sseApi ?? chatApi
  return Boolean((chatApi && url.includes(chatApi)) || (sseApi && url.includes(sseApi)))
}
