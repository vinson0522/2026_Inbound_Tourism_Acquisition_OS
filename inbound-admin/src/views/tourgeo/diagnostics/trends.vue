<template>
  <div class="p-2 tg-diagnostic-trends">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">趋势对比</h1>
        <p class="tg-page-sub">GEO 诊断 · 多次 run 可见率变化（FR-108）</p>
      </div>
      <project-selector @change="handleProjectChange" />
    </div>

    <el-skeleton v-if="!projectStore.initialized && projectStore.loading" :rows="6" animated />

    <el-empty v-else-if="!projectStore.hasProject" description="请先选择客户项目">
      <project-selector :show-label="false" />
    </el-empty>

    <template v-else>
      <el-card shadow="hover" class="mb-3 filter-card">
        <el-form :inline="true" label-width="72px">
          <el-form-item label="时间范围">
            <el-select v-model="timePreset" placeholder="时间范围" style="width: 120px">
              <el-option label="近30天" value="30" />
              <el-option label="近90天" value="90" />
              <el-option label="全部" value="all" />
              <el-option label="自定义" value="custom" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="timePreset === 'custom'" label="日期">
            <el-date-picker
              v-model="customDateRange"
              type="daterange"
              range-separator="-"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="市场">
            <el-select v-model="marketFilter" placeholder="全部" clearable style="width: 120px">
              <el-option label="全部" value="" />
              <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" :loading="loading" @click="loadTrends">应用</el-button>
            <el-button icon="Refresh" @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-loading="loading" shadow="hover" class="mb-3">
        <template #header>
          <div class="selector-header">
            <span>选择对比任务（2–6 次）</span>
            <el-button v-if="allRuns.length >= 2" link type="primary" @click="selectRecentSix">全选最近 6 次</el-button>
          </div>
        </template>

        <el-empty v-if="!loading && allRuns.length === 0" description="暂无已完成诊断">
          <el-button type="primary" @click="router.push('/diagnostics/runs')">前往诊断任务</el-button>
        </el-empty>

        <el-empty
          v-else-if="!loading && allRuns.length === 1"
          description="至少需要 2 次成功诊断才可查看趋势"
        >
          <el-button type="primary" @click="router.push('/diagnostics/runs')">发起第二次诊断</el-button>
        </el-empty>

        <el-checkbox-group
          v-else
          v-model="selectedRunIds"
          class="run-selector"
          @change="handleSelectionChange"
        >
          <el-checkbox
            v-for="run in allRunsDesc"
            :key="run.runId"
            :label="run.runId"
            class="run-checkbox"
          >
            <div class="run-card">
              <span class="run-name">{{ run.name }}</span>
              <geo-score-display :score="run.geoScore" />
              <el-tag size="small" type="info">{{ run.market }}</el-tag>
              <span class="run-date">{{ formatShortDate(run.finishedAt) }}</span>
              <el-tag v-if="run.status === 'PARTIAL_FAILED'" size="small" type="warning">部分失败</el-tag>
            </div>
          </el-checkbox>
        </el-checkbox-group>
      </el-card>

      <el-row v-if="allRuns.length >= 2" :gutter="16" class="chart-row">
        <el-col :xs="24" :lg="16">
          <el-card v-loading="loading" shadow="hover" class="chart-card">
            <template #header>GEO 综合分趋势</template>
            <el-empty v-if="selectedRuns.length < 2" description="请再选择至少 1 次诊断" />
            <div v-show="selectedRuns.length >= 2" ref="lineChartRef" class="chart-box" />
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="8">
          <el-card v-if="summary" shadow="hover" class="summary-card">
            <template #header>变化摘要</template>
            <el-statistic title="最新 GEO 分" :value="summary.latest" :precision="1" />
            <el-statistic title="较上次变化" class="mt-stat">
              <template #default>
                <span :class="summary.delta >= 0 ? 'delta-up' : 'delta-down'">
                  {{ summary.delta >= 0 ? '+' : '' }}{{ summary.delta.toFixed(1) }}
                  {{ summary.delta >= 0 ? '↑' : '↓' }}
                </span>
              </template>
            </el-statistic>
            <el-statistic title="期间最高" :value="summary.max" :precision="1" class="mt-stat" />
            <el-statistic title="期间最低" :value="summary.min" :precision="1" class="mt-stat" />
            <el-statistic title="平均值" :value="summary.avg" :precision="1" class="mt-stat" />
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="selectedRuns.length >= 2" shadow="hover" class="mb-3">
        <template #header>分项指标对比</template>
        <div ref="barChartRef" class="chart-box chart-box--bar" />
        <el-table
          :data="selectedRuns"
          border
          size="small"
          class="metrics-table metrics-table--clickable"
          @row-click="goRunDetail"
        >
          <el-table-column label="诊断任务" prop="name" min-width="140" show-overflow-tooltip />
          <el-table-column label="完成时间" width="170">
            <template #default="{ row }">{{ formatTime(row.finishedAt) }}</template>
          </el-table-column>
          <el-table-column label="GEO 分" width="88" align="center">
            <template #default="{ row }">{{ row.geoScore.toFixed(1) }}</template>
          </el-table-column>
          <el-table-column
            v-for="col in metricColumns"
            :key="col.key"
            :label="col.label"
            width="100"
            align="center"
          >
            <template #default="{ row }">{{ pct(row.metrics[col.key]) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <p class="compliance-footer">
        趋势图展示历史采样 GEO 分数变化，受模型版本、探针模式、采样时间影响，不表示持续排名承诺。报告级明细请进入单次诊断详情页，标注
        probe_mode 与 sampled_at。
      </p>
    </template>
  </div>
</template>

<script setup name="DiagnosticTrends" lang="ts">
import { getDiagnosticTrends } from '@/api/tourgeo/diagnostic';
import type { DiagnosticTrendPointVO } from '@/api/tourgeo/types';
import GeoScoreDisplay from '@/components/tourgeo/GeoScoreDisplay.vue';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import { useProjectStore } from '@/store/modules/project';
import * as echarts from 'echarts';
import { ElMessage } from 'element-plus';

const TREND_LINE_COLOR = '#1677A0';
const TREND_COLORS = ['#1677A0', '#059669', '#D4920A', '#64748B', '#7C3AED', '#DC2626'];
const MAX_SELECT = 6;
type TimePreset = '30' | '90' | 'all' | 'custom';

const METRIC_COLUMNS = [
  { key: 'brandMentionRate' as const, label: '品牌出现率' },
  { key: 'top3Rate' as const, label: 'Top3' },
  { key: 'competitorSuppression' as const, label: '竞品压制' },
  { key: 'citationCoverage' as const, label: '引用覆盖' },
  { key: 'longtailCoverage' as const, label: '长尾覆盖' },
  { key: 'assetCompleteness' as const, label: '资产完整度' }
];

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const allRuns = ref<DiagnosticTrendPointVO[]>([]);
const selectedRunIds = ref<number[]>([]);
const marketFilter = ref('');
const timePreset = ref<TimePreset>('90');
const customDateRange = ref<string[]>([]);
const lineChartRef = ref<HTMLElement>();
const barChartRef = ref<HTMLElement>();
let lineChart: echarts.ECharts | null = null;
let barChart: echarts.ECharts | null = null;

const metricColumns = METRIC_COLUMNS;
const marketOptions = computed(() => projectStore.currentProject?.targetMarkets ?? []);

const allRunsDesc = computed(() =>
  [...allRuns.value].sort((a, b) => Date.parse(b.finishedAt) - Date.parse(a.finishedAt))
);

const selectedRuns = computed(() => {
  const set = new Set(selectedRunIds.value);
  return allRuns.value.filter((r) => set.has(r.runId)).sort((a, b) => Date.parse(a.finishedAt) - Date.parse(b.finishedAt));
});

const summary = computed(() => {
  const runs = selectedRuns.value;
  if (runs.length < 2) return null;
  const scores = runs.map((r) => r.geoScore);
  const latest = scores[scores.length - 1];
  const prev = scores[scores.length - 2];
  return {
    latest,
    delta: latest - prev,
    max: Math.max(...scores),
    min: Math.min(...scores),
    avg: scores.reduce((a, b) => a + b, 0) / scores.length
  };
});

function pct(rate: number): string {
  return `${(rate * 100).toFixed(1)}%`;
}

function formatIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function resolveFinishedRange(): { from?: string; to?: string } | null {
  if (timePreset.value === 'all') {
    return {};
  }
  if (timePreset.value === '30' || timePreset.value === '90') {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - Number(timePreset.value) + 1);
    return { from: formatIsoDate(start), to: formatIsoDate(end) };
  }
  if (timePreset.value === 'custom') {
    if (!customDateRange.value || customDateRange.value.length !== 2) {
      return null;
    }
    const [from, to] = customDateRange.value;
    if (to < from) {
      ElMessage.warning('结束日期不能早于开始日期');
      return null;
    }
    return { from, to };
  }
  return {};
}

function goRunDetail(row: DiagnosticTrendPointVO) {
  router.push(`/diagnostics/runs/${row.runId}`);
}

function formatShortDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${mm}-${dd}`;
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function parseRunIdsFromQuery(): number[] {
  const raw = route.query.runIds;
  const str = Array.isArray(raw) ? raw.join(',') : String(raw ?? '');
  return str
    .split(',')
    .map((s) => Number(s.trim()))
    .filter((n) => !Number.isNaN(n) && n > 0);
}

function initDefaultSelection() {
  if (allRuns.value.length < 2) {
    selectedRunIds.value = allRuns.value.map((r) => r.runId);
    return;
  }
  const fromQuery = parseRunIdsFromQuery().filter((id) => allRuns.value.some((r) => r.runId === id));
  if (fromQuery.length) {
    const ids = [...fromQuery];
    if (ids.length < 2) {
      const extras = allRuns.value
        .filter((r) => !ids.includes(r.runId))
        .slice(-(2 - ids.length))
        .map((r) => r.runId);
      ids.push(...extras);
    }
    selectedRunIds.value = ids.slice(0, MAX_SELECT);
    return;
  }
  selectedRunIds.value = allRuns.value.slice(-2).map((r) => r.runId);
}

function selectRecentSix() {
  selectedRunIds.value = allRuns.value.slice(-MAX_SELECT).map((r) => r.runId);
}

function handleSelectionChange(ids: number[]) {
  if (ids.length > MAX_SELECT) {
    ElMessage.warning(`最多选择 ${MAX_SELECT} 次诊断`);
    selectedRunIds.value = ids.slice(0, MAX_SELECT);
  }
}

function disposeCharts() {
  lineChart?.dispose();
  barChart?.dispose();
  lineChart = null;
  barChart = null;
}

function renderLineChart() {
  if (!lineChartRef.value || selectedRuns.value.length < 2) return;
  if (!lineChart) {
    lineChart = echarts.init(lineChartRef.value);
    lineChart.on('click', (params) => {
      const idx = params.dataIndex;
      const run = selectedRuns.value[idx];
      if (run) router.push(`/diagnostics/runs/${run.runId}`);
    });
  }
  const runs = selectedRuns.value;
  lineChart.setOption(
    {
      color: [TREND_LINE_COLOR],
      tooltip: {
        trigger: 'axis',
        formatter: (items: { dataIndex: number }[]) => {
          const run = runs[items[0]?.dataIndex ?? 0];
          if (!run) return '';
          return `${run.name}<br/>GEO ${run.geoScore.toFixed(1)}<br/>${run.market}<br/>${formatTime(run.finishedAt)}<br/><span style="opacity:0.75">点击查看详情</span>`;
        }
      },
      grid: { left: 48, right: 24, top: 32, bottom: 48 },
      xAxis: {
        type: 'category',
        data: runs.map((r) => formatShortDate(r.finishedAt)),
        axisLabel: { color: '#6B7280' }
      },
      yAxis: {
        type: 'value',
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: { color: '#6B7280' }
      },
      series: [
        {
          name: 'GEO 综合分',
          type: 'line',
          data: runs.map((r) => r.geoScore),
          symbolSize: 8,
          lineStyle: { width: 2 },
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { type: 'dashed', color: '#D97706', opacity: 0.45 },
            label: { formatter: '参考 40', color: '#9CA3AF', fontSize: 11 },
            data: [{ yAxis: 40 }]
          }
        }
      ]
    },
    true
  );
}

function renderBarChart() {
  if (!barChartRef.value || selectedRuns.value.length < 2) return;
  if (!barChart) {
    barChart = echarts.init(barChartRef.value);
  }
  const runs = selectedRuns.value;
  barChart.setOption(
    {
      color: TREND_COLORS,
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        valueFormatter: (v: number) => `${v.toFixed(1)}%`
      },
      legend: { bottom: 0, type: 'scroll' },
      grid: { left: 48, right: 24, top: 24, bottom: 56 },
      xAxis: {
        type: 'category',
        data: METRIC_COLUMNS.map((c) => c.label),
        axisLabel: { interval: 0, rotate: 20, fontSize: 11 }
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: { formatter: '{value}%' }
      },
      series: runs.map((run, idx) => ({
        name: run.name,
        type: 'bar',
        data: METRIC_COLUMNS.map((c) => Number((run.metrics[c.key] * 100).toFixed(1))),
        itemStyle: { color: TREND_COLORS[idx % TREND_COLORS.length] }
      }))
    },
    true
  );
}

function updateCharts() {
  nextTick(() => {
    if (selectedRuns.value.length >= 2) {
      renderLineChart();
      renderBarChart();
      lineChart?.resize();
      barChart?.resize();
    } else {
      lineChart?.clear();
      barChart?.clear();
    }
  });
}

async function loadTrends() {
  const projectId = projectStore.currentProjectId;
  if (!projectId) return;

  const range = resolveFinishedRange();
  if (range === null) {
    ElMessage.warning('请选择自定义日期范围');
    return;
  }

  loading.value = true;
  disposeCharts();
  try {
    const data = await getDiagnosticTrends(projectId, {
      limit: 52,
      market: marketFilter.value || undefined,
      from: range.from,
      to: range.to
    });
    allRuns.value = data.runs;
    initDefaultSelection();
    updateCharts();
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  marketFilter.value = '';
  timePreset.value = '90';
  customDateRange.value = [];
  loadTrends();
}

function handleProjectChange() {
  loadTrends();
}

function onResize() {
  lineChart?.resize();
  barChart?.resize();
}

watch(selectedRuns, () => updateCharts(), { deep: true });

onMounted(async () => {
  if (!projectStore.initialized) {
    await projectStore.fetchProjects();
  }
  await loadTrends();
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  disposeCharts();
});
</script>

<style scoped lang="scss">
.tg-diagnostic-trends {
  color: var(--tg-color-text-regular);
}

.tg-page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--tg-space-4);
  margin-bottom: var(--tg-space-3);
}

.tg-page-title {
  margin: 0;
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
  color: var(--tg-color-text-primary);
}

.tg-page-sub {
  margin: var(--tg-space-1) 0 0;
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.selector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.run-selector {
  display: flex;
  flex-wrap: wrap;
  gap: var(--tg-space-3);
}

.run-checkbox {
  height: auto;
  margin-right: 0;

  :deep(.el-checkbox__label) {
    padding-left: var(--tg-space-2);
  }
}

.run-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--tg-space-2);
  padding: var(--tg-space-2) var(--tg-space-3);
  border: 1px solid var(--tg-color-border);
  border-radius: 6px;
  background: var(--tg-color-bg-surface);
}

.run-name {
  font-weight: 500;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-date {
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
}

.chart-row {
  margin-bottom: var(--tg-space-4);
}

.chart-box {
  height: 320px;
  width: 100%;
}

.chart-box--bar {
  margin-bottom: var(--tg-space-4);
}

.summary-card {
  margin-bottom: var(--tg-space-4);
}

.mt-stat {
  margin-top: var(--tg-space-4);
}

.delta-up {
  color: var(--tg-color-success);
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
}

.delta-down {
  color: var(--tg-color-danger);
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
}

.metrics-table {
  margin-top: var(--tg-space-2);
}

.metrics-table--clickable :deep(.el-table__row) {
  cursor: pointer;
}

.metrics-table--clickable :deep(.el-table__row:hover) {
  background-color: var(--el-fill-color-light);
}

.compliance-footer {
  margin: var(--tg-space-4) 0 0;
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
  line-height: 1.5;
}

@media (max-width: 992px) {
  .chart-box {
    height: 260px;
  }
}
</style>
