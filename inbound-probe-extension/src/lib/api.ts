import { PROBE_PLATFORMS } from "~lib/platforms"
import type {
  ApiResponse,
  PlatformAdapterConfig,
  ProbeCaptureResult,
  ProbePollTask,
  ProbeRuntimeState
} from "~lib/types"

const API_BASE = process.env.PLASMO_PUBLIC_API_BASE ?? "http://localhost:8080"
const NODE_KEY = process.env.PLASMO_PUBLIC_NODE_KEY ?? "demo-probe-1"
const CLIENT_ID = process.env.PLASMO_PUBLIC_CLIENT_ID ?? ""
const EXTENSION_VERSION = "0.1.0"

export const probeConfig = {
  apiBase: API_BASE.replace(/\/$/, ""),
  nodeKey: NODE_KEY,
  clientId: CLIENT_ID,
  extensionVersion: EXTENSION_VERSION,
  pollIntervalMinutes: 0.5,
  platforms: [...PROBE_PLATFORMS],
  mockMode: process.env.PLASMO_PUBLIC_PROBE_MOCK === "true"
}

function probeHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    "X-Probe-Node-Key": probeConfig.nodeKey
  }
  if (probeConfig.clientId) {
    headers.clientid = probeConfig.clientId
  }
  return headers
}

async function parseJson<T>(res: Response): Promise<ApiResponse<T>> {
  const text = await res.text()
  if (!text) {
    throw new Error(`HTTP ${res.status}: empty body`)
  }
  let payload: ApiResponse<T>
  try {
    payload = JSON.parse(text) as ApiResponse<T>
  } catch {
    throw new Error(`HTTP ${res.status}: invalid JSON`)
  }
  if (!res.ok || (payload.code !== 200 && payload.code !== 0)) {
    throw new Error(payload.msg ?? payload.message ?? `API error code=${payload.code}`)
  }
  return payload
}

export async function registerNode(region = "us-east"): Promise<number> {
  const res = await fetch(`${probeConfig.apiBase}/api/v1/probe/nodes/register`, {
    method: "POST",
    headers: probeHeaders(),
    body: JSON.stringify({
      nodeKey: probeConfig.nodeKey,
      region,
      platforms: probeConfig.platforms,
      extensionVersion: probeConfig.extensionVersion
    })
  })
  const payload = await parseJson<number>(res)
  return payload.data
}

export async function fetchAdapters(): Promise<PlatformAdapterConfig[]> {
  const res = await fetch(`${probeConfig.apiBase}/api/v1/probe/adapters`, {
    method: "GET",
    headers: probeHeaders()
  })
  const payload = await parseJson<PlatformAdapterConfig[]>(res)
  return payload.data ?? []
}

export async function pollTask(platform: string): Promise<ProbePollTask | null> {
  const url = `${probeConfig.apiBase}/api/v1/probe/tasks/poll?platform=${encodeURIComponent(platform)}`
  const res = await fetch(url, { method: "GET", headers: probeHeaders() })
  const payload = await parseJson<ProbePollTask | null>(res)
  return payload.data ?? null
}

export async function submitProbeResult(
  probeTaskId: number,
  status: "SUCCESS" | "FAILED",
  result?: ProbeCaptureResult,
  errorMessage?: string
): Promise<void> {
  const res = await fetch(
    `${probeConfig.apiBase}/api/v1/probe/tasks/${probeTaskId}/result`,
    {
      method: "POST",
      headers: probeHeaders(),
      body: JSON.stringify({
        status,
        result: result ?? undefined,
        errorMessage: errorMessage ?? undefined
      })
    }
  )
  await parseJson<null>(res)
}

export function defaultRuntimeState(): ProbeRuntimeState {
  return {
    nodeKey: probeConfig.nodeKey,
    apiBase: probeConfig.apiBase,
    lastPollAt: null,
    lastPollError: null,
    currentTask: null,
    taskStatus: "idle",
    lastTaskMessage: null,
    extensionVersion: probeConfig.extensionVersion,
    mockMode: probeConfig.mockMode
  }
}

export const STORAGE_KEY = "tourgeo_probe_runtime_state"
export const ADAPTER_STORAGE_KEY = "tourgeo_probe_adapters"
