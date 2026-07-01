# EPIC-3 M2 关键词机会评分 Sprint | 总览（可选并行）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-06-29 |
| **优先级** | Medium（**不阻塞 EPIC-4**） |
| **关联** | EPIC-3 · **FR-203** |
| **前置** | EPIC-3 M1 ✅ · EPIC-2 GEO 分数（可选输入） |

## 目标（M2）

**为已有关键词计算/刷新机会分 → 更新 `keyword_opportunity.score` + `score_detail_json` → Admin 列表展示真实分数**

| 范围 | M2 做 | M2 不做 |
|------|-------|---------|
| FR-203 | Python `/ai/keywords/score` 或 generate 后评分；Java `POST .../keywords/{id}/score` | 竞品词频爬虫 |
| Admin | 机会分列真实值 + 排序 | 编辑/合并/导出 |
| 联动 | 可选读最近 `geo_score` 作 score_detail 输入 | 自动转内容任务 |

## 任务拆分

| # | 角色 | 交付 | 验收 |
|---|------|------|------|
| 1 | 技术总监 | ADR 定评分权重来源（PRD §9 或配置表） | DECISIONS 条目 |
| 2 | 开发 Python | `/ai/keywords/score` | pytest |
| 3 | 开发 Java | score API + 批量 refresh | smoke |
| 4 | 开发 Admin | 列表 score 列 + 排序 | 截图 |

**启动条件**：EPIC-4 M1 启动后资源允许时并行；无 UI 线框依赖。

## 窗口激活 Prompt 摘要

```
角色：开发 inbound-ai。任务：EPIC-3 M2 FR-203 POST /ai/keywords/score；读 PRD 机会分字段；mock 可测。
```

---

> 详细子 HANDOFF 待 M2 正式启动时由技术总监拆分（与 EPIC-4 不冲突）。
