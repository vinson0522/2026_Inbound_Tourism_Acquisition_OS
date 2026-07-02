# EPIC-5 M1 爆款拆解 Sprint | 总览（技术总监）

| 字段 | 值 |
|------|-----|
| **日期** | 2026-07-09 |
| **优先级** | High |
| **关联** | EPIC-5 · **FR-401~403** · ADR-20260709-24 |
| **前置** | EPIC-9 M2 C20 待入库 · C19 `cf42562` |

## 商业决策（完整版路线图 #5）

内容 Agent 需「借鉴结构而非搬运」——M1 打通 **素材上传 → 拆帧 → 七维拆解表** 最小闭环，为 EPIC-4 脚本生成提供参考结构。

## 目标（M1 MVP）

| 范围 | M1 做 | M1 不做 |
|------|-------|---------|
| FR-401 | Admin 上传视频/图片 → MinIO · `material_asset` 记录 | 批量导入 · 外链抓取 |
| FR-402 | Python `/ai/breakdown/extract-frames` — 固定间隔抽帧（ffmpeg）或 **mock frames** | PySceneDetect 智能切镜 · VLM 逐帧描述 |
| FR-403 | `/ai/breakdown/analyze` — 七维 JSON（theme/hook/shot/subtitle/emotion/psychology/reusable）· `needs_human_review` | 素材标签库 FR-404 · 智能推荐 FR-406 |
| Java | `POST .../materials` upload · `GET` list · `POST .../breakdowns` 触发 MQ · `GET` 详情 | 版权导出拦截 FR-405（M2 alert only） |
| Admin | `/materials` 列表 · 上传 · 「开始拆解」· 详情 drawer 七维表 + frames 缩略图 | 素材标签 CRUD |

## 任务拆分

| # | 角色 | HANDOFF | 依赖 | 验收 |
|---|------|---------|------|------|
| **1** | **UI 设计** | [→ 线框](2026-07-09-tech-director-to-ui-epic5-viral-breakdown.md) | — | `viral-breakdown-list.md` |
| **2** | **开发 Java** | [→ material API](2026-07-09-tech-director-to-dev-java-epic5-viral.md) | — | smoke |
| **3** | **开发 Python** | [→ breakdown AI](2026-07-09-tech-director-to-dev-ai-epic5-breakdown.md) | — | pytest |
| **4** | **开发 Admin** | [→ material UI](2026-07-09-tech-director-to-dev-admin-epic5-viral.md) | #1+#2+#3 | build |

## 窗口派发（复制到各 Cursor 窗口）

| # | 窗口 | HANDOFF | 激活 Prompt（Custom Instructions 首行） |
|---|------|---------|----------------------------------------|
| **1** | **UI 设计** | [→ 线框](2026-07-09-tech-director-to-ui-epic5-viral-breakdown.md) | `角色：UI 设计。必读 PRD §8.6 FR-401~403、content-task-list 线框、001_schema material_asset/video_breakdown、ADR-24、HANDOFF 2026-07-09-tech-director-to-ui-epic5-viral-breakdown.md。任务：viral-breakdown-list.md 上传+拆解+七维详情 drawer + UI→开发 HANDOFF。` |
| **2** | **开发 Java** | [→ Java API](2026-07-09-tech-director-to-dev-java-epic5-viral.md) | `角色：开发 Java。必读 material_asset、video_breakdown DDL、MinIO 上传模式（knowledge_asset）、ContentTask 模式、ADR-24、HANDOFF 2026-07-09-tech-director-to-dev-java-epic5-viral.md。任务：material upload/list + breakdown trigger/callback + test_material_breakdown.py。` |
| **3** | **开发 Python** | [→ AI breakdown](2026-07-09-tech-director-to-dev-ai-epic5-breakdown.md) | `角色：开发 Python。必读 inbound-ai routers、content generate mock 模式、ADR-24、HANDOFF 2026-07-09-tech-director-to-dev-ai-epic5-breakdown.md。任务：/ai/breakdown/extract-frames + /ai/breakdown/analyze + BREAKDOWN_MOCK_LLM + pytest。` |
| **4** | **开发 Admin** | [→ Admin UI](2026-07-09-tech-director-to-dev-admin-epic5-viral.md) | `角色：开发 Admin。必读 content-tasks 列表模式、HANDOFF 2026-07-09-tech-director-to-dev-admin-epic5-viral.md。任务：/materials 页 + 拆解详情 drawer · build:prod。` |

**并行**：#1 #2 #3 可并行；#4 依赖 API。

## 完成后

- smoke：`deploy/scripts/test_material_breakdown.py`
- commit **C21**：`feat(core,ai,admin): EPIC-5 M1 viral video breakdown MVP`
- **技术总监签核（2026-07-09）**：✅ smoke materialId=2 · pytest 4 · Admin build · **C21 待入库**
- 下一 Sprint：**EPIC-7 M3** WhatsApp+AI跟进（路线图 #6）
