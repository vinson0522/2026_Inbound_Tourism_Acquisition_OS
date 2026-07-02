# EPIC-3 M2 关键词机会评分 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-06 |
| **优先级** | High |
| **关联** | EPIC-3 · **FR-203** · ADR-20260706-19 |
| **前置** | EPIC-3 M1 ✅ · **C15** EPIC-11 ⏳ |

## 目标（M2 MVP）

**为已有关键词计算机会分 → 写 `score` + `score_detail_json` → Admin 列表展示真实分数 + 排序**

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-203 | Python `/ai/keywords/score` · Java 单条/批量 refresh API | 竞品词频爬虫 · 实时 SERP |
| 评分维度 | 5 维加权（见 ADR-19）· mock/无 Key 可测 | 运营后台改权重 UI |
| Admin | 机会分列真实值 · 按 score 排序 · tooltip 展示分项 | 编辑/合并/导出 · 详情 drawer |
| 联动 | 可选读项目最近 SUCCESS `geo_score` 作竞品强度输入 | generate 后自动评分（→ M2.1 可选） |
| UI 线框 | **无新页**；复用 [keywords-list.md](../../design/wireframes/keywords-list.md) M2 增量说明 | 新 wireframe 文件 |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **0** | **开发** | [→ C15 commit](2026-07-06-tech-director-to-dev-c15-commit.md) | — | push |
| **1** | **开发 Python** | [→ `/ai/keywords/score`](2026-07-06-tech-director-to-dev-ai-epic3-keyword-score.md) | — | pytest |
| **2** | **开发 Java** | [→ score API](2026-07-06-tech-director-to-dev-java-epic3-keyword-score.md) | #1 | smoke |
| **3** | **开发 Admin** | [→ 列表 score](2026-07-06-tech-director-to-dev-admin-epic3-keyword-score.md) | #2 | build |
| — | 总览 | Sprint 索引 | [EPIC-3 M2](2026-07-06-tech-director-epic3-m2-keyword-score-sprint.md) | — |

**无 UI / 无运维 M2**（Admin 按现有线框改列与排序）。

## score_detail_json 契约（M1 固定权重 ADR-19）

```json
{
  "relevance": 82,
  "long_tail_value": 75,
  "producibility": 88,
  "landing_value": 70,
  "competitive_pressure": 65,
  "geo_score_input": 85,
  "weights_version": "keyword_score_v1",
  "computed_at": "2026-07-06T12:00:00Z"
}
```

`score` = 加权平均 0–100，保留 1 位小数。

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **0** | **开发** | [C15 commit+push](2026-07-06-tech-director-to-dev-c15-commit.md) | `角色：开发。必读 docs/agent-team/MEMORY.md。任务：C15 commit+push EPIC-11 探针全栈；更新 MEMORY EPIC-11 正式关闭签核；C15 push 完成前勿启动 EPIC-3。` |
| **1** | **开发 Python** | [→ AI `/ai/keywords/score`](2026-07-06-tech-director-to-dev-ai-epic3-keyword-score.md) | `角色：开发 inbound-ai。必读 ADR-19、keywords_service.py、HANDOFF 2026-07-06-tech-director-to-dev-ai-epic3-keyword-score.md。任务：POST /ai/keywords/score 五维加权 + mock + pytest。` |
| **2** | **开发 Java** | [→ Java score API](2026-07-06-tech-director-to-dev-java-epic3-keyword-score.md) | `角色：开发 Java。必读 KeywordOpportunityServiceImpl、AiServiceClient、ADR-19、HANDOFF 2026-07-06-tech-director-to-dev-java-epic3-keyword-score.md。任务：单条+批量 score API · Feign · test_keywords_score.py。` |
| **3** | **开发 Admin** | [→ Admin 关键词 score](2026-07-06-tech-director-to-dev-admin-epic3-keyword-score.md) | `角色：开发 Admin。必读 keywords/index.vue、keywords-list.md、HANDOFF 2026-07-06-tech-director-to-dev-admin-epic3-keyword-score.md。任务：刷新评分按钮 · 真实机会分列 · 默认 score DESC · build:prod。` |

**并行**：#0 必须先完成。#1 与 #2 串行（Java 依赖 Python）。#3 等 Java smoke 通过。

## 完成后

- smoke：`deploy/scripts/test_keywords_score.py`
- commit **C16**：`feat(ai,core,admin): EPIC-3 M2 keyword opportunity scoring FR-203`
- 下一 Sprint 候选：EPIC-9 M2 计费升级 · EPIC-11 M2 adapter/校准 · EPIC-7 M2 CRM
