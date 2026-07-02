<template>
  <div class="p-2 tg-diagnostics">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">诊断任务</h1>
        <p class="tg-page-sub">GEO 诊断 · 列表与新建</p>
      </div>
      <project-selector @change="handleProjectChange" />
    </div>

    <p class="compliance-hint">诊断结果基于采样时刻的 AI 回答，仅供参考；不承诺排名保证。</p>

    <el-skeleton v-if="!projectStore.initialized && projectStore.loading" :rows="6" animated />

    <el-empty v-else-if="!projectStore.hasProject" description="请先选择客户项目">
      <project-selector :show-label="false" />
    </el-empty>

    <template v-else>
      <el-tabs v-model="activePageTab" class="page-tabs" @tab-change="onPageTabChange">
        <el-tab-pane label="诊断任务" name="runs" />
        <el-tab-pane label="定时计划" name="schedule" />
      </el-tabs>

      <template v-if="activePageTab === 'runs'">
      <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
        <div v-show="showSearch" class="mb-[10px]">
          <el-card shadow="hover">
            <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="80px">
              <el-form-item label="任务名称" prop="name">
                <el-input v-model="queryParams.name" placeholder="模糊搜索" clearable @keyup.enter="handleQuery" />
              </el-form-item>
              <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
                  <el-option v-for="opt in statusOptions" :key="opt.value || 'all'" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="市场" prop="market">
                <el-select v-model="queryParams.market" placeholder="全部" clearable style="width: 120px">
                  <el-option label="全部" value="" />
                  <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
                </el-select>
              </el-form-item>
              <el-form-item label="探针模式" prop="probeMode">
                <el-select v-model="queryParams.probeMode" placeholder="全部" clearable style="width: 160px">
                  <el-option v-for="opt in probeModeOptions" :key="opt.value || 'all'" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="创建时间" prop="createdAt">
                <el-date-picker
                  v-model="queryParams.createdAt"
                  type="daterange"
                  range-separator="-"
                  start-placeholder="开始"
                  end-placeholder="结束"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </transition>

      <el-card shadow="hover">
        <template #header>
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" icon="Plus" @click="openDrawer">新建诊断任务</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button plain icon="Download" disabled>导出</el-button>
            </el-col>
            <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
          </el-row>
        </template>

        <el-table v-loading="loading" border :data="runList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column label="任务名称" prop="name" min-width="180" show-overflow-tooltip />
          <el-table-column label="市场/语言" width="130" align="center">
            <template #default="{ row }">
              {{ row.market }}
              <el-tag size="small" class="ml-1">{{ row.locale }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="探针模式" width="150" align="center" class-name="hidden-xs-only">
            <template #default="{ row }">
              <el-tag
                v-for="mode in row.probeModes"
                :key="mode"
                size="small"
                :type="mode === 'grounded-api' ? 'success' : 'info'"
                class="mr-1"
              >
                {{ mode }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="AI 平台" width="140" align="center" class-name="hidden-sm-only">
            <template #default="{ row }">
              <el-tag v-for="m in row.models" :key="m" size="small" class="mr-1">{{ m }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="采样" prop="sampleCount" width="70" align="center" />
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <diagnostic-status-tag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="进度" width="120" align="center">
            <template #default="{ row }">
              <el-progress v-if="row.status === 'RUNNING'" :percentage="row.progress ?? 0" :stroke-width="8" />
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="GEO 分" width="90" align="center">
            <template #default="{ row }">
              <geo-score-display :score="row.geoScore" />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createdAt" width="170" />
          <el-table-column label="操作" fixed="right" width="200" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING' || row.status === 'RUNNING'"
                link
                type="primary"
                @click="goDetail(row)"
              >
                查看进度
              </el-button>
              <el-button
                v-if="row.status === 'SUCCESS' || row.status === 'PARTIAL_FAILED'"
                link
                type="primary"
                @click="goDetail(row)"
              >
                查看结果
              </el-button>
              <el-button v-if="row.status === 'FAILED'" link type="primary" @click="goDetail(row)">查看日志</el-button>
              <el-tooltip :content="row.runId" placement="top">
                <el-button link type="info" @click="copyRunId(row)">复制 ID</el-button>
              </el-tooltip>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无诊断任务">
              <el-button type="primary" @click="openDrawer">创建首次 GEO 诊断</el-button>
            </el-empty>
          </template>
        </el-table>

        <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-card>
      </template>

      <template v-else>
        <el-skeleton v-if="scheduleLoading" :rows="10" animated />

        <template v-else>
          <p class="schedule-sub">按周或按月自动创建 GEO 诊断任务；到点由系统触发，无需手工新建。</p>

          <el-alert
            v-if="!scheduleConfigured"
            type="info"
            show-icon
            :closable="false"
            title="尚未保存定时计划，配置后点击保存。"
            class="mb-3"
          />

          <el-card shadow="hover" class="schedule-card">
            <template #header>
              <span class="schedule-card__title">定时 GEO 诊断</span>
            </template>

            <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleRules" label-width="110px">
              <el-form-item label="启用定时计划">
                <el-switch v-model="scheduleForm.enabled" />
                <span class="field-hint ml-2">关闭后 Job 不触发，仍可保存参数</span>
              </el-form-item>

              <el-divider content-position="left">执行频率 (FR-109)</el-divider>
              <el-form-item label="执行频率" prop="frequency">
                <el-radio-group v-model="scheduleForm.frequency">
                  <el-radio value="WEEKLY">每周 WEEKLY</el-radio>
                  <el-radio value="MONTHLY">每月 MONTHLY</el-radio>
                </el-radio-group>
                <div class="field-hint">周：自上次成功触发起每 7 日 · 月：每自然月同日（无 31 日则月末）</div>
              </el-form-item>

              <el-divider content-position="left">诊断参数</el-divider>
              <el-form-item label="目标市场" prop="market">
                <el-select v-model="scheduleForm.market" placeholder="选择市场" style="width: 100%">
                  <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
                </el-select>
              </el-form-item>
              <el-form-item label="语言/地区" prop="locale">
                <el-select v-model="scheduleForm.locale" placeholder="选择语言" style="width: 100%">
                  <el-option label="en-US" value="en-US" />
                  <el-option label="en-GB" value="en-GB" />
                  <el-option label="en-AU" value="en-AU" />
                </el-select>
              </el-form-item>
              <el-form-item label="问题范围" prop="questionScope">
                <el-radio-group v-model="scheduleForm.questionScope">
                  <el-radio value="all">全部问题</el-radio>
                  <el-radio value="stage">按阶段</el-radio>
                  <el-radio value="custom">自定义数量</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="探针模式" prop="probeModes">
                <el-checkbox-group v-model="scheduleForm.probeModes">
                  <el-checkbox value="grounded-api" disabled>grounded-api（默认）</el-checkbox>
                  <el-checkbox value="browser-extension">browser-extension</el-checkbox>
                  <el-checkbox value="headless-automation">headless-automation</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="AI 平台" prop="models">
                <el-checkbox-group v-model="scheduleForm.models">
                  <el-checkbox v-for="p in platformOptions" :key="p" :value="p">{{ p }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="采样次数" prop="sampleCount">
                <el-input-number v-model="scheduleForm.sampleCount" :min="1" :max="10" />
              </el-form-item>
              <el-form-item label="校准比例">
                <el-tooltip content="需同时勾选 browser-extension 探针模式" :disabled="scheduleCalibrationEnabled">
                  <div class="calibration-slider">
                    <el-slider
                      v-model="scheduleForm.calibrationRatio"
                      :max="30"
                      :disabled="!scheduleCalibrationEnabled"
                      show-input
                    />
                  </div>
                </el-tooltip>
                <div class="field-hint">扩展探针校准采样比例（0–30%）· API vs 网页版重叠对比</div>
              </el-form-item>

              <el-divider content-position="left">执行状态</el-divider>
              <el-descriptions :column="1" border class="schedule-status">
                <el-descriptions-item label="下次执行">
                  <span v-if="!scheduleForm.enabled" class="text-muted">已暂停</span>
                  <span v-else-if="scheduleStatus.nextRunAt">{{ formatDateTime(scheduleStatus.nextRunAt) }}</span>
                  <span v-else class="text-muted">—</span>
                </el-descriptions-item>
                <el-descriptions-item label="最近触发">
                  <span v-if="scheduleStatus.lastTriggeredAt">{{ formatDateTime(scheduleStatus.lastTriggeredAt) }}</span>
                  <span v-else class="text-muted">—</span>
                </el-descriptions-item>
                <el-descriptions-item label="最近任务">
                  <el-link
                    v-if="scheduleStatus.lastRunId"
                    type="primary"
                    @click="goDetailById(scheduleStatus.lastRunId!)"
                  >
                    #{{ scheduleStatus.lastRunId }}
                    <template v-if="scheduleStatus.lastRunName"> {{ scheduleStatus.lastRunName }}</template>
                  </el-link>
                  <span v-else class="text-muted">暂无自动任务</span>
                </el-descriptions-item>
              </el-descriptions>

              <el-alert
                type="info"
                show-icon
                :closable="false"
                title="GEO 诊断使用联网检索采样结果，不承诺排名保证。报告将标注 probe_mode、sampled_at、参与平台。"
                class="drawer-alert"
              />
            </el-form>

            <el-card shadow="never" class="schedule-footnote">
              <p class="footnote-title">定时执行说明</p>
              <ul class="footnote-list">
                <li>系统 <strong>每小时</strong> 检查到期计划（Spring @Scheduled）；到点自动创建诊断任务。</li>
                <li><strong>每周</strong>：自上次成功触发起间隔 7 日；<strong>每月</strong>：每自然月相同日期（无该日则月末）。</li>
                <li>若当月 <strong>GEO 诊断额度不足</strong>，本次触发 <strong>跳过</strong> 并记录日志，不创建任务。</li>
                <li>诊断结果为采样时刻 AI 回答，<strong>不承诺排名保证</strong>；M3 <strong>不发送</strong> 邮件/企微通知。</li>
                <li>每项目仅支持 <strong>一条</strong> 定时计划；如需变更参数请编辑后保存。</li>
              </ul>
            </el-card>

            <div class="schedule-actions">
              <el-button v-hasPermi="['tourgeo:diagnostic:edit']" type="primary" :loading="scheduleSaving" @click="saveSchedule">
                保存计划
              </el-button>
            </div>
          </el-card>
        </template>
      </template>
    </template>

    <el-drawer v-model="drawerVisible" title="新建 GEO 诊断任务" size="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="目标市场" prop="market">
          <el-select v-model="form.market" placeholder="选择市场" style="width: 100%">
            <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言/地区" prop="locale">
          <el-select v-model="form.locale" placeholder="选择语言" style="width: 100%">
            <el-option label="en-US" value="en-US" />
            <el-option label="en-GB" value="en-GB" />
            <el-option label="en-AU" value="en-AU" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题范围" prop="questionScope">
          <el-radio-group v-model="form.questionScope">
            <el-radio value="all">全部问题</el-radio>
            <el-radio value="stage">按阶段</el-radio>
            <el-radio value="custom">自定义数量</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="探针模式" prop="probeModes">
          <el-checkbox-group v-model="form.probeModes">
            <el-checkbox value="grounded-api" disabled>grounded-api（默认）</el-checkbox>
            <el-checkbox value="browser-extension">browser-extension</el-checkbox>
            <el-checkbox value="headless-automation">headless-automation</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="AI 平台" prop="models">
          <el-checkbox-group v-model="form.models">
            <el-checkbox v-for="p in platformOptions" :key="p" :value="p">{{ p }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="采样次数" prop="sampleCount">
          <el-input-number v-model="form.sampleCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="校准比例">
          <el-tooltip content="需同时勾选 browser-extension 探针模式" :disabled="calibrationEnabled">
            <div class="calibration-slider">
              <el-slider
                v-model="form.calibrationRatio"
                :max="30"
                :disabled="!calibrationEnabled"
                show-input
              />
            </div>
          </el-tooltip>
          <div class="field-hint">扩展探针校准采样比例（0–30%）· API vs 网页版重叠对比</div>
        </el-form-item>
      </el-form>
      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="GEO 诊断使用联网检索采样结果，不承诺排名保证。报告将标注 probe_mode、sampled_at、参与平台。"
        class="drawer-alert"
      />
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">创建任务</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="DiagnosticRuns" lang="ts">
import { createDiagnosticRun, getDiagnosticSchedule, listDiagnosticRuns, upsertDiagnosticSchedule } from '@/api/tourgeo/diagnostic';
import type {
  CreateDiagnosticForm,
  DiagnosticRunQuery,
  DiagnosticRunVO,
  DiagnosticScheduleForm,
  DiagnosticScheduleVO
} from '@/api/tourgeo/types';
import { AI_PLATFORM_OPTIONS, DIAGNOSTIC_STATUS_OPTIONS, PROBE_MODE_OPTIONS } from '@/constants/diagnostic';
import { MARKET_OPTIONS } from '@/constants/project';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import DiagnosticStatusTag from '@/components/tourgeo/DiagnosticStatusTag.vue';
import GeoScoreDisplay from '@/components/tourgeo/GeoScoreDisplay.vue';
import { useProjectStore } from '@/store/modules/project';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const statusOptions = DIAGNOSTIC_STATUS_OPTIONS;
const probeModeOptions = PROBE_MODE_OPTIONS;
const platformOptions = AI_PLATFORM_OPTIONS;

const loading = ref(true);
const showSearch = ref(true);
const activePageTab = ref<'runs' | 'schedule'>('runs');
const runList = ref<DiagnosticRunVO[]>([]);
const total = ref(0);
const ids = ref<number[]>([]);
const drawerVisible = ref(false);
const submitLoading = ref(false);
const scheduleLoading = ref(false);
const scheduleSaving = ref(false);
const scheduleConfigured = ref(false);

const scheduleStatus = reactive({
  nextRunAt: null as string | null,
  lastTriggeredAt: null as string | null,
  lastRunId: null as number | null,
  lastRunName: null as string | null
});

const queryFormRef = ref<ElFormInstance>();
const formRef = ref<ElFormInstance>();
const scheduleFormRef = ref<ElFormInstance>();

const queryParams = reactive<DiagnosticRunQuery>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: '',
  market: '',
  probeMode: '',
  createdAt: undefined
});

const defaultForm = (): CreateDiagnosticForm => ({
  name: '',
  market: 'US',
  locale: 'en-US',
  region: 'US',
  questionScope: 'all',
  probeModes: ['grounded-api'],
  models: ['Gemini'],
  sampleCount: 1,
  calibrationRatio: 10
});

const form = reactive<CreateDiagnosticForm>(defaultForm());

const defaultScheduleForm = (): DiagnosticScheduleForm => ({
  enabled: false,
  frequency: 'WEEKLY',
  market: 'US',
  locale: 'en-US',
  region: 'US',
  questionScope: 'all',
  probeModes: ['grounded-api'],
  models: ['Gemini'],
  sampleCount: 3,
  calibrationRatio: 10
});

const scheduleForm = reactive<DiagnosticScheduleForm>(defaultScheduleForm());

const rules = ref<ElFormRules>({
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  market: [{ required: true, message: '请选择市场', trigger: 'change' }],
  locale: [{ required: true, message: '请选择语言', trigger: 'change' }],
  probeModes: [{ required: true, type: 'array', min: 1, message: '至少选择一种探针模式', trigger: 'change' }],
  models: [{ required: true, type: 'array', min: 1, message: '至少选择一个 AI 平台', trigger: 'change' }],
  sampleCount: [{ required: true, message: '请设置采样次数', trigger: 'change' }]
});

const scheduleRules = ref<ElFormRules>({
  frequency: [{ required: true, message: '请选择执行频率', trigger: 'change' }],
  market: [{ required: true, message: '请选择市场', trigger: 'change' }],
  locale: [{ required: true, message: '请选择语言', trigger: 'change' }],
  probeModes: [{ required: true, type: 'array', min: 1, message: '至少选择一种探针模式', trigger: 'change' }],
  models: [{ required: true, type: 'array', min: 1, message: '至少选择一个 AI 平台', trigger: 'change' }],
  sampleCount: [{ required: true, message: '请设置采样次数', trigger: 'change' }]
});

const marketOptions = computed(() => projectStore.currentProject?.targetMarkets ?? MARKET_OPTIONS);

const calibrationEnabled = computed(() => form.probeModes.includes('browser-extension'));
const scheduleCalibrationEnabled = computed(() => scheduleForm.probeModes.includes('browser-extension'));

async function getList() {
  if (!projectStore.currentProjectId) {
    runList.value = [];
    total.value = 0;
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const res = await listDiagnosticRuns(projectStore.currentProjectId, { ...queryParams });
    runList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryFormRef.value?.resetFields();
  queryParams.pageNum = 1;
  getList();
}

function handleSelectionChange(selection: DiagnosticRunVO[]) {
  ids.value = selection.map((s) => s.id);
}

function handleProjectChange() {
  resetFormDefaults();
  resetScheduleDefaults();
  handleQuery();
  if (activePageTab.value === 'schedule') {
    loadSchedule();
  }
}

function resetScheduleDefaults() {
  const p = projectStore.currentProject;
  Object.assign(scheduleForm, defaultScheduleForm(), {
    market: p?.targetMarkets[0] ?? 'US',
    locale: p?.defaultLocale ?? 'en-US',
    region: p?.targetMarkets[0] ?? 'US'
  });
  if (!scheduleForm.probeModes.includes('grounded-api')) {
    scheduleForm.probeModes = ['grounded-api', ...scheduleForm.probeModes];
  }
  scheduleConfigured.value = false;
  scheduleStatus.nextRunAt = null;
  scheduleStatus.lastTriggeredAt = null;
  scheduleStatus.lastRunId = null;
  scheduleStatus.lastRunName = null;
}

function applyScheduleVo(vo: DiagnosticScheduleVO) {
  scheduleConfigured.value = vo.configured ?? vo.id != null;
  scheduleForm.enabled = vo.enabled;
  scheduleForm.frequency = vo.frequency;
  scheduleForm.market = vo.market;
  scheduleForm.locale = vo.locale;
  scheduleForm.region = vo.region ?? vo.market;
  scheduleForm.probeModes = vo.probeModes?.length ? [...vo.probeModes] : ['grounded-api'];
  scheduleForm.models = vo.models?.length ? [...vo.models] : ['Gemini'];
  scheduleForm.sampleCount = vo.sampleCount ?? 3;
  scheduleForm.calibrationRatio = Math.round((vo.calibrationRatio ?? 0) * 100);
  const scope = vo.questionScope;
  if (scope === 'all' || scope === 'custom') {
    scheduleForm.questionScope = scope;
  } else {
    scheduleForm.questionScope = 'stage';
  }
  if (!scheduleForm.probeModes.includes('grounded-api')) {
    scheduleForm.probeModes = ['grounded-api', ...scheduleForm.probeModes];
  }
  scheduleStatus.nextRunAt = vo.nextRunAt;
  scheduleStatus.lastTriggeredAt = vo.lastTriggeredAt;
  scheduleStatus.lastRunId = vo.lastRunId;
  scheduleStatus.lastRunName = vo.lastRunName ?? null;
}

async function loadSchedule() {
  if (!projectStore.currentProjectId) return;
  scheduleLoading.value = true;
  try {
    const vo = await getDiagnosticSchedule(projectStore.currentProjectId);
    applyScheduleVo(vo);
  } finally {
    scheduleLoading.value = false;
  }
}

async function saveSchedule() {
  await scheduleFormRef.value?.validate();
  if (!projectStore.currentProjectId) return;
  scheduleSaving.value = true;
  try {
    const vo = await upsertDiagnosticSchedule(projectStore.currentProjectId, {
      ...scheduleForm,
      region: scheduleForm.market
    });
    applyScheduleVo(vo);
    proxy?.$modal.msgSuccess('定时计划已保存');
  } finally {
    scheduleSaving.value = false;
  }
}

function onPageTabChange(name: string | number) {
  if (name === 'schedule') {
    loadSchedule();
  }
}

function formatDateTime(value: string) {
  return proxy?.parseTime(value, '{y}-{m}-{d} {h}:{i}') ?? value;
}

function goDetailById(runId: number) {
  router.push({ name: 'DiagnosticDetail', params: { runId } });
}

function resetFormDefaults() {
  const p = projectStore.currentProject;
  form.market = p?.targetMarkets[0] ?? 'US';
  form.locale = p?.defaultLocale ?? 'en-US';
  form.region = form.market;
  form.name = p ? `${p.name}-${new Date().toISOString().slice(0, 10)}诊断` : '';
}

function openDrawer() {
  resetFormDefaults();
  Object.assign(form, defaultForm(), {
    name: form.name,
    market: form.market,
    locale: form.locale,
    region: form.region
  });
  if (!form.probeModes.includes('grounded-api')) {
    form.probeModes = ['grounded-api', ...form.probeModes];
  }
  drawerVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  if (!projectStore.currentProjectId) return;
  submitLoading.value = true;
  try {
    const created = await createDiagnosticRun(projectStore.currentProjectId, { ...form, region: form.market });
    proxy?.$modal.msgSuccess('任务已创建');
    drawerVisible.value = false;
    await getList();
    router.push({ name: 'DiagnosticDetail', params: { runId: created.id } });
  } finally {
    submitLoading.value = false;
  }
}

function goDetail(row: DiagnosticRunVO) {
  router.push({ name: 'DiagnosticDetail', params: { runId: row.id } });
}

function copyRunId(row: DiagnosticRunVO) {
  navigator.clipboard.writeText(row.runId).then(() => {
    proxy?.$modal.msgSuccess('已复制任务 ID');
  });
}

onMounted(async () => {
  if (!projectStore.initialized) {
    await projectStore.fetchProjects();
  }
  if (route.query.tab === 'schedule') {
    activePageTab.value = 'schedule';
  }
  await getList();
  if (activePageTab.value === 'schedule') {
    await loadSchedule();
  }
  if (route.query.create === '1') {
    openDrawer();
  }
});
</script>

<style scoped lang="scss">
.tg-diagnostics {
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

.compliance-hint {
  margin: 0 0 var(--tg-space-4);
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
}

.field-hint {
  font-size: var(--tg-font-size-xs);
  color: var(--tg-color-text-secondary);
  margin-top: var(--tg-space-1);
}

.calibration-slider {
  width: 100%;
}

.drawer-alert {
  margin-top: var(--tg-space-4);
}

.page-tabs {
  margin-bottom: var(--tg-space-3);

  :deep(.el-tabs__header) {
    margin-bottom: var(--tg-space-3);
  }
}

.schedule-sub {
  margin: 0 0 var(--tg-space-3);
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.schedule-card {
  &__title {
    font-weight: 600;
    color: var(--tg-color-text-primary);
  }
}

.schedule-status {
  margin-bottom: var(--tg-space-4);
}

.schedule-footnote {
  margin-top: var(--tg-space-4);
  background: var(--el-fill-color-lighter);

  .footnote-title {
    margin: 0 0 var(--tg-space-2);
    font-weight: 600;
    font-size: var(--tg-font-size-sm);
  }

  .footnote-list {
    margin: 0;
    padding-left: 1.25rem;
    font-size: var(--tg-font-size-xs);
    color: var(--tg-color-text-secondary);
    line-height: 1.6;

    li + li {
      margin-top: var(--tg-space-1);
    }
  }
}

.schedule-actions {
  margin-top: var(--tg-space-4);
  text-align: right;
}

.text-muted {
  color: var(--tg-color-text-secondary);
}

.ml-2 {
  margin-left: var(--tg-space-2);
}

.mb-3 {
  margin-bottom: var(--tg-space-3);
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
