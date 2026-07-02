import type { PlatformAdapterConfig, ProbeRuntimeState } from "~lib/types"
import { ADAPTER_STORAGE_KEY, STORAGE_KEY } from "~lib/api"

export async function readRuntimeState(): Promise<ProbeRuntimeState | null> {
  const stored = await chrome.storage.local.get(STORAGE_KEY)
  return (stored[STORAGE_KEY] as ProbeRuntimeState | undefined) ?? null
}

export async function writeRuntimeState(state: ProbeRuntimeState): Promise<void> {
  await chrome.storage.local.set({ [STORAGE_KEY]: state })
}

export async function patchRuntimeState(
  patch: Partial<ProbeRuntimeState>
): Promise<ProbeRuntimeState> {
  const current = (await readRuntimeState()) ?? {
    nodeKey: "",
    apiBase: "",
    lastPollAt: null,
    lastPollError: null,
    currentTask: null,
    taskStatus: "idle" as const,
    lastTaskMessage: null,
    extensionVersion: "",
    mockMode: false
  }
  const next = { ...current, ...patch }
  await writeRuntimeState(next)
  return next
}

export async function saveAdapters(adapters: PlatformAdapterConfig[]): Promise<void> {
  await chrome.storage.local.set({ [ADAPTER_STORAGE_KEY]: adapters })
}

export async function readAdapters(): Promise<PlatformAdapterConfig[]> {
  const stored = await chrome.storage.local.get(ADAPTER_STORAGE_KEY)
  return (stored[ADAPTER_STORAGE_KEY] as PlatformAdapterConfig[] | undefined) ?? []
}

export function findAdapter(
  adapters: PlatformAdapterConfig[],
  platform: string
): PlatformAdapterConfig | undefined {
  return adapters.find((a) => a.platform === platform)
}
