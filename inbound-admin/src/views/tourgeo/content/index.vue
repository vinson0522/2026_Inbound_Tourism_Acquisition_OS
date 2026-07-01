<template>
  <div class="p-2 tg-content-tasks">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">内容任务</h1>
        <p class="tg-page-sub">内容 Agent · 社媒脚本任务与 AI 生成（FR-301/302）</p>
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
          <el-button type="primary" icon="Plus" :disabled="generating" @click="openCreateDrawer()">新建任务</el-button>
          <el-dropdown trigger="click" @command="openKeywordPicker">
            <el-button :disabled="generating">
              从关键词创建
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pick">选择关键词…</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-form :inline="true" class="toolbar-form" @submit.prevent="handleQuery">
            <el-form-item label="平台">
              <el-select v-model="queryParams.platform" placeholder="全部" clearable style="width: 130px">
                <el-option label="全部" value="" />
                <el-option v-for="p in CONTENT_PLATFORMS" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
                <el-option label="全部" value="" />
                <el-option
                  v-for="(meta, key) in CONTENT_TASK_STATUS_META"
                  :key="key"
                  :label="meta.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="人工审核">
              <el-select v-model="queryParams.needsHumanReview" placeholder="全部" clearable style="width: 110px">
                <el-option label="全部" value="" />
                <el-option label="待审核" :value="true" />
                <el-option label="已通过" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input
                v-model="queryParams.keyword"
                placeholder="当前页筛选"
                clearable
                style="width: 160px"
                @keyup.enter="handleQuery"
              />
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
        title="AI 正在生成脚本，约 30–90 秒，请勿关闭页面"
        class="mb-3"
      />

      <el-card shadow="hover">
        <el-table
          v-loading="loading"
          :element-loading-text="generating ? 'AI 正在生成脚本…' : '加载中…'"
          border
          :data="displayList"
        >
          <template #empty>
            <el-empty :description="emptyDescription">
              <el-button type="primary" @click="openCreateDrawer()">新建任务</el-button>
              <el-button @click="openKeywordPicker('pick')">从关键词创建</el-button>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="关联关键词" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.keywordText">{{ row.keywordText }}</span>
              <span v-else class="text-muted">—（手动任务）</span>
            </template>
          </el-table-column>
          <el-table-column label="平台" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ platformLabel(row.platform) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="格式" width="100" align="center" class-name="hidden-md-only">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ formatLabel(row.format) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="70" align="center" class-name="hidden-md-only">
            <template #default="{ row }">{{ row.duration ? `${row.duration}s` : '—' }}</template>
          </el-table-column>
          <el-table-column label="语气" width="90" align="center" class-name="hidden-md-only">
            <template #default="{ row }">{{ toneLabel(row.tone) }}</template>
          </el-table-column>
          <el-table-column label="市场" width="70" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.targetMarket" size="small" type="info">{{ row.targetMarket }}</el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="任务状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="contentStatusMeta(row.status).type" size="small">
                {{ contentStatusMeta(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="人工审核" width="100" align="center">
            <template #default="{ row }">
              <template v-if="row.contentTitle || row.needsHumanReview != null">
                <el-tooltip v-if="row.needsHumanReview" :content="NEEDS_REVIEW_TOOLTIP" placement="top">
                  <el-tag type="warning" size="small">待审核</el-tag>
                </el-tooltip>
                <el-tag v-else type="success" size="small">已通过</el-tag>
              </template>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="内容标题" min-width="160" show-overflow-tooltip class-name="hidden-sm-only">
            <template #default="{ row }">{{ row.contentTitle || '—' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="160" class-name="hidden-md-only">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="canGenerate(row)"
                link
                type="primary"
                :loading="generating && generatingTaskId === row.id"
                :disabled="generating"
                @click="handleGenerate(row)"
              >
                生成脚本
              </el-button>
              <el-button
                v-if="canPreview(row)"
                link
                type="primary"
                :disabled="generating"
                @click="openPreview(row)"
              >
                查看内容
              </el-button>
              <el-button link type="danger" :disabled="generating" @click="handleDelete(row)">删除</el-button>
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

        <p v-if="clientFilterActive" class="filter-hint">关键词 / 人工审核为 M1 当前页客户端筛选；服务端分页以状态、平台为准。</p>
      </el-card>
    </template>

    <!-- 创建抽屉 -->
    <el-drawer v-model="createVisible" title="创建内容任务" size="560px" destroy-on-close @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="关联关键词" prop="keywordId">
          <el-select
            v-model="createForm.keywordId"
            filterable
            placeholder="选择关键词"
            style="width: 100%"
            :loading="keywordLoading"
          >
            <el-option
              v-for="kw in keywordOptions"
              :key="kw.id"
              :label="kw.keyword"
              :value="kw.id"
            />
          </el-select>
          <el-button link type="primary" class="mt-1" @click="goKeywords">跳转关键词页</el-button>
        </el-form-item>
        <el-form-item label="目标平台" prop="platform">
          <el-select v-model="createForm.platform" style="width: 100%">
            <el-option v-for="p in CONTENT_PLATFORMS" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容格式" prop="format">
          <el-select v-model="createForm.format" style="width: 100%">
            <el-option v-for="f in CONTENT_FORMATS" :key="f.value" :label="f.label" :value="f.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="视频时长">
          <div class="duration-row">
            <el-input-number v-model="createForm.duration" :min="5" :max="180" :step="5" />
            <span class="duration-unit">秒</span>
            <el-button-group class="duration-quick">
              <el-button
                v-for="d in CONTENT_DURATIONS"
                :key="d"
                size="small"
                :type="createForm.duration === d ? 'primary' : 'default'"
                @click="createForm.duration = d"
              >
                {{ d }}s
              </el-button>
            </el-button-group>
          </div>
        </el-form-item>
        <el-form-item label="品牌语气">
          <el-select v-model="createForm.tone" style="width: 100%">
            <el-option v-for="t in CONTENT_TONES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标市场">
          <el-select v-model="createForm.targetMarket" style="width: 100%">
            <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
            <el-option v-if="!marketOptions.length" label="US" value="US" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="createForm.language" style="width: 100%">
            <el-option label="English (en)" value="en" />
            <el-option label="中文 (zh)" value="zh" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon class="mb-3">
          创建后将进入「草稿」，可在此列表触发 AI 脚本生成（FR-302）。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">创建任务</el-button>
      </template>
    </el-drawer>

    <!-- 关键词选择 -->
    <el-dialog v-model="keywordPickerVisible" title="从关键词创建" width="640px" destroy-on-close>
      <el-table v-loading="keywordLoading" :data="keywordOptions" max-height="360" @row-click="pickKeywordRow">
        <el-table-column label="关键词" prop="keyword" min-width="200" />
        <el-table-column label="阶段" width="90">
          <template #default="{ row }">{{ row.stage }}</template>
        </el-table-column>
        <el-table-column label="市场" width="70" prop="market" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="pickKeyword(row.id)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 脚本预览 drawer -->
    <el-drawer v-model="previewVisible" title="脚本预览" size="640px" destroy-on-close>
      <div v-loading="previewLoading" class="preview-body">
        <template v-if="previewDetail">
          <div class="preview-header">
            <h3>{{ previewDetail.generatedContent?.title || previewDetail.contentTitle || '未命名脚本' }}</h3>
            <div class="preview-tags">
              <el-tag :type="contentStatusMeta(previewDetail.status).type" size="small">
                {{ contentStatusMeta(previewDetail.status).label }}
              </el-tag>
              <el-tooltip v-if="previewDetail.generatedContent?.needsHumanReview !== false" :content="NEEDS_REVIEW_TOOLTIP">
                <el-tag type="warning" size="small">待人工审核</el-tag>
              </el-tooltip>
              <el-tag v-if="previewDetail.generatedContent?.version" size="small" type="info">
                v{{ previewDetail.generatedContent.version }}
              </el-tag>
            </div>
          </div>

          <template v-if="previewDetail.generatedContent">
            <section v-if="previewDetail.generatedContent.hook" class="preview-section">
              <h4>Hook</h4>
              <p>{{ previewDetail.generatedContent.hook }}</p>
            </section>
            <section v-if="previewDetail.generatedContent.script" class="preview-section">
              <h4>脚本</h4>
              <pre class="preview-pre">{{ previewDetail.generatedContent.script }}</pre>
            </section>
            <section v-if="previewDetail.generatedContent.voiceover" class="preview-section">
              <h4>口播</h4>
              <p>{{ previewDetail.generatedContent.voiceover }}</p>
            </section>
            <section v-if="previewDetail.generatedContent.onScreenText" class="preview-section">
              <h4>屏幕文字</h4>
              <pre class="preview-pre">{{ previewDetail.generatedContent.onScreenText }}</pre>
            </section>
            <section v-if="previewDetail.generatedContent.cta" class="preview-section">
              <h4>CTA</h4>
              <p>{{ previewDetail.generatedContent.cta }}</p>
            </section>
            <section
              v-if="previewDetail.generatedContent.storyboardJson?.length"
              class="preview-section"
            >
              <h4>分镜</h4>
              <el-table :data="previewDetail.generatedContent.storyboardJson" border size="small">
                <el-table-column label="#" prop="scene" width="50" align="center" />
                <el-table-column label="时长(s)" prop="duration" width="80" align="center" />
                <el-table-column label="画面" prop="visual" min-width="160" show-overflow-tooltip />
                <el-table-column label="备注" prop="note" min-width="100" show-overflow-tooltip />
              </el-table>
            </section>
          </template>
          <el-empty v-else description="暂无生成内容，请先点击「生成脚本」" />
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="ContentTaskList" lang="ts">
import {
  createContentTask,
  deleteContentTask,
  generateContentScript,
  getContentTask,
  listContentTasks
} from '@/api/tourgeo/content';
import { listKeywords } from '@/api/tourgeo/keyword';
import type { ContentTaskDetailVo, ContentTaskForm, ContentTaskVo, KeywordOpportunityVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  CONTENT_DURATIONS,
  CONTENT_FORMATS,
  CONTENT_PLATFORMS,
  CONTENT_TASK_STATUS_META,
  CONTENT_TONES,
  NEEDS_REVIEW_TOOLTIP,
  contentStatusMeta,
  formatLabel,
  platformLabel,
  toneLabel
} from '@/constants/content';
import { useProjectStore } from '@/store/modules/project';
import { ArrowDown } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const generating = ref(false);
const generatingTaskId = ref<number | null>(null);
const taskList = ref<ContentTaskVo[]>([]);
const total = ref(0);

const createVisible = ref(false);
const createSubmitting = ref(false);
const createFormRef = ref<FormInstance>();
const keywordLoading = ref(false);
const keywordOptions = ref<KeywordOpportunityVo[]>([]);
const keywordPickerVisible = ref(false);

const previewVisible = ref(false);
const previewLoading = ref(false);
const previewDetail = ref<ContentTaskDetailVo | null>(null);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  platform: '',
  status: '' as '' | ContentTaskVo['status'],
  keyword: '',
  needsHumanReview: '' as boolean | ''
});

const defaultCreateForm = (): Omit<ContentTaskForm, 'keywordId'> & { keywordId?: number } => ({
  keywordId: undefined,
  platform: 'tiktok',
  format: 'short_video',
  duration: 30,
  tone: 'friendly',
  language: 'en',
  targetMarket: ''
});

const createForm = reactive(defaultCreateForm());

const createRules: FormRules = {
  keywordId: [{ required: true, message: '请选择关联关键词', trigger: 'change' }],
  platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
  format: [{ required: true, message: '请选择格式', trigger: 'change' }]
};

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

const clientFilterActive = computed(
  () => Boolean(queryParams.keyword) || queryParams.needsHumanReview !== ''
);

const hasActiveFilters = computed(
  () =>
    Boolean(
      queryParams.platform ||
        queryParams.status ||
        queryParams.keyword ||
        queryParams.needsHumanReview !== ''
    )
);

const emptyDescription = computed(() => (hasActiveFilters.value ? '未找到匹配任务' : '暂无内容任务'));

const displayList = computed(() => {
  let rows = taskList.value;
  const kw = queryParams.keyword?.trim().toLowerCase();
  if (kw) {
    rows = rows.filter((r) => (r.keywordText ?? '').toLowerCase().includes(kw));
  }
  if (queryParams.needsHumanReview === true) {
    rows = rows.filter((r) => r.needsHumanReview === true);
  } else if (queryParams.needsHumanReview === false) {
    rows = rows.filter((r) => r.contentTitle && r.needsHumanReview === false);
  }
  return rows;
});

function formatTime(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function canGenerate(row: ContentTaskVo): boolean {
  return row.status === 'DRAFT' || row.status === 'FAILED';
}

function canPreview(row: ContentTaskVo): boolean {
  return Boolean(row.contentTitle) || row.status === 'DRAFT' || row.status === 'GENERATED' || row.status === 'ADOPTED';
}

function syncProjectFromRoute() {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) {
    projectStore.setCurrentProject(fromRoute);
  }
}

async function loadKeywordOptions(): Promise<KeywordOpportunityVo[]> {
  const pid = projectId.value;
  if (!pid) return [];
  keywordLoading.value = true;
  try {
    const res = await listKeywords(pid, { pageNum: 1, pageSize: 100, status: 'ACTIVE' });
    keywordOptions.value = res.rows;
    return res.rows;
  } catch {
    keywordOptions.value = [];
    return [];
  } finally {
    keywordLoading.value = false;
  }
}

async function getList() {
  const pid = projectId.value;
  if (!pid) {
    taskList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listContentTasks(pid, {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status,
      platform: queryParams.platform
    });
    taskList.value = res.rows;
    total.value = res.total;
  } catch {
    taskList.value = [];
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
  queryParams.platform = '';
  queryParams.status = '';
  queryParams.keyword = '';
  queryParams.needsHumanReview = '';
  queryParams.pageNum = 1;
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectContentTasks') {
    router.replace({ name: 'ProjectContentTasks', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function resetCreateForm() {
  Object.assign(createForm, defaultCreateForm());
  createForm.targetMarket = marketOptions.value[0] ?? 'US';
}

async function openCreateDrawer(keywordId?: number) {
  resetCreateForm();
  createVisible.value = true;
  const keywords = await loadKeywordOptions();
  if (keywordId) {
    const found = keywords.find((k) => k.id === keywordId);
    if (found) {
      createForm.keywordId = found.id;
      createForm.targetMarket = found.market || marketOptions.value[0] || 'US';
    } else {
      ElMessage.warning('关键词不存在或已删除，请重新选择');
    }
  }
  if (!createForm.targetMarket) {
    createForm.targetMarket = marketOptions.value[0] ?? 'US';
  }
}

function openKeywordPicker(_cmd?: string) {
  keywordPickerVisible.value = true;
  loadKeywordOptions();
}

function pickKeywordRow(row: KeywordOpportunityVo) {
  pickKeyword(row.id);
}

function pickKeyword(keywordId: number) {
  keywordPickerVisible.value = false;
  openCreateDrawer(keywordId);
}

async function submitCreate() {
  const pid = projectId.value;
  if (!pid || !createFormRef.value) return;
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return;
    createSubmitting.value = true;
    try {
      await createContentTask(pid, {
        keywordId: createForm.keywordId!,
        platform: createForm.platform,
        format: createForm.format,
        duration: createForm.duration,
        tone: createForm.tone,
        language: createForm.language,
        targetMarket: createForm.targetMarket
      });
      ElMessage.success('任务已创建');
      createVisible.value = false;
      await getList();
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg && msg !== 'error') ElMessage.error(msg);
    } finally {
      createSubmitting.value = false;
    }
  });
}

async function handleGenerate(row: ContentTaskVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(`为「${row.keywordText ?? '任务'}」生成 AI 脚本？`, '生成脚本', {
      confirmButtonText: '开始生成',
      cancelButtonText: '取消',
      type: 'info'
    });
  } catch {
    return;
  }
  generating.value = true;
  generatingTaskId.value = row.id;
  try {
    await generateContentScript(pid, row.id, { useRag: false });
    ElMessage.success('脚本生成完成');
    await getList();
    await openPreviewById(row.id);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg.includes('402') || msg.includes('额度')) {
      ElMessage.warning('内容生成额度不足');
    } else if (msg && msg !== 'error') {
      ElMessage.error(msg);
    }
    await getList();
  } finally {
    generating.value = false;
    generatingTaskId.value = null;
  }
}

async function openPreviewById(taskId: number) {
  const pid = projectId.value;
  if (!pid) return;
  previewVisible.value = true;
  previewLoading.value = true;
  previewDetail.value = null;
  try {
    previewDetail.value = await getContentTask(pid, taskId);
  } catch {
    ElMessage.error('加载脚本详情失败');
    previewVisible.value = false;
  } finally {
    previewLoading.value = false;
  }
}

async function openPreview(row: ContentTaskVo) {
  await openPreviewById(row.id);
}

async function handleDelete(row: ContentTaskVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(`确定删除内容任务 #${row.id}？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteContentTask(pid, row.id);
    ElMessage.success('已删除');
    await getList();
  } catch {
    /* cancelled */
  }
}

function goKeywords() {
  const pid = projectId.value;
  if (!pid) {
    router.push('/keywords/index');
    return;
  }
  router.push({ name: 'ProjectKeywords', params: { projectId: pid } });
}

function initFromQuery() {
  const action = route.query.action;
  const rawKid = route.query.keywordId;
  const keywordId = Number(Array.isArray(rawKid) ? rawKid[0] : rawKid);
  if (action === 'create') {
    openCreateDrawer(Number.isFinite(keywordId) && keywordId > 0 ? keywordId : undefined);
    const q = { ...route.query };
    delete q.action;
    delete q.keywordId;
    router.replace({ query: q });
  }
  const rawReview = route.query.needsHumanReview;
  const review = Array.isArray(rawReview) ? rawReview[0] : rawReview;
  if (review === 'true') {
    queryParams.needsHumanReview = true;
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
  initFromQuery();
  await getList();
});
</script>

<style scoped lang="scss">
.tg-content-tasks {
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

  .text-muted {
    color: var(--tg-color-text-placeholder, #9ca3af);
  }

  .filter-hint {
    margin: var(--tg-space-3, 12px) 0 0;
    font-size: var(--tg-font-size-xs, 12px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .duration-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tg-space-2, 8px);
  }

  .duration-unit {
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .duration-quick {
    margin-left: var(--tg-space-2, 8px);
  }

  .preview-body {
    min-height: 200px;
  }

  .preview-header {
    margin-bottom: var(--tg-space-4, 16px);

    h3 {
      margin: 0 0 var(--tg-space-2, 8px);
      font-size: var(--tg-font-size-lg, 18px);
    }
  }

  .preview-tags {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tg-space-2, 8px);
  }

  .preview-section {
    margin-bottom: var(--tg-space-4, 16px);

    h4 {
      margin: 0 0 var(--tg-space-2, 8px);
      font-size: var(--tg-font-size-sm, 13px);
      color: var(--tg-color-text-secondary, #6b7280);
    }

    p {
      margin: 0;
      line-height: 1.6;
      white-space: pre-wrap;
    }
  }

  .preview-pre {
    margin: 0;
    padding: var(--tg-space-3, 12px);
    background: var(--tg-color-bg-muted, #f3f4f6);
    border-radius: var(--tg-radius-sm, 4px);
    font-family: inherit;
    font-size: var(--tg-font-size-sm, 13px);
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

@media (max-width: 768px) {
  .tg-content-tasks :deep(.hidden-md-only) {
    display: none;
  }
}

@media (max-width: 992px) {
  .tg-content-tasks :deep(.hidden-sm-only) {
    display: none;
  }
}
</style>
