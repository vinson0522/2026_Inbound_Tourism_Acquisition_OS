# HANDOFF | 技术总监 → 开发（Python / inbound-ai）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-09 | EPIC-5 M1 · FR-402/403 · ADR-20260709-24 |

## 交付请求

- [x] `POST /ai/breakdown/extract-frames` — 输入 `sourceUrl` · 输出 `frames[]`（timestamp · thumbnailUrl · caption）· `BREAKDOWN_MOCK=true` 时固定 6 帧 mock
- [x] `POST /ai/breakdown/analyze` — 输入 frames + 可选 title · 输出七维 `dimensions_json` + `reusable_structure` · `needs_human_review: true`
- [x] Worker 或 sync path：Java MQ 消费 → 调上述两接口 → HTTP callback
- [x] pytest：`tests/test_breakdown.py` — mock 模式 2 用例

## Prompt

```
角色：开发 Python。必读 content generate mock、llm_gateway、ADR-24。
任务：breakdown extract+analyze + pytest。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-09
- **结果摘要**：`POST /ai/breakdown/extract-frames` · `POST /ai/breakdown/analyze` · `BREAKDOWN_MOCK_LLM` · MQ worker `ai.breakdown` → Java callback · `tests/test_breakdown.py` 4 passed
- **遗留**：Admin `/materials` 待 #4；生产拆帧需 ffmpeg/PySceneDetect 后置
