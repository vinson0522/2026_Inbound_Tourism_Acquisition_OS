<template>
  <div class="p-2 tg-reports">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">报告列表</h1>
        <p class="tg-page-sub">报告中心 · GEO 诊断报告与增长周报（FR-701/702）</p>
      </div>
      <project-selector @change="handleProjectChange" />
    </div>

    <el-skeleton v-if="!projectStore.initialized && projectStore.loading" :rows="8" animated />

    <el-empty v-else-if="!projectStore.hasProject" description="请先创建客户项目">
      <el-button type="primary" @click="router.push('/projects/index')">前往项目列表</el-button>
    </el-empty>

    <template v-else>
      <el-card shadow="never" class="mb-3 project-card">
        <div class="project-card__row">
          <span class="project-card__label">当前项目</span>
          <span class="project-card__name">{{ currentProject?.name }}</span>
          <el-tag v-for="m in marketOptions" :key="m" size="small" class="mr-1">{{ m }}</el-tag>
        </div>
      </el-card>

      <el-card shadow="hover" class="mb-3 toolbar-card">
        <div class="toolbar-row">
          <el-button type="primary" icon="Document" :loading="weeklySubmitting" @click="openWeeklyDialog">
            生成本周报告
          </el-button>
          <el-tooltip content="月度增长报告 FR-703 · M2" placement="top">
            <el-button disabled>月报</el-button>
          </el-tooltip>
          <el-tooltip content="白标模板 FR-704 · M2" placement="top">
            <el-button disabled>模板配置</el-button>
          </el-tooltip>
          <el-form :inline="true" class="toolbar-form" @submit.prevent="handleQuery">
            <el-form-item label="报告类型">
              <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option v-for="opt in REPORT_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="周期/关联">
              <el-input
                v-model="queryParams.period"
                placeholder="2026-W26 或 run 号"
                clearable
                style="width: 140px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="创建时间">
              <el-date-picker
                v-model="createdAtRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始"
                end-placeholder="结束"
                style="width: 240px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <p v-if="clientDateFilterActive" class="filter-hint">创建时间为 M1 当前页客户端筛选；类型 / 周期走服务端分页。</p>
      </el-card>

      <el-card shadow="hover">
        <el-table v-loading="loading" border :data="displayList">
          <template #empty>
            <el-empty :description="emptyDescription">
              <template v-if="!hasActiveFilters">
                <p class="empty-sub">完成 GEO 诊断并导出，或生成本周增长报告</p>
                <el-button type="primary" @click="openWeeklyDialog">生成本周报告</el-button>
                <el-button @click="goDiagnostics">前往诊断</el-button>
              </template>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="typeMeta(row.type).type">{{ typeMeta(row.type).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="周期/关联" width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button
                v-if="row.type === 'DIAGNOSTIC' && diagnosticRunId(row)"
                link
                type="primary"
                @click="goDiagnosticDetail(row)"
              >
                诊断 #{{ diagnosticRunId(row) }}
              </el-button>
              <span v-else-if="row.type === 'WEEKLY'">{{ row.period || '—' }}</span>
              <span v-else>{{ row.period || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" class="summary-link" @click="openPreview(row)">
                {{ summaryLine(row) }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPreview(row)">预览</el-button>
              <el-button
                link
                type="primary"
                :loading="exportingKey === `${row.id}-docx`"
                @click="handleExport(row, 'docx')"
              >
                DOCX
              </el-button>
              <el-button
                link
                type="primary"
                :loading="exportingKey === `${row.id}-pdf`"
                @click="handleExport(row, 'pdf')"
              >
                PDF
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :total="total"
          @pagination="getList"
        />

        <p class="page-hint">诊断报告由 GEO 详情页导出写入；周报由本页手动生成。</p>
      </el-card>
    </template>

    <el-dialog v-model="weeklyVisible" title="生成本周增长报告 (FR-702)" width="480px" destroy-on-close @closed="resetWeeklyForm">
      <el-form label-width="88px">
        <el-form-item label="统计区间" required>
          <el-date-picker
            v-model="weeklyDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报告周期">
          <el-input :model-value="weeklyPeriodLabel" readonly />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon class="mb-2">
          将聚合 GEO 诊断、关键词、内容任务、落地页与询盘数据；含 3 条静态优化建议（无 AI 摘要）。
        </el-alert>
        <el-alert type="warning" :closable="false" show-icon>
          同周期重复生成将创建新记录（不覆盖）。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="weeklyVisible = false">取消</el-button>
        <el-button type="primary" :loading="weeklySubmitting" @click="submitWeekly">生成报告</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="previewVisible"
      :title="previewTitle"
      size="600px"
      destroy-on-close
      class="report-preview-drawer"
      @closed="resetPreview"
    >
      <div v-loading="previewLoading">
        <template v-if="previewDetail">
          <div class="preview-header">
            <el-tag :type="typeMeta(previewDetail.type).type">{{ typeMeta(previewDetail.type).label }}</el-tag>
            <span v-if="previewDetail.type === 'WEEKLY' && parsedPreview?.periodStart" class="preview-range">
              {{ parsedPreview.periodStart }} ~ {{ parsedPreview.periodEnd }}
            </span>
            <span v-else-if="diagnosticRunId(previewDetail)" class="preview-range">
              诊断 #{{ diagnosticRunId(previewDetail) }}
            </span>
            <span class="preview-time">{{ formatTime(previewDetail.createdAt) }}</span>
          </div>

          <template v-if="previewDetail.type === 'WEEKLY'">
            <el-row :gutter="12" class="kpi-row">
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">GEO 分数</div>
                  <div class="kpi-value">
                    {{ parsedPreview?.geo?.latestScore ?? '—' }}
                    <span v-if="parsedPreview?.geo?.delta != null" class="kpi-delta">
                      {{ parsedPreview.geo.delta >= 0 ? '↑' : '↓' }}{{ Math.abs(parsedPreview.geo.delta) }}
                    </span>
                  </div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">询盘</div>
                  <div class="kpi-value">{{ parsedPreview?.leads?.newCount ?? 0 }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">新词</div>
                  <div class="kpi-value">{{ parsedPreview?.keywords?.newCount ?? 0 }}</div>
                </div>
              </el-col>
            </el-row>
            <el-row :gutter="12" class="kpi-row">
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">内容生成</div>
                  <div class="kpi-value">{{ parsedPreview?.content?.generated ?? 0 }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">落地页</div>
                  <div class="kpi-value">{{ parsedPreview?.landing?.draftCount ?? 0 }}</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="kpi-card">
                  <div class="kpi-label">诊断次数</div>
                  <div class="kpi-value">{{ parsedPreview?.geo?.runs ?? 0 }}</div>
                </div>
              </el-col>
            </el-row>

            <h4 class="section-title">章节摘要</h4>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="GEO">
                诊断 {{ parsedPreview?.geo?.runs ?? 0 }} 次 · 最新分 {{ parsedPreview?.geo?.latestScore ?? '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="关键词">
                新增 {{ parsedPreview?.keywords?.newCount ?? 0 }} 条
              </el-descriptions-item>
              <el-descriptions-item label="内容">
                任务 {{ parsedPreview?.content?.tasksCreated ?? 0 }} · 生成 {{ parsedPreview?.content?.generated ?? 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="询盘">
                新增 {{ parsedPreview?.leads?.newCount ?? 0 }} 条
              </el-descriptions-item>
            </el-descriptions>

            <h4 v-if="parsedPreview?.recommendations?.length" class="section-title">优化建议</h4>
            <el-timeline v-if="parsedPreview?.recommendations?.length">
              <el-timeline-item v-for="(item, idx) in parsedPreview!.recommendations!" :key="idx">
                {{ item }}
              </el-timeline-item>
            </el-timeline>
          </template>

          <template v-else-if="previewDetail.type === 'DIAGNOSTIC'">
            <el-descriptions :column="1" border size="small" class="mt-2">
              <el-descriptions-item label="GEO Score">{{ parsedPreview?.geoScore ?? '—' }}</el-descriptions-item>
              <el-descriptions-item v-if="parsedPreview?.probe_mode" label="探针模式">
                {{ parsedPreview.probe_mode }}
              </el-descriptions-item>
              <el-descriptions-item v-if="parsedPreview?.region" label="Region">{{ parsedPreview.region }}</el-descriptions-item>
              <el-descriptions-item v-if="parsedPreview?.sampled_at" label="采样时间">
                {{ formatTime(parsedPreview.sampled_at) }}
              </el-descriptions-item>
            </el-descriptions>
            <el-button
              v-if="diagnosticRunId(previewDetail)"
              link
              type="primary"
              class="mt-2"
              @click="goDiagnosticDetail(previewDetail)"
            >
              查看完整诊断结果
            </el-button>
          </template>

          <el-alert type="info" :closable="false" show-icon class="mt-3" :title="WEEKLY_REPORT_DISCLAIMER" />
        </template>
      </div>

      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button
          v-if="previewDetail"
          type="primary"
          plain
          :loading="exportingKey === `${previewDetail.id}-docx`"
          @click="handleExport(previewDetail, 'docx')"
        >
          下载 DOCX
        </el-button>
        <el-button
          v-if="previewDetail"
          type="primary"
          :loading="exportingKey === `${previewDetail.id}-pdf`"
          @click="handleExport(previewDetail, 'pdf')"
        >
          下载 PDF
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="ReportsList" lang="ts">
import { createWeeklyReport, downloadReport, getReport, listReports } from '@/api/tourgeo/report';
import type { ReportDetailVo, ReportSummary, ReportVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  REPORT_TYPE_OPTIONS,
  WEEKLY_REPORT_DISCLAIMER,
  defaultWeeklyRange,
  isoWeekLabel,
  reportTypeMeta
} from '@/constants/report';
import { useProjectStore } from '@/store/modules/project';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const reportList = ref<ReportVo[]>([]);
const total = ref(0);
const createdAtRange = ref<string[]>([]);

const weeklyVisible = ref(false);
const weeklySubmitting = ref(false);
const weeklyDateRange = ref<string[]>(defaultWeeklyRange());

const previewVisible = ref(false);
const previewLoading = ref(false);
const previewDetail = ref<ReportDetailVo | null>(null);
const exportingKey = ref('');

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  type: '' as '' | ReportVo['type'],
  period: ''
});

const projectId = computed(() => {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) return fromRoute;
  return projectStore.currentProjectId;
});

const currentProject = computed(() => {
  const id = projectId.value;
  return projectStore.projects.find((p) => p.id === id) ?? projectStore.currentProject;
});

const marketOptions = computed(() => currentProject.value?.targetMarkets ?? []);

const weeklyPeriodLabel = computed(() => {
  const end = weeklyDateRange.value?.[1];
  if (!end) return '—';
  return isoWeekLabel(new Date(`${end}T12:00:00`));
});

const clientDateFilterActive = computed(() => (createdAtRange.value?.length ?? 0) === 2);

const hasActiveFilters = computed(
  () => Boolean(queryParams.type || queryParams.period || clientDateFilterActive.value)
);

const emptyDescription = computed(() => (hasActiveFilters.value ? '未找到匹配报告' : '暂无报告'));

const displayList = computed(() => {
  let rows = reportList.value;
  if (createdAtRange.value?.length === 2) {
    const [start, end] = createdAtRange.value;
    const startMs = new Date(`${start}T00:00:00`).getTime();
    const endMs = new Date(`${end}T23:59:59`).getTime();
    rows = rows.filter((r) => {
      if (!r.createdAt) return false;
      const t = new Date(r.createdAt).getTime();
      return t >= startMs && t <= endMs;
    });
  }
  return rows;
});

const previewTitle = computed(() => {
  if (!previewDetail.value) return '报告预览';
  const meta = typeMeta(previewDetail.value.type);
  const suffix = previewDetail.value.period ? ` · ${previewDetail.value.period}` : '';
  return `报告预览 · ${meta.label}${suffix}`;
});

const parsedPreview = computed(() => (previewDetail.value ? parseSummary(previewDetail.value.summary) : null));

function typeMeta(type: string) {
  return reportTypeMeta(type);
}

function parseSummary(raw: string | ReportSummary | undefined): ReportSummary | null {
  if (!raw) return null;
  if (typeof raw === 'object') return raw;
  try {
    return JSON.parse(raw) as ReportSummary;
  } catch {
    return null;
  }
}

function diagnosticRunId(row: ReportVo): number | null {
  const summary = parseSummary(row.summary);
  if (summary?.runId) return Number(summary.runId);
  if (row.type === 'DIAGNOSTIC' && row.period) {
    const n = Number(row.period);
    if (!Number.isNaN(n) && n > 0) return n;
  }
  return null;
}

function summaryLine(row: ReportVo): string {
  if (row.summaryPreview?.trim()) return row.summaryPreview.trim();
  const s = parseSummary(row.summary);
  if (!s) {
    if (typeof row.summary === 'string' && row.summary) {
      const t = row.summary.trim();
      return t.length > 80 ? `${t.slice(0, 80)}…` : t;
    }
    return '—';
  }
  if (row.type === 'DIAGNOSTIC') {
    const parts = [`GEO Score ${s.geoScore ?? '—'}`];
    if (s.region) parts.push(s.region);
    return parts.join(' · ');
  }
  if (row.type === 'WEEKLY') {
    const parts: string[] = [];
    if (s.leads?.newCount != null) parts.push(`询盘 ${s.leads.newCount}`);
    if (s.keywords?.newCount != null) parts.push(`新词 ${s.keywords.newCount}`);
    if (s.geo?.delta != null) parts.push(`GEO ${s.geo.delta >= 0 ? '+' : ''}${s.geo.delta}`);
    else if (s.geo?.latestScore != null) parts.push(`GEO ${s.geo.latestScore}`);
    return parts.length ? parts.join(' · ') : row.period || '—';
  }
  return row.period || '—';
}

function formatTime(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function syncProjectFromRoute() {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) {
    projectStore.setCurrentProject(fromRoute);
  }
}

function initFiltersFromQuery() {
  const type = route.query.type;
  if (typeof type === 'string' && type) {
    queryParams.type = type as ReportVo['type'];
  }
}

async function getList() {
  const pid = projectId.value;
  if (!pid) {
    reportList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listReports(pid, {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      type: queryParams.type || undefined,
      period: queryParams.period || undefined
    });
    reportList.value = res.rows;
    total.value = res.total;
  } catch {
    reportList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.type = '';
  queryParams.period = '';
  queryParams.pageNum = 1;
  createdAtRange.value = [];
  router.replace({ query: { ...route.query, type: undefined } });
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectReports' || route.name === 'ReportsList') {
    router.replace({ name: 'ProjectReports', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function goDiagnostics() {
  router.push({ name: 'DiagnosticRuns' });
}

function goDiagnosticDetail(row: ReportVo) {
  const runId = diagnosticRunId(row);
  if (!runId) return;
  router.push({ name: 'DiagnosticDetail', params: { runId } });
}

function openWeeklyDialog() {
  weeklyDateRange.value = defaultWeeklyRange();
  weeklyVisible.value = true;
}

function resetWeeklyForm() {
  weeklyDateRange.value = defaultWeeklyRange();
}

async function submitWeekly() {
  const pid = projectId.value;
  if (!pid) {
    ElMessage.warning('请先选择项目');
    return;
  }
  if (!weeklyDateRange.value || weeklyDateRange.value.length !== 2) {
    ElMessage.warning('请选择统计区间');
    return;
  }
  const [periodStart, periodEnd] = weeklyDateRange.value;
  if (periodEnd < periodStart) {
    ElMessage.warning('结束日期不能早于开始日期');
    return;
  }
  const startMs = new Date(`${periodStart}T00:00:00`).getTime();
  const endMs = new Date(`${periodEnd}T00:00:00`).getTime();
  const days = Math.round((endMs - startMs) / 86400000) + 1;
  if (days > 31) {
    ElMessage.warning('统计区间不能超过 31 天');
    return;
  }
  weeklySubmitting.value = true;
  try {
    const reportId = await createWeeklyReport(pid, { periodStart, periodEnd });
    ElMessage.success('周报已生成');
    weeklyVisible.value = false;
    await getList();
    if (reportId > 0) {
      const row = reportList.value.find((r) => r.id === reportId);
      if (row) {
        openPreview(row);
      }
    }
  } catch (e) {
    const msg = e instanceof Error ? e.message : '生成失败';
    ElMessage.error(msg);
  } finally {
    weeklySubmitting.value = false;
  }
}

async function openPreview(row: ReportVo) {
  const pid = projectId.value;
  if (!pid) return;
  previewVisible.value = true;
  previewLoading.value = true;
  previewDetail.value = null;
  try {
    previewDetail.value = await getReport(pid, row.id);
  } catch {
    ElMessage.error('报告不存在或无权访问');
    previewVisible.value = false;
  } finally {
    previewLoading.value = false;
  }
}

function resetPreview() {
  previewDetail.value = null;
}

function exportFilename(row: ReportVo, format: 'docx' | 'pdf'): string {
  const meta = typeMeta(row.type);
  const base = `${meta.label}-${row.period || row.id}`.replace(/[\\/:*?"<>|]/g, '-');
  return `${base}.${format === 'docx' ? 'docx' : 'pdf'}`;
}

async function handleExport(row: ReportVo, format: 'docx' | 'pdf') {
  const pid = projectId.value;
  if (!pid) return;
  const key = `${row.id}-${format}`;
  exportingKey.value = key;
  try {
    await downloadReport(pid, row.id, format, exportFilename(row, format));
    ElMessage.success(format === 'docx' ? 'DOCX 报告已下载' : 'PDF 报告已下载');
  } catch (e) {
    const msg = e instanceof Error ? e.message : '导出失败';
    ElMessage.error(msg);
  } finally {
    if (exportingKey.value === key) exportingKey.value = '';
  }
}

watch(
  () => route.params.projectId,
  () => {
    syncProjectFromRoute();
    queryParams.pageNum = 1;
    getList();
  }
);

watch(
  () => projectStore.currentProjectId,
  (id, prev) => {
    if (route.params.projectId) return;
    if (id != null && id !== prev) getList();
  }
);

onMounted(async () => {
  if (!projectStore.initialized) {
    await projectStore.fetchProjects();
  }
  syncProjectFromRoute();
  initFiltersFromQuery();
  await getList();
});
</script>

<style scoped lang="scss">
.tg-reports {
  .project-card {
    background: var(--tg-color-primary-light, #e8f4fa);

    &__row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--tg-space-2, 8px);
    }

    &__label {
      font-size: var(--tg-font-size-sm, 13px);
      color: var(--tg-color-text-secondary, #6b7280);
    }

    &__name {
      font-weight: 600;
      color: var(--tg-color-text-primary, #1f2937);
    }
  }

  .toolbar-row {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: var(--tg-space-3, 12px);
  }

  .toolbar-form {
    flex: 1;
    min-width: 280px;
    margin-bottom: 0;

    :deep(.el-form-item) {
      margin-bottom: 0;
    }
  }

  .filter-hint,
  .page-hint {
    margin: var(--tg-space-2, 8px) 0 0;
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .empty-sub {
    margin: 0 0 12px;
    font-size: 13px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .summary-link {
    padding: 0;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .preview-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  .preview-range,
  .preview-time {
    font-size: 14px;
    color: var(--tg-color-text-regular, #4b5563);
  }

  .kpi-row {
    margin-bottom: 12px;
  }

  .kpi-card {
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    text-align: center;
  }

  .kpi-label {
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .kpi-value {
    font-size: 20px;
    font-weight: 600;
    margin-top: 4px;
  }

  .kpi-delta {
    font-size: 14px;
    margin-left: 4px;
    color: var(--el-color-success);
  }

  .section-title {
    margin: 16px 0 8px;
    font-size: 14px;
    font-weight: 600;
  }

  @media (max-width: 767px) {
    :deep(.report-preview-drawer) {
      width: 100% !important;
    }
  }
}
</style>
