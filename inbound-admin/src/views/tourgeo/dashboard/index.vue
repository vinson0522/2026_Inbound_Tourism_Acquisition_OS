<template>
  <div class="p-2 tg-dashboard">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">工作台</h1>
        <p class="tg-page-sub">项目健康度与待办一屏掌握</p>
      </div>
      <div class="tg-page-header__actions">
        <project-selector @change="loadData" />
        <el-button type="primary" icon="Plus" @click="goCreateDiagnostic">发起 GEO 诊断</el-button>
      </div>
    </div>

    <el-skeleton v-if="pageLoading" :rows="8" animated />

    <template v-else-if="!projectStore.hasProject">
      <el-empty description="暂无客户项目，请先创建">
        <el-button type="primary" @click="router.push({ path: '/projects/index', query: { create: '1' } })">新建客户项目</el-button>
      </el-empty>
    </template>

    <template v-else>
      <el-row :gutter="20" class="mb-[10px]">
        <el-col v-for="card in kpiCards" :key="card.key" :xs="24" :md="12" :lg="6">
          <el-card shadow="hover" class="kpi-card">
            <div class="kpi-card__label">{{ card.label }}</div>
            <div class="kpi-card__value" :style="card.valueStyle">{{ card.display }}</div>
            <div v-if="card.hint" class="kpi-card__hint">{{ card.hint }}</div>
            <el-tag v-if="card.trend != null" size="small" :type="card.trend >= 0 ? 'success' : 'danger'" class="kpi-card__trend">
              {{ card.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(card.trend) }} 较上周
            </el-tag>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="mb-[10px]">
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header><span>今日任务</span></template>
            <el-empty v-if="!data?.tasks.length" description="暂无待办" />
            <ul v-else class="task-list">
              <li v-for="task in data.tasks" :key="task.id">{{ task.title }}</li>
            </ul>
            <el-button v-if="data?.tasks.length" link type="primary">查看全部任务</el-button>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="hover">
            <template #header><span>预警中心</span></template>
            <el-empty v-if="!data?.alerts.length" description="暂无预警" />
            <div v-else class="alert-stack">
              <el-alert
                v-for="alert in data.alerts"
                :key="alert.id"
                :title="alert.message"
                :type="alert.level"
                show-icon
                :closable="false"
              />
            </div>
            <el-button v-if="data?.alerts.length" link type="primary">查看全部预警</el-button>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" class="mb-[10px]">
        <template #header><span>本周建议</span></template>
        <p class="suggestion-text">{{ data?.suggestion.summary }}</p>
        <el-button-group>
          <el-button plain>转为内容任务</el-button>
          <el-button plain>转为落地页任务</el-button>
          <el-button plain>忽略</el-button>
        </el-button-group>
      </el-card>

      <el-card shadow="hover" class="mb-[10px]">
        <template #header><span>转化漏斗摘要</span></template>
        <el-empty description="数据接入后展示曝光 → 点击 → 表单 → 有效线索" />
      </el-card>

      <el-card shadow="hover">
        <template #header>
          <div class="card-header-row">
            <span>最近诊断</span>
            <el-button link type="primary" @click="router.push('/diagnostics/runs')">查看全部</el-button>
          </div>
        </template>
        <el-table v-loading="tableLoading" :data="data?.recentRuns ?? []" border empty-text="暂无诊断记录">
          <el-table-column label="任务名称" prop="name" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <diagnostic-status-tag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="GEO 分" width="90" align="center">
            <template #default="{ row }">
              <geo-score-display :score="row.geoScore" />
            </template>
          </el-table-column>
          <el-table-column label="完成时间" prop="finishedAt" width="170">
            <template #default="{ row }">{{ row.finishedAt || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default>
              <el-button link type="primary">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup name="TourgeoDashboard" lang="ts">
import { getDashboard, type DashboardData } from '@/api/tourgeo/diagnostic';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import DiagnosticStatusTag from '@/components/tourgeo/DiagnosticStatusTag.vue';
import GeoScoreDisplay from '@/components/tourgeo/GeoScoreDisplay.vue';
import { useProjectStore } from '@/store/modules/project';

const router = useRouter();
const projectStore = useProjectStore();
const pageLoading = ref(true);
const tableLoading = ref(false);
const data = ref<DashboardData | null>(null);

const kpiCards = computed(() => {
  const k = data.value?.kpi;
  const score = k?.geoScore;
  const scoreColor =
    score == null ? undefined : score >= 70 ? 'var(--tg-score-high)' : score >= 40 ? 'var(--tg-score-mid)' : 'var(--tg-score-low)';
  return [
    {
      key: 'geo',
      label: 'GEO 可见率',
      display: score != null ? score.toFixed(1) : '—',
      hint: k?.geoScoreDate ? `最近诊断 ${k.geoScoreDate}` : undefined,
      valueStyle: { color: scoreColor, fontSize: 'var(--tg-font-size-xl)' },
      trend: null
    },
    {
      key: 'content',
      label: '内容产出（本周）',
      display: String(k?.contentCount ?? 0),
      trend: k?.contentTrend ?? 0,
      valueStyle: { fontSize: 'var(--tg-font-size-xl)' }
    },
    {
      key: 'landing',
      label: '落地页（已发布）',
      display: String(k?.landingPageCount ?? 0),
      trend: k?.landingTrend ?? 0,
      valueStyle: { fontSize: 'var(--tg-font-size-xl)' }
    },
    {
      key: 'lead',
      label: '线索（本周）',
      display: String(k?.leadCount ?? 0),
      trend: k?.leadTrend ?? 0,
      valueStyle: { fontSize: 'var(--tg-font-size-xl)' }
    }
  ];
});

async function loadData() {
  if (!projectStore.currentProjectId) {
    data.value = null;
    pageLoading.value = false;
    return;
  }
  tableLoading.value = true;
  try {
    data.value = await getDashboard(projectStore.currentProjectId);
  } finally {
    tableLoading.value = false;
    pageLoading.value = false;
  }
}

function goCreateDiagnostic() {
  router.push({ path: '/diagnostics/runs', query: { create: '1' } });
}

onMounted(async () => {
  if (!projectStore.initialized) {
    await projectStore.fetchProjects();
  }
  await loadData();
});
</script>

<style scoped lang="scss">
.tg-dashboard {
  color: var(--tg-color-text-regular);
}

.tg-page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--tg-space-4);
  margin-bottom: var(--tg-space-5);

  &__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tg-space-3);
  }
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

.kpi-card {
  background: var(--tg-color-bg-surface);

  &__label {
    font-size: var(--tg-font-size-sm);
    color: var(--tg-color-text-secondary);
  }

  &__value {
    margin-top: var(--tg-space-2);
    font-weight: 600;
    color: var(--tg-color-text-primary);
  }

  &__hint {
    margin-top: var(--tg-space-1);
    font-size: var(--tg-font-size-xs);
    color: var(--tg-color-text-secondary);
  }

  &__trend {
    margin-top: var(--tg-space-2);
  }
}

.task-list {
  margin: 0 0 var(--tg-space-3);
  padding-left: var(--tg-space-5);
  color: var(--tg-color-text-regular);

  li + li {
    margin-top: var(--tg-space-2);
  }
}

.alert-stack {
  display: flex;
  flex-direction: column;
  gap: var(--tg-space-2);
  margin-bottom: var(--tg-space-3);
}

.suggestion-text {
  margin: 0 0 var(--tg-space-4);
  line-height: 1.6;
  color: var(--tg-color-text-regular);
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
