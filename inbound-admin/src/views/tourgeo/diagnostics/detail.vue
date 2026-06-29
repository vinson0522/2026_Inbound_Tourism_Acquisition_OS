<template>
  <div v-loading="pageLoading" class="p-2 tg-diagnostic-detail">
    <el-page-header @back="goBack">
      <template #content>
        <span class="breadcrumb-text">GEO 诊断 / 诊断任务 / {{ run?.name || '…' }}</span>
      </template>
    </el-page-header>

    <el-result v-if="loadError === '404'" icon="warning" title="诊断任务不存在">
      <template #extra>
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </template>
    </el-result>

    <template v-else-if="run">
      <!-- 任务头 -->
      <el-card shadow="hover" class="header-card">
        <div class="header-row">
          <div class="header-main">
            <h1 class="run-title">{{ run.name }}</h1>
            <diagnostic-status-tag :status="run.status" />
          </div>
          <div class="header-score">
            <span v-if="showScore" class="score-label">GEO</span>
            <geo-score-display v-if="showScore" :score="run.geoScore" class="score-value" />
            <span v-else class="score-placeholder">—</span>
          </div>
        </div>

        <el-descriptions :column="3" size="small" class="run-meta">
          <el-descriptions-item label="市场">{{ run.market }}</el-descriptions-item>
          <el-descriptions-item label="语言">{{ run.locale }}</el-descriptions-item>
          <el-descriptions-item label="Region">{{ run.region || '—' }}</el-descriptions-item>
          <el-descriptions-item label="探针">
            <el-tag v-for="m in run.probeModes" :key="m" size="small" type="success" class="mr-1">{{ m }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="AI 平台">
            <el-tag v-for="m in run.models" :key="m" size="small" class="mr-1">{{ m }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="采样">×{{ run.sampleCount }}</el-descriptions-item>
          <el-descriptions-item label="时间" :span="3">{{ timeRangeText }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          type="info"
          show-icon
          :closable="false"
          class="compliance-alert"
          :title="complianceText"
        />

        <div class="header-actions">
          <el-button
            plain
            :disabled="!canExportReport"
            :loading="exportingDocx"
            @click="handleExportReport('docx')"
          >
            导出 DOCX
          </el-button>
          <el-tooltip
            content="PDF 由 Gotenberg 生成；服务未启动时将提示启动指引（deploy/LOCAL_DOCKER.md）"
            placement="top"
          >
            <el-button
              plain
              :disabled="!canExportReport"
              :loading="exportingPdf"
              @click="handleExportReport('pdf')"
            >
              导出 PDF
            </el-button>
          </el-tooltip>
          <el-button
            v-if="run.status === 'PENDING' || run.status === 'RUNNING'"
            type="danger"
            plain
            disabled
          >
            取消任务
          </el-button>
        </div>
      </el-card>

      <el-alert
        v-if="run.status === 'PARTIAL_FAILED'"
        type="warning"
        show-icon
        :closable="false"
        title="部分子任务失败，评分基于成功样本"
        class="mb-3"
      />

      <!-- PENDING -->
      <el-card v-if="run.status === 'PENDING'" shadow="never">
        <el-empty description="等待调度，探针任务即将开始" />
      </el-card>

      <!-- FAILED -->
      <el-result v-else-if="run.status === 'FAILED'" icon="error" title="诊断任务失败">
        <template #sub-title>
          <p v-for="t in failedTasks" :key="t.id" class="fail-msg">{{ t.errorMessage || '探针失败' }}</p>
        </template>
        <template #extra>
          <el-button type="primary" disabled>重试</el-button>
          <el-button @click="goBack">返回列表</el-button>
        </template>
      </el-result>

      <!-- CANCELLED -->
      <el-result v-else-if="run.status === 'CANCELLED'" icon="info" title="任务已取消">
        <template #extra>
          <el-button @click="goBack">返回列表</el-button>
        </template>
      </el-result>

      <!-- RUNNING 进度条 -->
      <el-card v-if="run.status === 'RUNNING'" shadow="never" class="progress-card">
        <div class="progress-row">
          <el-progress :percentage="run.progress ?? 0" :stroke-width="14" style="flex: 1" />
          <span class="progress-text">{{ probeProgressText }}</span>
        </div>
      </el-card>

      <!-- SUCCESS / PARTIAL_FAILED / RUNNING：KPI（终态才显示数值） -->
      <el-row v-if="showKpiGrid" :gutter="16" class="kpi-row">
        <el-col v-for="item in kpiItems" :key="item.key" :xs="12" :md="8" :lg="4">
          <el-card shadow="never" class="kpi-card">
            <div class="kpi-label">{{ item.label }}</div>
            <div class="kpi-value">{{ item.display }}</div>
            <div class="kpi-weight">权重 {{ item.weight }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Tabs -->
      <el-card v-if="showTabs" shadow="hover" class="tabs-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="概览" name="overview" :disabled="run.status === 'RUNNING'">
            <el-row v-if="metrics" :gutter="20">
              <el-col :xs="24" :lg="12">
                <h3 class="section-title">六维指标</h3>
                <div v-for="bar in metricBars" :key="bar.key" class="metric-bar">
                  <div class="metric-bar__label">{{ bar.label }}</div>
                  <el-progress :percentage="bar.pct" :stroke-width="12" :color="bar.color" />
                </div>
              </el-col>
              <el-col :xs="24" :lg="12">
                <h3 class="section-title">竞品提及 Top</h3>
                <el-empty v-if="!competitorStats.length" description="暂无竞品数据" />
                <el-table v-else :data="competitorStats" size="small" border>
                  <el-table-column label="品牌" prop="name" />
                  <el-table-column label="提及次数" prop="count" width="100" align="center" />
                </el-table>
              </el-col>
            </el-row>
            <el-empty v-else description="暂无评分数据，等待探针完成" />
            <el-alert
              type="info"
              show-icon
              :closable="false"
              class="mt-4"
              title="优化建议将在 M2 由 score_json.recommendations 驱动；当前可依据问题明细制定内容策略。"
            />
          </el-tab-pane>

          <el-tab-pane label="问题明细" name="results">
            <div class="filter-bar">
              <el-select v-model="resultFilter.platform" placeholder="平台" clearable style="width: 120px">
                <el-option v-for="p in platformOptions" :key="p" :label="p" :value="p" />
              </el-select>
              <el-select v-model="resultFilter.brandMentioned" placeholder="品牌提及" clearable style="width: 120px">
                <el-option label="已提及" value="yes" />
                <el-option label="未提及" value="no" />
              </el-select>
              <el-select v-model="resultFilter.inTop3" placeholder="Top3" clearable style="width: 100px">
                <el-option label="是" value="yes" />
                <el-option label="否" value="no" />
              </el-select>
              <el-input v-model="resultFilter.keyword" placeholder="搜索回答文本" clearable style="width: 200px" />
            </div>
            <el-table :data="filteredResults" border row-key="id" default-expand-all>
              <el-table-column type="expand">
                <template #default="{ row }">
                  <div class="expand-panel">
                    <p class="expand-label">AI 回答</p>
                    <p class="expand-answer">{{ row.answerText || '—' }}</p>
                    <p v-if="row.citations.length" class="expand-label">引用 ({{ row.citations.length }})</p>
                    <el-table v-if="row.citations.length" :data="row.citations" size="small" border>
                      <el-table-column label="#" prop="rank" width="50" />
                      <el-table-column label="域名" prop="domain" width="160" />
                      <el-table-column label="标题" prop="title" show-overflow-tooltip />
                      <el-table-column label="URL" min-width="200">
                        <template #default="{ row: cit }">
                          <el-link :href="cit.url" target="_blank" type="primary">{{ cit.url }}</el-link>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-descriptions size="small" :column="3" class="mt-2">
                      <el-descriptions-item label="模型">{{ row.model || '—' }}</el-descriptions-item>
                      <el-descriptions-item label="采集">{{ row.captureMethod || '—' }}</el-descriptions-item>
                      <el-descriptions-item label="采样">{{ row.sampledAt || '—' }}</el-descriptions-item>
                    </el-descriptions>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="问题 ID" prop="questionId" width="90" align="center" />
              <el-table-column label="回答摘要" min-width="220">
                <template #default="{ row }">
                  {{ answerPreview(row.answerText) }}
                </template>
              </el-table-column>
              <el-table-column label="平台" prop="platform" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.platform }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="排名" prop="rank" width="70" align="center">
                <template #default="{ row }">{{ row.rank ?? '—' }}</template>
              </el-table-column>
              <el-table-column label="品牌" min-width="120">
                <template #default="{ row }">
                  <el-tag v-for="b in row.mentionedBrands.slice(0, 2)" :key="b" size="small" type="success" class="mr-1">
                    {{ b }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="竞品" min-width="120" class-name="hidden-xs-only">
                <template #default="{ row }">
                  <el-tag v-for="c in row.competitors.slice(0, 2)" :key="c" size="small" type="warning" class="mr-1">
                    {{ c }}
                  </el-tag>
                  <span v-if="row.competitors.length > 2">+{{ row.competitors.length - 2 }}</span>
                </template>
              </el-table-column>
              <el-table-column label="引用" width="70" align="center" class-name="hidden-sm-only">
                <template #default="{ row }">{{ row.citations.length }}</template>
              </el-table-column>
              <el-table-column label="采样时间" prop="sampledAt" width="170" class-name="hidden-xs-only" />
            </el-table>
            <el-empty v-if="!filteredResults.length" description="探针尚未产生结果" />
          </el-tab-pane>

          <el-tab-pane label="竞品对比" name="competitors">
            <el-table v-if="competitorStats.length" :data="competitorStats" border>
              <el-table-column label="品牌" prop="name" />
              <el-table-column label="结果中出现次数" prop="count" width="140" align="center" />
              <el-table-column label="说明">
                <template #default>基于本次诊断采样提及统计（MVP）</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无竞品对比数据" />
            <div class="mt-4 trends-link">
              <router-link :to="trendsLink">查看历史趋势 →</router-link>
            </div>
          </el-tab-pane>

          <el-tab-pane label="探针进度" name="probes">
            <el-table :data="probeTasks" border>
              <el-table-column label="问题 ID" prop="questionId" width="90" align="center" />
              <el-table-column label="平台" prop="platform" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.platform }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="探针" prop="probeMode" width="120" />
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="probeStatusMeta(row.status).type">
                    {{ probeStatusMeta(row.status).label }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="重试" prop="retryCount" width="70" align="center" />
              <el-table-column label="耗时" width="100" align="center">
                <template #default="{ row }">{{ taskDuration(row) }}</template>
              </el-table-column>
              <el-table-column label="错误" prop="errorMessage" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>
  </div>
</template>

<script setup name="DiagnosticDetail" lang="ts">
import {
  computeMetricsFromResults,
  downloadDiagnosticReport,
  getDiagnosticResults,
  getDiagnosticRun,
  getProbeTasks
} from '@/api/tourgeo/diagnostic';
import type { DiagnosticResultVO, DiagnosticRunVO, ProbeTaskVO } from '@/api/tourgeo/types';
import DiagnosticStatusTag from '@/components/tourgeo/DiagnosticStatusTag.vue';
import GeoScoreDisplay from '@/components/tourgeo/GeoScoreDisplay.vue';
import { METRIC_WEIGHT_LABELS, PROBE_TASK_STATUS_META } from '@/constants/diagnostic';
import { ElMessage } from 'element-plus';

const POLL_MS = 5000;

const route = useRoute();
const router = useRouter();

const pageLoading = ref(true);
const loadError = ref<'404' | 'other' | null>(null);
const run = ref<DiagnosticRunVO | null>(null);
const results = ref<DiagnosticResultVO[]>([]);
const probeTasks = ref<ProbeTaskVO[]>([]);
const activeTab = ref('overview');
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null);
const exportingDocx = ref(false);
const exportingPdf = ref(false);

const resultFilter = reactive({
  platform: '',
  brandMentioned: '' as '' | 'yes' | 'no',
  inTop3: '' as '' | 'yes' | 'no',
  keyword: ''
});

const runId = computed(() => Number(route.params.runId));

const trendsLink = computed(() => ({
  path: '/diagnostics/trends',
  query: run.value ? { runIds: String(run.value.id) } : undefined
}));

const showScore = computed(
  () => run.value && ['SUCCESS', 'PARTIAL_FAILED'].includes(run.value.status)
);

const showKpiGrid = computed(
  () => run.value && ['SUCCESS', 'PARTIAL_FAILED'].includes(run.value.status) && metrics.value
);

const showTabs = computed(
  () => run.value && !['PENDING', 'FAILED', 'CANCELLED'].includes(run.value.status)
);

const canExportReport = computed(
  () => run.value && ['SUCCESS', 'PARTIAL_FAILED'].includes(run.value.status)
);

const isPolling = computed(
  () => run.value && (run.value.status === 'RUNNING' || run.value.status === 'PENDING')
);

const timeRangeText = computed(() => {
  if (!run.value) return '—';
  const start = run.value.startedAt || run.value.createdAt;
  const end = run.value.finishedAt;
  if (end) return `${formatTime(start)} – ${formatTime(end)}`;
  return `${formatTime(start)} – 进行中`;
});

const complianceText = computed(() => {
  if (!run.value) return '';
  const platforms = [...new Set(probeTasks.value.map((t) => t.platform))].join(', ') || run.value.models.join(', ');
  const probe = run.value.probeModes.join(', ') || 'grounded-api';
  return `采样结果基于联网检索，不承诺 AI 推荐排名。probe_mode=${probe}；region=${run.value.region || run.value.market}；locale=${run.value.locale}；平台：${platforms}。本报告为指定时刻、指定平台的采样结果，不代表持续排名承诺。`;
});

const probeProgressText = computed(() => {
  const total = probeTasks.value.length;
  const done = probeTasks.value.filter((t) => t.status === 'SUCCESS' || t.status === 'FAILED').length;
  return `已完成 ${done}/${total} 探针子任务`;
});

const failedTasks = computed(() => probeTasks.value.filter((t) => t.status === 'FAILED'));

const metrics = computed(() => computeMetricsFromResults(results.value));

const platformOptions = computed(() => [...new Set(results.value.map((r) => r.platform))]);

const filteredResults = computed(() => {
  return results.value.filter((r) => {
    if (resultFilter.platform && r.platform !== resultFilter.platform) return false;
    if (resultFilter.brandMentioned === 'yes' && !r.mentionedBrands.length) return false;
    if (resultFilter.brandMentioned === 'no' && r.mentionedBrands.length) return false;
    if (resultFilter.inTop3 === 'yes' && (r.rank == null || r.rank > 3)) return false;
    if (resultFilter.inTop3 === 'no' && r.rank != null && r.rank <= 3) return false;
    if (resultFilter.keyword) {
      const kw = resultFilter.keyword.toLowerCase();
      if (!(r.answerText || '').toLowerCase().includes(kw)) return false;
    }
    return true;
  });
});

const kpiItems = computed(() => {
  const m = metrics.value;
  if (!m) return [];
  return [
    { key: 'brandMentionRate', label: '品牌出现率', display: pct(m.brandMentionRate), weight: METRIC_WEIGHT_LABELS.brandMentionRate },
    { key: 'top3Rate', label: 'Top3 推荐率', display: pct(m.top3Rate), weight: METRIC_WEIGHT_LABELS.top3Rate },
    {
      key: 'competitorSuppression',
      label: '竞品压制指数',
      display: pct(m.competitorSuppression),
      weight: METRIC_WEIGHT_LABELS.competitorSuppression
    },
    { key: 'citationCoverage', label: '引用覆盖', display: pct(m.citationCoverage), weight: METRIC_WEIGHT_LABELS.citationCoverage },
    { key: 'longtailCoverage', label: '长尾覆盖', display: pct(m.longtailCoverage), weight: METRIC_WEIGHT_LABELS.longtailCoverage },
    { key: 'assetCompleteness', label: '资产完整度', display: pct(m.assetCompleteness), weight: METRIC_WEIGHT_LABELS.assetCompleteness }
  ];
});

const metricBars = computed(() => {
  const m = metrics.value;
  if (!m) return [];
  return kpiItems.value.map((item) => ({
    key: item.key,
    label: item.label,
    pct: Math.round(parseFloat(item.display)),
    color: item.key === 'competitorSuppression' ? 'var(--tg-score-low, #dc2626)' : 'var(--tg-color-primary, #1677a0)'
  }));
});

const competitorStats = computed(() => {
  const counts = new Map<string, number>();
  for (const r of results.value) {
    for (const c of r.competitors) {
      counts.set(c, (counts.get(c) ?? 0) + 1);
    }
  }
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);
});

watch(
  () => run.value?.status,
  (status) => {
    if (status === 'RUNNING' || status === 'PENDING') {
      activeTab.value = 'probes';
    } else if (status === 'SUCCESS' || status === 'PARTIAL_FAILED') {
      activeTab.value = 'overview';
    }
  },
  { immediate: true }
);

function pct(v: number) {
  return `${(v * 100).toFixed(1)}%`;
}

function formatTime(iso: string) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

function answerPreview(text?: string) {
  if (!text) return '—';
  return text.length > 80 ? `${text.slice(0, 80)}…` : text;
}

function probeStatusMeta(status: string) {
  return PROBE_TASK_STATUS_META[status] ?? { label: status, type: 'info' as const };
}

function taskDuration(task: ProbeTaskVO) {
  if (!task.dispatchedAt || !task.finishedAt) return '—';
  const ms = new Date(task.finishedAt).getTime() - new Date(task.dispatchedAt).getTime();
  if (ms < 1000) return `${ms}ms`;
  return `${Math.round(ms / 1000)}s`;
}

function goBack() {
  router.push({ name: 'DiagnosticRuns' });
}

async function handleExportReport(format: 'docx' | 'pdf') {
  if (!run.value || !canExportReport.value) return;
  const loadingRef = format === 'docx' ? exportingDocx : exportingPdf;
  loadingRef.value = true;
  const safeName = (run.value.name || 'diagnostic').replace(/[\\/:*?"<>|]/g, '-');
  const ext = format === 'docx' ? 'docx' : 'pdf';
  const filename = `${safeName}-geo-report-${run.value.id}.${ext}`;
  try {
    await downloadDiagnosticReport(run.value.id, format, filename);
    ElMessage.success(format === 'docx' ? 'DOCX 报告已下载' : 'PDF 报告已下载');
  } catch (e) {
    const msg = e instanceof Error ? e.message : '导出失败';
    ElMessage.error(msg);
  } finally {
    loadingRef.value = false;
  }
}

async function loadAll(silent = false) {
  if (!runId.value || Number.isNaN(runId.value)) {
    loadError.value = '404';
    return;
  }
  if (!silent) pageLoading.value = true;
  try {
    const [runData, resultData, taskData] = await Promise.all([
      getDiagnosticRun(runId.value),
      getDiagnosticResults(runId.value),
      getProbeTasks(runId.value)
    ]);
    run.value = runData;
    results.value = resultData;
    probeTasks.value = taskData;
    loadError.value = null;
  } catch {
    if (!run.value) loadError.value = '404';
  } finally {
    pageLoading.value = false;
  }
}

function startPolling() {
  stopPolling();
  pollTimer.value = setInterval(() => {
    if (isPolling.value) loadAll(true);
    else stopPolling();
  }, POLL_MS);
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

onMounted(async () => {
  await loadAll();
  if (isPolling.value) startPolling();
});

watch(isPolling, (polling) => {
  if (polling) startPolling();
  else stopPolling();
});

onUnmounted(stopPolling);
</script>

<style scoped lang="scss">
.tg-diagnostic-detail {
  color: var(--tg-color-text-regular);
}

.breadcrumb-text {
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.header-card {
  margin: var(--tg-space-3) 0;
}

.header-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--tg-space-3);
  margin-bottom: var(--tg-space-3);
}

.header-main {
  display: flex;
  align-items: center;
  gap: var(--tg-space-3);
  flex-wrap: wrap;
}

.run-title {
  margin: 0;
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
}

.header-score {
  display: flex;
  align-items: baseline;
  gap: var(--tg-space-2);
}

.score-label {
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.score-value :deep(.geo-score) {
  font-size: 2rem;
}

.score-placeholder {
  font-size: 2rem;
  color: var(--tg-color-text-secondary);
}

.run-meta {
  margin-bottom: var(--tg-space-3);
}

.compliance-alert {
  margin-bottom: var(--tg-space-3);
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--tg-space-2);
}

.progress-card {
  margin-bottom: var(--tg-space-3);
}

.progress-row {
  display: flex;
  align-items: center;
  gap: var(--tg-space-4);
}

.progress-text {
  white-space: nowrap;
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.kpi-row {
  margin-bottom: var(--tg-space-3);
}

.kpi-card {
  text-align: center;
  margin-bottom: var(--tg-space-2);
}

.kpi-label {
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
}

.kpi-value {
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
  margin: var(--tg-space-1) 0;
}

.kpi-weight {
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
}

.tabs-card {
  margin-top: var(--tg-space-3);
}

.section-title {
  font-size: var(--tg-font-size-base);
  margin: 0 0 var(--tg-space-3);
}

.metric-bar {
  margin-bottom: var(--tg-space-3);
}

.metric-bar__label {
  font-size: var(--tg-font-size-sm);
  margin-bottom: var(--tg-space-1);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--tg-space-2);
  margin-bottom: var(--tg-space-3);
}

.expand-panel {
  padding: var(--tg-space-3);
  background: var(--tg-color-bg-muted, #f9fafb);
}

.expand-label {
  font-weight: 600;
  font-size: var(--tg-font-size-sm);
  margin: 0 0 var(--tg-space-1);
}

.expand-answer {
  margin: 0 0 var(--tg-space-3);
  white-space: pre-wrap;
  line-height: 1.5;
}

.fail-msg {
  margin: 0.25rem 0;
  color: var(--tg-color-text-secondary);
}

.trends-link a {
  color: var(--tg-color-primary);
  font-size: var(--tg-font-size-sm);
  text-decoration: none;
}

.trends-link a:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  :deep(.hidden-xs-only) {
    display: none;
  }
}

@media (max-width: 992px) {
  :deep(.hidden-sm-only) {
    display: none;
  }
}
</style>
