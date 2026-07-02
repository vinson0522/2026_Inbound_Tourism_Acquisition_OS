import {
  defaultRuntimeState,
  fetchAdapters,
  pollTask,
  probeConfig,
  registerNode,
  submitProbeResult
} from "~lib/api"
import {
  findAdapter,
  patchRuntimeState,
  readAdapters,
  readRuntimeState,
  saveAdapters
} from "~lib/storage-state"
import type { BackgroundMessage, ProbePollTask, ProbeRuntimeState } from "~lib/types"
import { mergeAdapter } from "~adapters/perplexity"

const ALARM_NAME = "tourgeo-probe-poll"
const PERPLEXITY_URL = "https://www.perplexity.ai/"

let executingTaskId: number | null = null

async function bootstrap(): Promise<void> {
  await patchRuntimeState(defaultRuntimeState())
  try {
    await registerNode()
    const adapters = await fetchAdapters()
    await saveAdapters(adapters)
    await patchRuntimeState({
      lastTaskMessage: `Registered · ${adapters.length} adapter(s)`
    })
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err)
    await patchRuntimeState({ lastPollError: msg, lastTaskMessage: `Register failed: ${msg}` })
    console.error("[TourGEO Probe] bootstrap failed", err)
  }

  chrome.alarms.create(ALARM_NAME, {
    periodInMinutes: probeConfig.pollIntervalMinutes,
    delayInMinutes: 0.05
  })
}

async function getPerplexityTab(): Promise<chrome.tabs.Tab> {
  const tabs = await chrome.tabs.query({ url: "https://www.perplexity.ai/*" })
  if (tabs[0]?.id != null) {
    return tabs[0]
  }
  const created = await chrome.tabs.create({ url: PERPLEXITY_URL, active: false })
  if (created.id == null) {
    throw new Error("Failed to open Perplexity tab")
  }
  await waitForTabLoad(created.id)
  return created
}

function waitForTabLoad(tabId: number, timeoutMs = 30_000): Promise<void> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      chrome.tabs.onUpdated.removeListener(listener)
      reject(new Error("Perplexity tab load timeout"))
    }, timeoutMs)

    const listener = (updatedId: number, info: chrome.tabs.TabChangeInfo) => {
      if (updatedId === tabId && info.status === "complete") {
        clearTimeout(timer)
        chrome.tabs.onUpdated.removeListener(listener)
        resolve()
      }
    }
    chrome.tabs.onUpdated.addListener(listener)
  })
}

async function runTaskOnTab(task: ProbePollTask): Promise<void> {
  const tab = await getPerplexityTab()
  if (tab.id == null) {
    throw new Error("Perplexity tab has no id")
  }

  const adapters = await readAdapters()
  const remote = findAdapter(adapters, task.platform)
  const adapter = mergeAdapter(remote)

  const response = await chrome.tabs.sendMessage(tab.id, {
    type: "EXECUTE_PROBE",
    task,
    adapter,
    mockMode: probeConfig.mockMode
  })

  if (!response?.ok) {
    throw new Error(response?.error ?? "Content script returned failure")
  }

  await submitProbeResult(task.probeTaskId, "SUCCESS", response.result)
}

async function handlePollCycle(): Promise<void> {
  if (executingTaskId != null) {
    return
  }

  await patchRuntimeState({ taskStatus: "polling", lastPollAt: new Date().toISOString() })

  try {
    const task = await pollTask(probeConfig.platform)
    await patchRuntimeState({ lastPollError: null })

    if (!task?.probeTaskId) {
      await patchRuntimeState({
        taskStatus: "idle",
        currentTask: null,
        lastTaskMessage: "No pending tasks"
      })
      return
    }

    executingTaskId = task.probeTaskId
    await patchRuntimeState({
      taskStatus: "running",
      currentTask: task,
      lastTaskMessage: `Running task #${task.probeTaskId}`
    })

    await runTaskOnTab(task)

    await patchRuntimeState({
      taskStatus: "success",
      currentTask: null,
      lastTaskMessage: `Task #${task.probeTaskId} submitted`
    })
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err)
    const failedTaskId = executingTaskId
    if (failedTaskId != null) {
      try {
        await submitProbeResult(failedTaskId, "FAILED", undefined, msg)
      } catch (submitErr) {
        console.error("[TourGEO Probe] failed to submit FAILED status", submitErr)
      }
    }
    await patchRuntimeState({
      taskStatus: "failed",
      lastPollError: msg,
      lastTaskMessage: msg,
      currentTask: null
    })
    console.error("[TourGEO Probe] poll cycle error", err)
  } finally {
    executingTaskId = null
  }
}

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === ALARM_NAME) {
    void handlePollCycle()
  }
})

chrome.runtime.onMessage.addListener(
  (message: BackgroundMessage, _sender, sendResponse: (state: ProbeRuntimeState | void) => void) => {
    if (message.type === "GET_STATE") {
      void readRuntimeState().then((state) => sendResponse(state ?? defaultRuntimeState()))
      return true
    }
    if (message.type === "FORCE_POLL") {
      void handlePollCycle().then(() => sendResponse())
      return true
    }
    return false
  }
)

chrome.runtime.onInstalled.addListener(() => {
  void bootstrap()
})

chrome.runtime.onStartup.addListener(() => {
  void bootstrap()
})

void bootstrap()

console.info("[TourGEO Probe] background service worker started")
