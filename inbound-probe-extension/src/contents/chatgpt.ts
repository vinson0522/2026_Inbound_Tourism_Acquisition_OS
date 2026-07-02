import type { PlasmoCSConfig } from "plasmo"

import {
  buildMockCapture,
  mergeAdapter,
  parseChatgptPayload,
  urlMatchesChatApi
} from "~adapters/chatgpt"
import type {
  ContentMessage,
  ContentResponse,
  PlatformAdapterConfig,
  ProbeCaptureResult,
  ProbePollTask
} from "~lib/types"

export const config: PlasmoCSConfig = {
  matches: ["https://chatgpt.com/*"],
  run_at: "document_start",
  all_frames: false
}

const CHATGPT_HOME = "https://chatgpt.com/"

function randomDelayMs(min = 200, max = 800): Promise<void> {
  const ms = min + Math.floor(Math.random() * (max - min + 1))
  return new Promise((resolve) => setTimeout(resolve, ms))
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

async function submitQuestion(question: string, adapter: PlatformAdapterConfig): Promise<void> {
  const selectors = adapter.domSelectors ?? {}
  const inputSelector = selectors.input ?? "#prompt-textarea"
  const submitSelector = selectors.submit ?? "button[data-testid=send-button]"

  await randomDelayMs()
  const input = (await waitForElement(inputSelector, 30_000)) as HTMLTextAreaElement
  input.focus()
  await randomDelayMs()
  setNativeValue(input, question)
  await randomDelayMs()

  const submit = document.querySelector(submitSelector) as HTMLButtonElement | null
  if (submit && !submit.disabled) {
    submit.click()
    return
  }

  input.dispatchEvent(
    new KeyboardEvent("keydown", { key: "Enter", code: "Enter", bubbles: true, ctrlKey: true })
  )
}

function tryParsePayload(raw: string): unknown {
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function installCaptureHooks(
  adapter: PlatformAdapterConfig,
  timeoutMs: number
): Promise<ProbeCaptureResult> {
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
        reject(new Error(err ?? "Failed to capture ChatGPT response"))
      }
    }

    const onPayload = (payload: unknown) => {
      const parsed = parseChatgptPayload(payload, adapter)
      if (parsed?.answer_text && parsed.answer_text !== "(no answer text captured)") {
        finish(parsed)
      } else if (parsed && parsed.citations.length > 0) {
        finish(parsed)
      }
    }

    const originalFetch = window.fetch.bind(window)
    const patchedFetch: typeof window.fetch = async (...args) => {
      const response = await originalFetch(...args)
      try {
        const req = args[0]
        const url = typeof req === "string" ? req : req instanceof Request ? req.url : String(req)
        if (urlMatchesChatApi(url, adapter)) {
          const clone = response.clone()
          void clone
            .json()
            .then(onPayload)
            .catch(() =>
              clone.text().then((text) => {
                const parsed = tryParsePayload(text)
                if (parsed) {
                  onPayload(parsed)
                }
              })
            )
        }
      } catch {
        /* non-fatal */
      }
      return response
    }
    window.fetch = patchedFetch

    const OriginalEventSource = window.EventSource
    class PatchedEventSource extends OriginalEventSource {
      constructor(url: string | URL, init?: EventSourceInit) {
        super(url, init)
        this.addEventListener("message", (ev) => {
          const parsed = tryParsePayload(String(ev.data))
          if (parsed) {
            onPayload(parsed)
          }
        })
      }
    }
    window.EventSource = PatchedEventSource as typeof EventSource

    const timer = window.setTimeout(() => {
      finish(null, `Capture timeout after ${timeoutMs}ms`)
    }, timeoutMs)

    const cleanup = () => {
      window.clearTimeout(timer)
      window.fetch = originalFetch
      window.EventSource = OriginalEventSource
    }
  })
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

  if (!window.location.href.startsWith(CHATGPT_HOME)) {
    window.location.href = CHATGPT_HOME
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

console.info("[TourGEO Probe] chatgpt content script ready")
