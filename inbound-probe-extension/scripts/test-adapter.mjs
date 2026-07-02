#!/usr/bin/env node
/** Sanity check aligned with adapter parse rules (perplexity + chatgpt). */

function getByPath(obj, path) {
  if (!path || obj == null) return undefined
  let cur = obj
  for (const part of path.split(".")) {
    if (cur == null || typeof cur !== "object") return undefined
    cur = cur[part]
  }
  return cur
}

function partsToText(parts) {
  if (typeof parts === "string" && parts.trim()) return parts.trim()
  if (!Array.isArray(parts)) return ""
  return parts
    .map((p) => (typeof p === "string" ? p : ""))
    .join("")
    .trim()
}

function testPerplexity() {
  const sample = {
    answer: "Dragon Journey Travel offers curated China tours.",
    citations: [{ url: "https://example.com/tour", title: "Tours", domain: "example.com" }]
  }
  const answer = getByPath(sample, "answer")
  const citations = getByPath(sample, "citations")
  if (typeof answer !== "string" || !answer.includes("Dragon Journey")) {
    throw new Error("perplexity: answer parse failed")
  }
  if (!Array.isArray(citations) || citations.length === 0) {
    throw new Error("perplexity: citations parse failed")
  }
  console.log("perplexity ok:", answer.slice(0, 40), "citations=", citations.length)
}

function testChatgpt() {
  const sample = {
    message: {
      content: { parts: ["Dragon Journey Travel offers private China tours via ChatGPT browse."] },
      metadata: {
        citations: [{ url: "https://example.com/chatgpt", title: "Tours", domain: "example.com" }]
      }
    }
  }
  const answer = partsToText(getByPath(sample, "message.content.parts"))
  const citations = getByPath(sample, "message.metadata.citations")
  if (!answer.includes("Dragon Journey")) {
    throw new Error("chatgpt: answer parse failed")
  }
  if (!Array.isArray(citations) || citations.length === 0) {
    throw new Error("chatgpt: citations parse failed")
  }
  console.log("chatgpt ok:", answer.slice(0, 40), "citations=", citations.length)
}

try {
  testPerplexity()
  testChatgpt()
} catch (err) {
  console.error(err instanceof Error ? err.message : err)
  process.exit(1)
}
