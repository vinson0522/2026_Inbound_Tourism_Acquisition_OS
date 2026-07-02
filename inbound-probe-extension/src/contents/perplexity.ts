import type { PlasmoCSConfig } from "plasmo"

import {
  buildMockCapture,
  isCaptureReady,
  mergeAdapter,
  mergePerplexityCapture,
  parsePerplexityPayload,
  parsePerplexityStreamText,
  urlMatchesChatApi
} from "~adapters/perplexity"
import type {
  ContentMessage,
  ContentResponse,
  PlatformAdapterConfig,
  ProbeCaptureResult,
  ProbePollTask
} from "~lib/types"

export const config: PlasmoCSConfig = {
  matches: ["https://www.perplexity.ai/*"],
  run_at: "document_start",
  all_frames: false
}

const PERPLEXITY_HOME = "https://www.perplexity.ai/"

function randomDelayMs(min = 200, max = 800): Promise<void> {
  const ms = min + Math.floor(Math.random() * (max - min + 1))
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function tryParsePayload(raw: string): unknown {
  try {
    return JSON.parse(raw) as unknown
  } catch {
    return null
  }
}

function waitForElement(selector: string, timeoutMs: number): Promise<Element> {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(selector)
    if (existing) {
      resolve(existing)
      return
    }
    const observer = new MutationObserver(() => {
      const el = document.querySelector(selector)
      if (el) {
        observer.disconnect()
        clearTimeout(timer)
        resolve(el)
      }
    })
    observer.observe(document.documentElement, { childList: true, subtree: true })
    const timer = window.setTimeout(() => {
      observer.disconnect()
      reject(new Error(`Element not found: ${selector}`))
    }, timeoutMs)
  })
}

function queryFirstSelector(selectors: string): HTMLElement | null {
  for (const sel of selectors.split(",").map((s) => s.trim())) {
    if (!sel) {
      continue
    }
    const el = document.querySelector<HTMLElement>(sel)
    if (el) {
      return el
    }
  }
  return null
}

function setNativeValue(el: HTMLTextAreaElement | HTMLInputElement, value: string): void {
  const proto =
    el instanceof HTMLTextAreaElement
      ? HTMLTextAreaElement.prototype
      : HTMLInputElement.prototype
  const setter = Object.getOwnPropertyDescriptor(proto, "value")?.set
  setter?.call(el, value)
  el.dispatchEvent(new Event("input", { bubbles: true }))
  el.dispatchEvent(new Event("change", { bubbles: true }))
}

function installCaptureHooks(
  adapter: PlatformAdapterConfig,
  timeoutMs: number
): Promise<ProbeCaptureResult> {
  let capture: ProbeCaptureResult | null = null
  let settled = false

  return new Promise((resolve, reject) => {
    const finish = (result: ProbeCaptureResult | null, err?: string) => {
      if (settled) {
        return
      }
      settled = true
      cleanup()
      if (result) {
        resolve(result)
      } else {
        reject(new Error(err ?? "Failed to capture Perplexity response"))
      }
    }

    const onPayload = (payload: unknown) => {
      const parsed = parsePerplexityPayload(payload, adapter)
      if (!parsed) {
        return
      }
      capture = mergePerplexityCapture(capture, parsed)
      if (isCaptureReady(capture)) {
        finish(capture)
      } else if (capture.citations.length > 0 && capture.answer_text.length > 5) {
        finish(capture)
      }
    }

    const originalFetch = window.fetch.bind(window)
    const originalXhrOpen = XMLHttpRequest.prototype.open
    const originalXhrSend = XMLHttpRequest.prototype.send

    window.fetch = async (...args) => {
      const response = await originalFetch(...args)
      const url = String(args[0] instanceof Request ? args[0].url : args[0])
      if (urlMatchesChatApi(url, adapter)) {
        const contentType = response.headers.get("content-type") ?? ""
        void response
          .clone()
          .text()
          .then((text) => {
            if (contentType.includes("event-stream") || contentType.includes("ndjson")) {
              parsePerplexityStreamText(text, adapter, (chunk) => onPayload(chunk.raw_response_json))
            } else {
              const payload = tryParsePayload(text)
              if (payload) {
                onPayload(payload)
              }
            }
          })
          .catch(() => undefined)
      }
      return response
    }

    XMLHttpRequest.prototype.open = function (
      method: string,
      url: string | URL,
      ...rest: [boolean?, string?, string?]
    ) {
      ;(this as XMLHttpRequest & { __probeUrl?: string }).__probeUrl = String(url)
      return originalXhrOpen.call(this, method, url, ...rest)
    }

    XMLHttpRequest.prototype.send = function (...sendArgs: Parameters<XMLHttpRequest["send"]>) {
      this.addEventListener("load", () => {
        const url = (this as XMLHttpRequest & { __probeUrl?: string }).__probeUrl ?? ""
        if (!urlMatchesChatApi(url, adapter)) {
          return
        }
        const ct = this.getResponseHeader("content-type") ?? ""
        const text = typeof this.responseText === "string" ? this.responseText : ""
        if (!text) {
          return
        }
        if (ct.includes("event-stream") || ct.includes("ndjson")) {
          parsePerplexityStreamText(text, adapter, (chunk) => onPayload(chunk.raw_response_json))
        } else {
          const payload = tryParsePayload(text)
          if (payload) {
            onPayload(payload)
          }
        }
      })
      return originalXhrSend.apply(this, sendArgs)
    }

    const OriginalEventSource = window.EventSource
    class PatchedEventSource extends OriginalEventSource {
      constructor(url: string | URL, init?: EventSourceInit) {
        super(url, init)
        const urlStr = String(url)
        if (!urlMatchesChatApi(urlStr, adapter)) {
          return
        }
        this.addEventListener("message", (ev) => {
          const payload = tryParsePayload(String(ev.data))
          if (payload) {
            onPayload(payload)
          }
        })
      }
    }
    window.EventSource = PatchedEventSource as typeof EventSource

    const timer = window.setTimeout(() => {
      if (capture && capture.citations.length > 0) {
        finish(capture)
        return
      }
      finish(null, `Capture timeout after ${timeoutMs}ms — no citations from fetch/SSE hook`)
    }, timeoutMs)

    const cleanup = () => {
      window.clearTimeout(timer)
      window.fetch = originalFetch
      XMLHttpRequest.prototype.open = originalXhrOpen
      XMLHttpRequest.prototype.send = originalXhrSend
      window.EventSource = OriginalEventSource
    }
  })
}

async function submitQuestion(question: string, adapter: PlatformAdapterConfig): Promise<void> {
  const inputSel = adapter.domSelectors?.input ?? "textarea"
  const submitSel = adapter.domSelectors?.submit ?? "button[type=submit]"

  await randomDelayMs()
  const input = queryFirstSelector(inputSel) ?? (await waitForElement(inputSel.split(",")[0].trim(), 30_000))
  if (!(input instanceof HTMLTextAreaElement) && !input.isContentEditable) {
    throw new Error(`Unsupported Perplexity input: ${inputSel}`)
  }

  input.focus()
  await randomDelayMs()

  if (input instanceof HTMLTextAreaElement) {
    setNativeValue(input, question)
  } else {
    input.textContent = question
    input.dispatchEvent(new Event("input", { bubbles: true }))
  }

  await randomDelayMs()

  const submit = queryFirstSelector(submitSel)
  if (submit && !(submit as HTMLButtonElement).disabled) {
    submit.click()
    return
  }

  input.dispatchEvent(
    new KeyboardEvent("keydown", { key: "Enter", code: "Enter", bubbles: true })
  )
}

async function executeProbe(
  task: ProbePollTask,
  adapter: PlatformAdapterConfig,
  mockMode: boolean
): Promise<ProbeCaptureResult> {
  if (mockMode) {
    await randomDelayMs(300, 600)
    return buildMockCapture(task.question)
  }

  if (!window.location.href.startsWith(PERPLEXITY_HOME)) {
    window.location.href = PERPLEXITY_HOME
    await new Promise((r) => window.setTimeout(r, 2500))
  }

  const capturePromise = installCaptureHooks(adapter, 120_000)
  await submitQuestion(task.question, adapter)
  return capturePromise
}

chrome.runtime.onMessage.addListener(
  (message: ContentMessage, _sender, sendResponse: (resp: ContentResponse) => void) => {
    if (message.type === "PING") {
      sendResponse({ ok: true, result: buildMockCapture("") })
      return false
    }

    if (message.type === "EXECUTE_PROBE") {
      const adapter = mergeAdapter(message.adapter)
      executeProbe(message.task, adapter, message.mockMode)
        .then((result) => sendResponse({ ok: true, result }))
        .catch((err: unknown) =>
          sendResponse({
            ok: false,
            error: err instanceof Error ? err.message : String(err)
          })
        )
      return true
    }

    return false
  }
)

console.info("[TourGEO Probe] perplexity content script ready")
