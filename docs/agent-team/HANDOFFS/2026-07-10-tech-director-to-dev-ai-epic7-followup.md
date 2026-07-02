# HANDOFF | 技术总监 → 开发（Python / inbound-ai）

| From | To | 日期 | 关联 |
|------|-----|------|------|
| 技术总监 | 开发 | 2026-07-10 | EPIC-7 M3 · FR-603 · ADR-20260710-25 |

## 交付请求

- [x] `POST /ai/followup/generate` — 输入 lead 摘要（name · message · budget · travelDate · source · keyword）· locale en/zh · 输出跟进话术
- [x] `FOLLOWUP_MOCK_LLM=true` 默认 mock · template `lead_followup_v1`
- [x] `needs_human_review: true` · 禁止承诺价格/签证
- [x] pytest：`tests/test_followup.py` — mock 2 用例（en + zh）

## Prompt

```
角色：开发 Python。必读 content generate、template_service、ADR-25。
任务：/ai/followup/generate + pytest。
```

## Done（由 To 角色填写）

- **完成时间**：2026-07-02
- **结果摘要**：
  - `POST /ai/followup/generate` · 输入 name/message/budget/travelDate/source/keyword · 输出 `suggestionEn` + `suggestionZh`
  - `FOLLOWUP_MOCK_LLM=true` 默认 · `template_service.load_followup_generate_prompt` · `lead_followup_v1`
  - 真实 LLM 路径（无 key 回退 mock）· 合规校验禁止价格/签证承诺 · `needs_human_review: true`
  - `tests/test_followup.py` **7 passed**（mock en/zh · LLM parse · token · compliance）
- **遗留**：生产环境 seed `lead_followup_v1` template 到 DB（当前 fallback prompt 可用）
