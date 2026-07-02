<template>
  <div class="p-2 tg-keywords">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">机会词列表</h1>
        <p class="tg-page-sub">关键词洞察 · AI 生成海外机会词（FR-201/202）</p>
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
          <span v-if="currentProject?.brandName" class="project-card__brand">{{ currentProject.brandName }}</span>
        </div>
      </el-card>

      <el-card shadow="hover" class="mb-3 toolbar-card">
        <div class="toolbar-row">
          <el-button type="primary" icon="MagicStick" :loading="generating" :disabled="generating || scoringBatch" @click="openGenerateConfirm">
            AI 生成机会词
          </el-button>
          <el-button icon="RefreshRight" :loading="scoringBatch" :disabled="generating || scoringBatch" @click="openBatchScoreConfirm">
            刷新评分
          </el-button>
          <el-form :inline="true" class="toolbar-form" @submit.prevent="handleQuery">
            <el-form-item label="市场">
              <el-select v-model="queryParams.market" placeholder="全部" clearable style="width: 100px">
                <el-option label="全部" value="" />
                <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="queryParams.keyword" placeholder="模糊搜索" clearable style="width: 180px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" :disabled="generating" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" :disabled="generating" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-alert
        v-if="generating"
        type="info"
        show-icon
        :closable="false"
        title="生成任务进行中，请勿关闭页面"
        class="mb-3"
      />

      <el-card shadow="hover">
        <el-tabs
          v-model="activeStage"
          type="card"
          class="stage-tabs"
          :before-leave="() => !generating"
          @tab-change="handleStageChange"
        >
          <el-tab-pane
            v-for="tab in stageTabs"
            :key="tab.value || ALL_STAGE_TAB"
            :label="tab.label"
            :name="tab.value || ALL_STAGE_TAB"
          />
        </el-tabs>

        <el-table
          v-loading="loading"
          :element-loading-text="generating ? 'AI 正在生成关键词，约 30–90 秒…' : '加载中…'"
          border
          :data="keywordList"
          class="keyword-table"
        >
          <template #empty>
            <el-empty :description="emptyDescription">
              <el-button v-if="showGenerateInEmpty" type="primary" :loading="generating" @click="openGenerateConfirm">
                AI 生成机会词
              </el-button>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="关键词" prop="keyword" min-width="200" show-overflow-tooltip />
          <el-table-column label="英文" prop="keywordEn" min-width="180" show-overflow-tooltip class-name="hidden-sm-only">
            <template #default="{ row }">{{ row.keywordEn || '—' }}</template>
          </el-table-column>
          <el-table-column label="中文释义" prop="keywordCn" min-width="160" show-overflow-tooltip class-name="hidden-md-only">
            <template #default="{ row }">{{ row.keywordCn || '—' }}</template>
          </el-table-column>
          <el-table-column label="阶段" width="100" align="center">
            <template #default="{ row }">
              <el-tooltip v-if="stageTooltip(row.stage)" :content="stageTooltip(row.stage)" placement="top">
                <el-tag size="small">{{ stageLabel(row.stage) }}</el-tag>
              </el-tooltip>
              <el-tag v-else size="small">{{ stageLabel(row.stage) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="市场" prop="market" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.market }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="机会分" width="100" align="center">
            <template #default="{ row }">
              <span v-if="!hasKeywordScore(row.score)" class="score-placeholder">—</span>
              <el-tooltip v-else :content="formatScoreDetailTooltip(row.scoreDetailJson)" placement="top">
                <span :class="['score-value', scoreColorClass(row.score)]">{{ formatScore(row.score) }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160" class-name="hidden-md-only">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :loading="scoringKeywordIds.has(row.id)"
                :disabled="generating || scoringBatch"
                @click="handleScoreRow(row)"
              >
                评分
              </el-button>
              <el-button link type="danger" :disabled="generating || scoringBatch" @click="handleDelete(row)">删除</el-button>
              <el-tooltip content="创建内容任务 FR-205" placement="top">
                <el-button link type="primary" :disabled="generating || scoringBatch" @click="goCreateContent(row)">创建内容</el-button>
              </el-tooltip>
              <el-tooltip content="转落地页 FR-205" placement="top">
                <el-button link type="primary" :disabled="generating || scoringBatch" @click="goCreateLanding(row)">转落地页</el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :total="total"
          :disabled="generating"
          @pagination="getList"
        />
      </el-card>
    </template>
  </div>
</template>

<script setup name="KeywordsList" lang="ts">
import { deleteKeyword, generateKeywords, listKeywords, scoreKeyword, scoreKeywordsBatch } from '@/api/tourgeo/keyword';
import type { KeywordOpportunityVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  formatScoreDetailTooltip,
  hasKeywordScore,
  KEYWORD_STAGE_TABS,
  LIFECYCLE_STAGE_KEYS,
  LIFECYCLE_STAGE_LABELS,
  LIFECYCLE_STAGE_TOOLTIPS,
  scoreColorClass,
  type LifecycleStage
} from '@/constants/keyword';
import { ENTITY_STATUS_META } from '@/constants/project';
import { useProjectStore } from '@/store/modules/project';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const stageTabs = KEYWORD_STAGE_TABS;
const loading = ref(false);
const generating = ref(false);
const scoringBatch = ref(false);
const scoringKeywordIds = ref<Set<number>>(new Set());

function setRowScoring(keywordId: number, scoring: boolean) {
  const next = new Set(scoringKeywordIds.value);
  if (scoring) {
    next.add(keywordId);
  } else {
    next.delete(keywordId);
  }
  scoringKeywordIds.value = next;
}
const keywordList = ref<KeywordOpportunityVo[]>([]);
const total = ref(0);
const ALL_STAGE_TAB = 'all';
const activeStage = ref<string>(ALL_STAGE_TAB);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  market: '',
  keyword: '',
  status: 'ACTIVE' as const,
  orderByColumn: 'score',
  isAsc: 'desc'
});

const currentProject = computed(() => {
  const id = projectId.value;
  return projectStore.projects.find((p) => p.id === id) ?? projectStore.currentProject;
});

const marketOptions = computed(() => currentProject.value?.targetMarkets ?? []);

const projectId = computed(() => {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) return fromRoute;
  return projectStore.currentProjectId;
});

const hasActiveFilters = computed(
  () => Boolean(queryParams.market || queryParams.keyword || (activeStage.value && activeStage.value !== ALL_STAGE_TAB))
);

const emptyDescription = computed(() => {
  if (hasActiveFilters.value) return '未找到匹配关键词';
  if (activeStage.value && activeStage.value !== ALL_STAGE_TAB) return '该阶段暂无词条';
  return '暂无关键词机会词';
});

const showGenerateInEmpty = computed(() => !hasActiveFilters.value || activeStage.value !== ALL_STAGE_TAB);

function stageQueryValue(): string | undefined {
  return activeStage.value === ALL_STAGE_TAB ? undefined : activeStage.value;
}

function stageLabel(stage: string): string {
  return LIFECYCLE_STAGE_LABELS[stage as LifecycleStage] ?? stage;
}

function stageTooltip(stage: string): string | undefined {
  return LIFECYCLE_STAGE_TOOLTIPS[stage as LifecycleStage];
}

function statusMeta(status: string) {
  return ENTITY_STATUS_META[status] ?? { label: status, type: 'info' as const };
}

function formatScore(score: number | string | null | undefined): string {
  if (!hasKeywordScore(score)) return '—';
  return Number(score).toFixed(1);
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

function initStageFromQuery() {
  const raw = route.query.stage;
  const stage = Array.isArray(raw) ? raw[0] : raw;
  if (stage && LIFECYCLE_STAGE_KEYS.includes(stage as LifecycleStage)) {
    activeStage.value = stage;
  } else {
    activeStage.value = ALL_STAGE_TAB;
  }
}

async function getList() {
  const pid = projectId.value;
  if (!pid) {
    keywordList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listKeywords(pid, {
      ...queryParams,
      stage: stageQueryValue()
    });
    keywordList.value = res.rows;
    total.value = res.total;
  } catch {
    keywordList.value = [];
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
  queryParams.market = '';
  queryParams.keyword = '';
  queryParams.pageNum = 1;
  activeStage.value = ALL_STAGE_TAB;
  router.replace({ query: { ...route.query, stage: undefined } });
  getList();
}

function handleStageChange(name: string | number) {
  activeStage.value = String(name);
  queryParams.pageNum = 1;
  const q = { ...route.query };
  if (activeStage.value !== ALL_STAGE_TAB) {
    q.stage = activeStage.value;
  } else {
    delete q.stage;
  }
  router.replace({ query: q });
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectKeywords') {
    router.replace({ name: 'ProjectKeywords', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function defaultMarket(): string {
  const markets = marketOptions.value;
  return markets[0] ?? 'US';
}

function marketsLabel(): string {
  const markets = marketOptions.value;
  return markets.length ? markets.join('、') : defaultMarket();
}

async function openGenerateConfirm() {
  const pid = projectId.value;
  if (!pid) {
    ElMessage.warning('请先选择项目');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `将为当前项目生成各生命周期阶段的推荐关键词（FR-201/202）。\n\n` +
        `目标市场：${marketsLabel()}（本次按 ${defaultMarket()} 生成）\n` +
        `预计每阶段约 5 条；已有词条默认保留，新生成追加。\n\n` +
        `⚠ 消耗套餐「关键词/月」额度（若计费已启用）。`,
      'AI 生成机会词',
      {
        confirmButtonText: '开始生成',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: false
      }
    );
    await runGenerate(pid);
  } catch {
    /* cancelled */
  }
}

async function runGenerate(pid: number) {
  generating.value = true;
  try {
    const result = await generateKeywords(pid, {
      market: defaultMarket(),
      locale: 'en',
      stages: [...LIFECYCLE_STAGE_KEYS],
      wordsPerStage: 5,
      useRag: false
    });
    const count = result.insertedCount ?? 0;
    ElMessage.success(`已生成 ${count} 条关键词`);
    activeStage.value = ALL_STAGE_TAB;
    router.replace({ query: { ...route.query, stage: undefined } });
    await getList();
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg.includes('402') || msg.includes('额度')) {
      ElMessage.warning('关键词额度不足，请联系管理员');
    } else if (msg && msg !== 'error') {
      ElMessage.error(msg);
    }
  } finally {
    generating.value = false;
  }
}

async function openBatchScoreConfirm() {
  const pid = projectId.value;
  if (!pid) {
    ElMessage.warning('请先选择项目');
    return;
  }
  const filterHint =
    activeStage.value !== ALL_STAGE_TAB || queryParams.market || queryParams.keyword
      ? '当前 Tab/筛选条件下'
      : '当前项目 ACTIVE 词条';
  try {
    await ElMessageBox.confirm(
      `将对${filterHint}最多 50 条关键词刷新机会分（FR-203 五维加权）。\n\n已有评分将被覆盖，无评分词条将排在列表末尾。`,
      '刷新评分',
      {
        confirmButtonText: '开始评分',
        cancelButtonText: '取消',
        type: 'info'
      }
    );
    await runBatchScore(pid);
  } catch {
    /* cancelled */
  }
}

async function runBatchScore(pid: number) {
  scoringBatch.value = true;
  try {
    const hasFilters = Boolean(stageQueryValue() || queryParams.market || queryParams.keyword);
    let keywordIds: number[] | undefined;
    if (hasFilters) {
      const res = await listKeywords(pid, {
        pageNum: 1,
        pageSize: 50,
        market: queryParams.market,
        keyword: queryParams.keyword,
        status: queryParams.status,
        stage: stageQueryValue(),
        orderByColumn: queryParams.orderByColumn,
        isAsc: queryParams.isAsc
      });
      keywordIds = res.rows.map((row) => row.id).filter((id) => id > 0);
      if (!keywordIds.length) {
        ElMessage.warning('没有可评分的关键词');
        return;
      }
    }
    const result = await scoreKeywordsBatch(pid, { keywordIds, useRag: false });
    const count = result.scoredCount ?? result.results?.length ?? 0;
    ElMessage.success(`已刷新 ${count} 条关键词评分`);
    await getList();
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') {
      ElMessage.error(msg);
    }
  } finally {
    scoringBatch.value = false;
  }
}

async function handleScoreRow(row: KeywordOpportunityVo) {
  const pid = projectId.value;
  if (!pid || !row.id) return;
  setRowScoring(row.id, true);
  try {
    const result = await scoreKeyword(pid, row.id, { useRag: false });
    ElMessage.success(`「${row.keyword}」机会分 ${formatScore(result.score)}`);
    await getList();
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') {
      ElMessage.error(msg);
    }
  } finally {
    setRowScoring(row.id, false);
  }
}

function goCreateContent(row: KeywordOpportunityVo) {
  const pid = projectId.value;
  if (!pid) {
    ElMessage.warning('请先选择项目');
    return;
  }
  router.push({
    name: 'ProjectContentTasks',
    params: { projectId: pid },
    query: { action: 'create', keywordId: String(row.id) }
  });
}

function goCreateLanding(row: KeywordOpportunityVo) {
  const pid = projectId.value;
  if (!pid) {
    ElMessage.warning('请先选择项目');
    return;
  }
  router.push({
    name: 'ProjectLandingPages',
    params: { projectId: pid },
    query: { action: 'create', keywordId: String(row.id) }
  });
}

async function handleDelete(row: KeywordOpportunityVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(`确定删除关键词「${row.keyword}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteKeyword(pid, row.id);
    ElMessage.success('已删除');
    await getList();
  } catch {
    /* cancelled or failed */
  }
}

watch(
  () => route.params.projectId,
  () => {
    syncProjectFromRoute();
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
  initStageFromQuery();
  await getList();
});
</script>

<style scoped lang="scss">
.tg-keywords {
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

    &__brand {
      font-size: var(--tg-font-size-sm, 13px);
      color: var(--tg-color-text-regular, #4b5563);
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

  .stage-tabs {
    margin-bottom: var(--tg-space-3, 12px);

    :deep(.el-tabs__header) {
      margin-bottom: 0;
    }
  }

  .score-value {
    font-weight: 600;

    &.score-high {
      color: var(--tg-score-high, #059669);
    }

    &.score-mid {
      color: var(--tg-score-mid, #d97706);
    }

    &.score-low {
      color: var(--tg-score-low, #dc2626);
    }
  }

  .score-placeholder {
    color: var(--tg-color-text-placeholder, #9ca3af);
  }
}

@media (max-width: 768px) {
  .tg-keywords :deep(.hidden-md-only) {
    display: none;
  }
}

@media (max-width: 992px) {
  .tg-keywords :deep(.hidden-sm-only) {
    display: none;
  }
}
</style>
