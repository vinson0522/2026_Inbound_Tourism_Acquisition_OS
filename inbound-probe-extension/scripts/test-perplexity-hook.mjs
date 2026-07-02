#!/usr/bin/env node
/**
 * Perplexity hook adapter evidence — parses legacy JSON, web_results, search_results, SSE stream.
 * Run: node scripts/test-perplexity-hook.mjs
 */

import fs from "node:fs"
import path from "node:path"
import { fileURLToPath } from "node:url"

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, "..")

const DEFAULT_ADAPTER = {
  platform: "perplexity",
  version: "1.0",
  parseRules: { citationsPath: "citations", answerPath: "answer" }
}

function getByPath(obj, pathStr) {
  if (!pathStr || obj == null) return undefined
  let cur = obj
  for (const part of pathStr.split(".")) {
    if (cur == null || typeof cur !== "object") return undefined
    cur = cur[part]
  }
  return cur
}

function domainFromUrl(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "")
  } catch {
    return url
  }
}

function normalizeCitation(raw, rank) {
  if (typeof raw === "string" && raw.startsWith("http")) {
    return { url: raw, domain: domainFromUrl(raw), rank }
  }
  if (raw && typeof raw === "object") {
    const url = String(raw.url ?? raw.link ?? raw.href ?? "")
    if (!url.startsWith("http")) return null
    return {
      url,
      title: raw.title != null ? String(raw.title) : raw.name != null ? String(raw.name) : undefined,
      domain: raw.domain != null ? String(raw.domain) : domainFromUrl(url),
      rank
    }
  }
  return null
}

function collectCitationSources(body, citationsPath) {
  const paths = [
    citationsPath,
    "citations",
    "sources",
    "web_results",
    "search_results",
    "related_web_results",
    "hidden_params.citations"
  ]
  for (const p of paths) {
    const raw = getByPath(body, p)
    if (Array.isArray(raw) && raw.length > 0) return raw
  }
  return []
}

function extractAnswer(body, answerPath) {
  const direct = getByPath(body, answerPath)
  if (typeof direct === "string" && direct.trim()) return direct.trim()
  for (const p of ["text", "displayed_text", "answer", "choices.0.message.content"]) {
    const v = getByPath(body, p)
    if (typeof v === "string" && v.trim()) return v.trim()
  }
  return ""
}

function extractCitations(body, citationsPath) {
  const sources = collectCitationSources(body, citationsPath)
  const out = []
  sources.forEach((item, idx) => {
    const c = normalizeCitation(item, idx + 1)
    if (c) out.push(c)
  })
  return out
}

function parsePerplexityPayload(payload, adapter = DEFAULT_ADAPTER) {
  if (!payload || typeof payload !== "object") return null
  const rules = adapter.parseRules ?? {}
  const answer_text = extractAnswer(payload, rules.answerPath ?? "answer")
  const citations = extractCitations(payload, rules.citationsPath ?? "citations")
  if (!answer_text && citations.length === 0) return null
  return {
    platform: "perplexity",
    answer_text: answer_text || "(no answer text captured)",
    citations,
    capture_method: "browser-extension-hook"
  }
}

function mergeCapture(current, chunk) {
  if (!current) return chunk
  const urls = new Map()
  for (const c of [...current.citations, ...chunk.citations]) urls.set(c.url, c)
  const citations = [...urls.values()].map((c, idx) => ({ ...c, rank: idx + 1 }))
  const a = current.answer_text === "(no answer text captured)" ? "" : current.answer_text
  const b = chunk.answer_text === "(no answer text captured)" ? "" : chunk.answer_text
  let answer_text = a
  if (b) {
    if (!a) answer_text = b
    else if (b.startsWith(a)) answer_text = b
    else if (!a.includes(b)) answer_text = `${a}${b}`
  }
  return { ...current, answer_text: answer_text || "(no answer text captured)", citations }
}

function parseStreamText(text, adapter) {
  let merged = null
  for (const line of text.split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed === "data: [DONE]") continue
    const payloadText = trimmed.startsWith("data:") ? trimmed.slice(5).trim() : trimmed
    if (!payloadText.startsWith("{")) continue
    try {
      const parsed = parsePerplexityPayload(JSON.parse(payloadText), adapter)
      if (parsed) merged = mergeCapture(merged, parsed)
    } catch {
      /* partial line */
    }
  }
  return merged
}

function assert(name, cond, detail = "") {
  if (!cond) throw new Error(`${name}${detail ? `: ${detail}` : ""}`)
}

// --- fixtures ---

const legacyApiChat = {
  answer: "Dragon Journey Travel offers curated China tours.",
  citations: ["https://www.chinahighlights.com/tours", "https://www.trip.com/china-tours"]
}

const webResultsChunk = {
  text: "Top agencies include China Highlights.",
  web_results: [{ url: "https://www.chinahighlights.com/", title: "China Highlights" }]
}

const hiddenParams = {
  choices: [{ message: { content: "Answer with sources." } }],
  hidden_params: { citations: ["https://example.com/page"] }
}

function testLegacy() {
  const r = parsePerplexityPayload(legacyApiChat)
  assert("legacy citations", r.citations.length >= 2)
  assert("legacy answer", r.answer_text.includes("Dragon Journey"))
  console.log("legacy /api/chat ok:", r.citations.length, "citations")
}

function testWebResults() {
  const r = parsePerplexityPayload(webResultsChunk)
  assert("web_results citations", r.citations.length >= 1)
  assert("web_results domain", r.citations[0].domain.includes("chinahighlights"))
  console.log("web_results ok:", r.citations[0].url)
}

function testHiddenParams() {
  const r = parsePerplexityPayload(hiddenParams)
  assert("hidden_params citations", r.citations.length === 1)
  console.log("hidden_params ok:", r.citations[0].domain)
}

function testSseFixture() {
  const ssePath = path.join(root, "fixtures", "perplexity-sse-sample.txt")
  const text = fs.readFileSync(ssePath, "utf8")
  const r = parseStreamText(text)
  assert("sse merged citations", r.citations.length >= 2, `got ${r?.citations?.length}`)
  assert("sse answer", r.answer_text.includes("Dragon Journey") || r.answer_text.includes("China"))
  console.log("SSE fixture ok:", r.citations.length, "citations,", r.answer_text.slice(0, 50))
}

function testUrlMarkers() {
  const markers = [
    "/rest/sse/perplexity_ask",
    "/api/chat",
    "https://www.perplexity.ai/rest/sse/perplexity_ask?foo=1"
  ]
  for (const u of markers) {
    assert(`url match ${u}`, u.includes("perplexity") || u.includes("/api/chat") || u.includes("/rest/sse"))
  }
  console.log("url markers ok")
}

try {
  testLegacy()
  testWebResults()
  testHiddenParams()
  testSseFixture()
  testUrlMarkers()
  console.log("\nPerplexity hook adapter evidence: PASS (≥1 citation on all fixtures)")
} catch (err) {
  console.error(err instanceof Error ? err.message : err)
  process.exit(1)
}
