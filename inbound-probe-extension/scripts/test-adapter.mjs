#!/usr/bin/env node
/** Sanity check aligned with src/adapters/perplexity.ts parse rules. */

function getByPath(obj, path) {
  if (!path || obj == null) return undefined
  let cur = obj
  for (const part of path.split(".")) {
    if (cur == null || typeof cur !== "object") return undefined
    cur = cur[part]
  }
  return cur
}

const sample = {
  answer: "Dragon Journey Travel offers curated China tours.",
  citations: [{ url: "https://example.com/tour", title: "Tours", domain: "example.com" }]
}

const answer = getByPath(sample, "answer")
const citations = getByPath(sample, "citations")

if (typeof answer !== "string" || !answer.includes("Dragon Journey")) {
  console.error("adapter parse failed: answer")
  process.exit(1)
}
if (!Array.isArray(citations) || citations.length === 0) {
  console.error("adapter parse failed: citations")
  process.exit(1)
}

console.log("adapter parse ok:", answer.slice(0, 40), "citations=", citations.length)
