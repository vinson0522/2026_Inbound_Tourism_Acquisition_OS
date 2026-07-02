import { useEffect, useState } from "react"

import type { BackgroundMessage, ProbeRuntimeState } from "~lib/types"
import { defaultRuntimeState } from "~lib/api"

function formatTime(iso: string | null): string {
  if (!iso) {
    return "—"
  }
  try {
    return new Date(iso).toLocaleString()
  } catch {
    return iso
  }
}

function statusLabel(status: ProbeRuntimeState["taskStatus"]): string {
  switch (status) {
    case "polling":
      return "Polling…"
    case "running":
      return "Running task"
    case "success":
      return "Last task OK"
    case "failed":
      return "Last task failed"
    default:
      return "Idle"
  }
}

function IndexPopup() {
  const [state, setState] = useState<ProbeRuntimeState>(defaultRuntimeState())

  const refresh = () => {
    const msg: BackgroundMessage = { type: "GET_STATE" }
    chrome.runtime.sendMessage(msg, (resp: ProbeRuntimeState) => {
      if (resp) {
        setState(resp)
      }
    })
  }

  useEffect(() => {
    refresh()
    const id = window.setInterval(refresh, 2000)
    return () => window.clearInterval(id)
  }, [])

  const forcePoll = () => {
    const msg: BackgroundMessage = { type: "FORCE_POLL" }
    chrome.runtime.sendMessage(msg, () => refresh())
  }

  return (
    <div style={styles.root}>
      <header style={styles.header}>
        <strong>TourGEO Probe</strong>
        <span style={styles.badge}>M2</span>
      </header>

      <dl style={styles.dl}>
        <dt>Node key</dt>
        <dd>{state.nodeKey || "—"}</dd>
        <dt>API</dt>
        <dd style={styles.mono}>{state.apiBase || "—"}</dd>
        <dt>Last poll</dt>
        <dd>{formatTime(state.lastPollAt)}</dd>
        <dt>Status</dt>
        <dd>{statusLabel(state.taskStatus)}</dd>
        <dt>Mock mode</dt>
        <dd>{state.mockMode ? "ON" : "OFF"}</dd>
      </dl>

      {state.currentTask ? (
        <section style={styles.taskBox}>
          <div style={styles.taskTitle}>Current task #{state.currentTask.probeTaskId}</div>
          <div style={styles.taskQuestion}>{state.currentTask.question}</div>
        </section>
      ) : null}

      {state.lastTaskMessage ? <p style={styles.note}>{state.lastTaskMessage}</p> : null}
      {state.lastPollError ? <p style={styles.error}>{state.lastPollError}</p> : null}

      <button type="button" style={styles.button} onClick={forcePoll}>
        Poll now
      </button>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  root: {
    width: 320,
    padding: 12,
    fontFamily: "system-ui, sans-serif",
    fontSize: 13,
    color: "#1a1a1a"
  },
  header: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    marginBottom: 12
  },
  badge: {
    fontSize: 10,
    padding: "2px 6px",
    borderRadius: 4,
    background: "#eef2ff",
    color: "#4338ca"
  },
  dl: {
    display: "grid",
    gridTemplateColumns: "90px 1fr",
    gap: "6px 8px",
    margin: "0 0 12px"
  },
  mono: {
    fontFamily: "ui-monospace, monospace",
    fontSize: 11,
    wordBreak: "break-all"
  },
  taskBox: {
    background: "#f8fafc",
    border: "1px solid #e2e8f0",
    borderRadius: 6,
    padding: 8,
    marginBottom: 8
  },
  taskTitle: {
    fontWeight: 600,
    marginBottom: 4
  },
  taskQuestion: {
    fontSize: 12,
    color: "#475569",
    lineHeight: 1.4
  },
  note: {
    margin: "0 0 8px",
    color: "#64748b",
    fontSize: 12
  },
  error: {
    margin: "0 0 8px",
    color: "#b91c1c",
    fontSize: 12
  },
  button: {
    width: "100%",
    padding: "8px 12px",
    border: "1px solid #cbd5e1",
    borderRadius: 6,
    background: "#fff",
    cursor: "pointer",
    fontWeight: 500
  }
}

export default IndexPopup
