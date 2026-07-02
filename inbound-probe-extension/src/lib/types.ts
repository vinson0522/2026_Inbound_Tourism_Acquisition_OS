export interface ApiResponse<T> {
  code: number
  msg?: string
  message?: string
  data: T
}

export interface ProbePollTask {
  probeTaskId: number
  runId: number
  questionId: number
  question: string
  platform: string
  probeMode: string
  region?: string
  locale?: string
  market?: string
}

export interface PlatformAdapterConfig {
  platform: string
  version: string
  domSelectors?: Record<string, string>
  apiPatterns?: Record<string, string>
  parseRules?: Record<string, string>
}

export interface ProbeCitation {
  url: string
  title?: string
  domain?: string
  rank?: number
}

export interface ProbeCaptureResult {
  probe_mode: string
  platform: string
  answer_text: string
  citations: ProbeCitation[]
  mentioned_brands?: string[]
  raw_response_json?: Record<string, unknown>
  capture_method?: string
}

export type ProbeTaskStatus = "idle" | "polling" | "running" | "success" | "failed"

export interface ProbeRuntimeState {
  nodeKey: string
  apiBase: string
  lastPollAt: string | null
  lastPollError: string | null
  currentTask: ProbePollTask | null
  taskStatus: ProbeTaskStatus
  lastTaskMessage: string | null
  extensionVersion: string
  mockMode: boolean
}

export type BackgroundMessage =
  | { type: "GET_STATE" }
  | { type: "FORCE_POLL" }
  | { type: "PROBE_TASK_RESULT"; taskId: number; ok: boolean; error?: string }

export type ContentMessage =
  | {
      type: "EXECUTE_PROBE"
      task: ProbePollTask
      adapter: PlatformAdapterConfig
      mockMode: boolean
    }
  | { type: "PING" }

export type ContentResponse =
  | { ok: true; result: ProbeCaptureResult }
  | { ok: false; error: string }
