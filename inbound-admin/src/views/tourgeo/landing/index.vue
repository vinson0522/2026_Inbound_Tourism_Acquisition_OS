<template>
  <div class="p-2 tg-landing-pages">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">页面草稿</h1>
        <p class="tg-page-sub">落地页 Agent · 英文落地页结构与 AI 文案（FR-501~505）</p>
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
          <el-button type="primary" icon="Plus" :disabled="generating" @click="openCreateDialog()">新建页面</el-button>
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
            <el-form-item label="标题">
              <el-input
                v-model="queryParams.title"
                placeholder="当前页筛选"
                clearable
                style="width: 140px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="Slug">
              <el-input
                v-model="queryParams.slug"
                placeholder="当前页筛选"
                clearable
                style="width: 130px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="模板">
              <el-select v-model="queryParams.templateType" placeholder="全部" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option v-for="t in LANDING_TEMPLATE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
                <el-option label="全部" value="" />
                <el-option
                  v-for="(meta, key) in LANDING_PAGE_STATUS_META"
                  :key="key"
                  :label="meta.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input
                v-model="queryParams.keyword"
                placeholder="当前页筛选"
                clearable
                style="width: 140px"
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
        title="AI 正在生成英文文案与页面结构，约 1–3 分钟…"
        class="mb-3"
      />

      <el-card shadow="hover">
        <el-table
          v-loading="loading"
          :element-loading-text="generating ? 'AI 正在生成落地页…' : '加载中…'"
          border
          :data="displayList"
        >
          <template #empty>
            <el-empty :description="emptyDescription">
              <el-button type="primary" @click="openCreateDialog()">新建页面</el-button>
              <el-button @click="openKeywordPicker('pick')">从关键词创建</el-button>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="页面标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" class="title-link" @click="openPreview(row)">
                {{ row.title }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="Slug" width="160" show-overflow-tooltip class-name="hidden-sm-only">
            <template #default="{ row }">
              <span class="slug-mono">/{{ row.slug }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模板" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ templateTypeLabel(row.templateType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联关键词" min-width="180" show-overflow-tooltip class-name="hidden-md-only">
            <template #default="{ row }">
              <span v-if="row.keywordText">{{ row.keywordText }}</span>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="市场" width="70" align="center" class-name="hidden-md-only">
            <template #default="{ row }">
              <el-tag v-if="keywordMarket(row)" size="small" type="info">{{ keywordMarket(row) }}</el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="landingStatusMeta(row.status).type" size="small">
                <el-icon v-if="row.status === 'EDITING' || (generating && generatingPageId === row.id)" class="is-loading mr-1">
                  <Loading />
                </el-icon>
                {{ landingStatusMeta(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="160" class-name="hidden-md-only">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="canPreview(row)"
                link
                type="primary"
                :disabled="generating"
                @click="openPreview(row)"
              >
                预览
              </el-button>
              <el-button
                v-if="canGenerate(row)"
                link
                type="primary"
                :loading="generating && generatingPageId === row.id"
                :disabled="generating"
                @click="handleGenerate(row)"
              >
                AI 生成
              </el-button>
              <el-button link type="primary" :disabled="generating" @click="copySlug(row.slug)">复制 slug</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                link
                type="danger"
                :disabled="generating"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
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

        <p v-if="clientFilterActive" class="filter-hint">标题 / Slug / 关键词为 M1 当前页客户端筛选；服务端分页以状态、模板为准。</p>
      </el-card>
    </template>

    <!-- 创建弹窗 -->
    <el-dialog v-model="createVisible" title="新建落地页" width="560px" destroy-on-close @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="页面标题" prop="title">
          <el-input v-model="createForm.title" maxlength="500" show-word-limit placeholder="英文页面标题" @blur="suggestSlugFromTitle" />
        </el-form-item>
        <el-form-item label="URL Slug" prop="slug">
          <el-input v-model="createForm.slug" placeholder="chongqing-cyberpunk-tour" @blur="validateSlug">
            <template #prefix>/</template>
          </el-input>
          <p v-if="slugHint === 'ok'" class="slug-hint slug-hint--ok">✓ Slug 格式可用（创建时校验唯一性）</p>
          <p v-else-if="slugHint === 'invalid'" class="slug-hint slug-hint--err">仅小写字母、数字与连字符，2–200 字</p>
          <p v-else-if="slugHint === 'conflict'" class="slug-hint slug-hint--err">Slug 与现有页面冲突</p>
        </el-form-item>
        <el-form-item label="页面模板" prop="templateType">
          <el-select v-model="createForm.templateType" style="width: 100%">
            <el-option v-for="t in LANDING_TEMPLATE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联关键词" prop="keywordId">
          <el-select
            v-model="createForm.keywordId"
            filterable
            placeholder="选择关键词"
            style="width: 100%"
            :loading="keywordLoading"
            @change="onKeywordChange"
          >
            <el-option v-for="kw in keywordOptions" :key="kw.id" :label="kw.keyword" :value="kw.id" />
          </el-select>
          <el-button link type="primary" class="mt-1" @click="goKeywords">跳转关键词页</el-button>
        </el-form-item>
        <el-form-item label="目标市场">
          <el-select v-model="createForm.targetMarket" style="width: 100%">
            <el-option v-for="m in marketOptions" :key="m" :label="m" :value="m" />
            <el-option v-if="!marketOptions.length" label="US" value="US" />
          </el-select>
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon :title="NEEDS_REVIEW_TOOLTIP" class="mb-2" />
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button :loading="createSubmitting" :disabled="!canSubmitCreate" @click="submitCreate(false)">仅创建</el-button>
        <el-button type="primary" :loading="createSubmitting || generating" :disabled="!canSubmitCreate" @click="submitCreate(true)">
          创建并 AI 生成
        </el-button>
      </template>
    </el-dialog>

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

    <!-- 预览 drawer -->
    <el-drawer
      v-model="previewVisible"
      :title="previewTitle"
      size="640px"
      destroy-on-close
      class="landing-preview-drawer"
    >
      <div v-loading="previewLoading" class="preview-body">
        <template v-if="previewDetail">
          <div class="preview-header">
            <el-tag :type="landingStatusMeta(previewDetail.status).type" size="small">
              {{ landingStatusMeta(previewDetail.status).label }}
            </el-tag>
            <span class="slug-mono ml-2">/{{ previewDetail.slug }}</span>
          </div>

          <template v-if="hasPreviewContent">
            <section class="preview-section">
              <h4>可读摘要</h4>
              <p v-if="previewH1" class="preview-h1">{{ previewH1 }}</p>
              <p v-if="previewHeroSubtitle" class="text-secondary">{{ previewHeroSubtitle }}</p>
              <p class="text-muted">共 {{ previewModules.length }} 个模块</p>
            </section>

            <section class="preview-section">
              <h4>SEO / GEO 元数据</h4>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="Title">{{ previewSeo.title || '—' }}</el-descriptions-item>
                <el-descriptions-item label="Meta Description">{{ previewSeo.description || '—' }}</el-descriptions-item>
                <el-descriptions-item label="H1">{{ previewSeo.h1 || '—' }}</el-descriptions-item>
                <el-descriptions-item label="FAQ Schema">
                  {{ previewFaqSchema ? '有' : '无' }}
                </el-descriptions-item>
              </el-descriptions>
              <el-collapse v-if="previewFaqSchema" class="mt-2">
                <el-collapse-item title="查看 FAQ Schema JSON" name="faq">
                  <pre class="preview-json">{{ formatJson(previewFaqSchema) }}</pre>
                </el-collapse-item>
              </el-collapse>
            </section>

            <section class="preview-section">
              <h4>页面模块</h4>
              <el-collapse>
                <el-collapse-item
                  v-for="(mod, idx) in previewModules"
                  :key="moduleKey(mod, idx)"
                  :title="moduleCollapseTitle(mod)"
                  :name="String(idx)"
                >
                  <p class="module-summary">{{ moduleSummary(mod) }}</p>
                  <el-collapse>
                    <el-collapse-item title="查看 JSON" :name="`json-${idx}`">
                      <pre class="preview-json">{{ formatJson(mod) }}</pre>
                    </el-collapse-item>
                  </el-collapse>
                </el-collapse-item>
              </el-collapse>
            </section>

            <section class="preview-section">
              <h4>表单与 WhatsApp</h4>
              <div v-if="previewFormFields.length" class="form-tags">
                <el-tag v-for="f in previewFormFields" :key="f" size="small" class="mr-1 mb-1">
                  {{ LANDING_FORM_FIELD_LABELS[f] ?? f }}
                </el-tag>
              </div>
              <p v-else class="text-muted">未配置表单字段</p>
              <p v-if="previewWhatsapp" class="mt-2">
                <el-link :href="previewWhatsapp" target="_blank" type="primary">WhatsApp 咨询链接</el-link>
              </p>
            </section>

            <el-alert type="warning" :closable="false" show-icon :title="NEEDS_REVIEW_TOOLTIP" />
          </template>

          <el-empty v-else description="尚未生成内容，请先 AI 生成">
            <el-button
              v-if="canGenerate(previewDetail)"
              type="primary"
              :loading="generating"
              @click="handleGenerate(previewDetail)"
            >
              AI 生成页面
            </el-button>
          </el-empty>
        </template>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-tooltip content="M2 可视化编辑" placement="top">
          <el-button disabled>编辑页面</el-button>
        </el-tooltip>
        <el-tooltip content="FR-506 M2" placement="top">
          <el-button disabled>导出 HTML</el-button>
        </el-tooltip>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="LandingPagesList" lang="ts">
import {
  createLandingPage,
  deleteLandingPage,
  generateLandingPage,
  getLandingPage,
  listLandingPages
} from '@/api/tourgeo/landing';
import { listKeywords } from '@/api/tourgeo/keyword';
import type { KeywordOpportunityVo, LandingModuleItem, LandingPageDetailVo, LandingPageVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  LANDING_FORM_FIELD_LABELS,
  LANDING_MODULE_LABELS,
  LANDING_PAGE_STATUS_META,
  LANDING_TEMPLATE_TYPES,
  NEEDS_REVIEW_TOOLTIP,
  isValidSlug,
  landingStatusMeta,
  slugifyTitle,
  templateTypeLabel
} from '@/constants/landing';
import { useProjectStore } from '@/store/modules/project';
import { ArrowDown, Loading } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const generating = ref(false);
const generatingPageId = ref<number | null>(null);
const pageList = ref<LandingPageVo[]>([]);
const total = ref(0);
const knownSlugs = ref<Set<string>>(new Set());

const createVisible = ref(false);
const createSubmitting = ref(false);
const createFormRef = ref<FormInstance>();
const keywordLoading = ref(false);
const keywordOptions = ref<KeywordOpportunityVo[]>([]);
const keywordPickerVisible = ref(false);
const slugHint = ref<'ok' | 'invalid' | 'conflict' | ''>('');

const previewVisible = ref(false);
const previewLoading = ref(false);
const previewDetail = ref<LandingPageDetailVo | null>(null);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  slug: '',
  templateType: '',
  status: '' as '' | LandingPageVo['status'],
  keyword: ''
});

const defaultCreateForm = () => ({
  title: '',
  slug: '',
  templateType: 'destination',
  keywordId: undefined as number | undefined,
  targetMarket: '',
  language: 'en'
});

const createForm = reactive(defaultCreateForm());

const createRules: FormRules = {
  title: [{ required: true, message: '请输入页面标题', trigger: 'blur' }],
  slug: [{ required: true, message: '请输入 URL Slug', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板', trigger: 'change' }],
  keywordId: [{ required: true, message: '请选择关联关键词', trigger: 'change' }]
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

const keywordMarketMap = computed(() => {
  const map = new Map<number, string>();
  for (const kw of keywordOptions.value) {
    if (kw.id) map.set(kw.id, kw.market);
  }
  return map;
});

const clientFilterActive = computed(
  () => Boolean(queryParams.title || queryParams.slug || queryParams.keyword)
);

const hasActiveFilters = computed(
  () => Boolean(queryParams.title || queryParams.slug || queryParams.templateType || queryParams.status || queryParams.keyword)
);

const emptyDescription = computed(() => (hasActiveFilters.value ? '未找到匹配页面' : '暂无落地页草稿'));

const canSubmitCreate = computed(
  () => createForm.title.trim() && isValidSlug(createForm.slug) && slugHint.value !== 'conflict' && createForm.keywordId
);

const displayList = computed(() => {
  let rows = pageList.value;
  const title = queryParams.title?.trim().toLowerCase();
  const slug = queryParams.slug?.trim().toLowerCase();
  const kw = queryParams.keyword?.trim().toLowerCase();
  if (title) rows = rows.filter((r) => (r.title ?? '').toLowerCase().includes(title));
  if (slug) rows = rows.filter((r) => (r.slug ?? '').toLowerCase().includes(slug));
  if (kw) rows = rows.filter((r) => (r.keywordText ?? '').toLowerCase().includes(kw));
  return rows;
});

const previewTitle = computed(() => {
  if (!previewDetail.value) return '预览';
  return `预览 · ${previewDetail.value.title}`;
});

const previewModules = computed((): LandingModuleItem[] => {
  const raw = previewDetail.value?.contentJson?.modules;
  return Array.isArray(raw) ? (raw as LandingModuleItem[]) : [];
});

const hasPreviewContent = computed(() => previewModules.value.length > 0);

const previewSeo = computed(() => previewDetail.value?.seoMetaJson ?? {});

const previewH1 = computed(() => {
  const seo = previewSeo.value as Record<string, unknown>;
  if (seo.h1) return String(seo.h1);
  const hero = previewModules.value.find((m) => moduleKey(m) === 'hero');
  const content = hero?.content as Record<string, unknown> | undefined;
  return content?.headline ? String(content.headline) : '';
});

const previewHeroSubtitle = computed(() => {
  const hero = previewModules.value.find((m) => moduleKey(m) === 'hero');
  const content = hero?.content as Record<string, unknown> | undefined;
  return content?.subtitle ? String(content.subtitle) : '';
});

const previewFaqSchema = computed(() => {
  const seo = previewSeo.value as Record<string, unknown>;
  return seo.faq_schema ?? seo.faqSchema ?? null;
});

const previewFormFields = computed(() => {
  const form = previewDetail.value?.formConfigJson as Record<string, unknown> | undefined;
  const fields = form?.fields;
  return Array.isArray(fields) ? fields.map(String) : [];
});

const previewWhatsapp = computed((): string => {
  const form = previewDetail.value?.formConfigJson as Record<string, unknown> | undefined;
  const link = previewDetail.value?.whatsappLink || form?.whatsapp_link || form?.whatsappLink;
  return link ? String(link) : '';
});

function keywordMarket(row: LandingPageVo): string | undefined {
  if (!row.keywordId) return undefined;
  return keywordMarketMap.value.get(row.keywordId);
}

function formatTime(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatJson(value: unknown): string {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function moduleKey(mod: LandingModuleItem, _idx?: number): string {
  return String(mod.key ?? mod.type ?? 'module');
}

function moduleLabel(mod: LandingModuleItem): string {
  const key = moduleKey(mod);
  return LANDING_MODULE_LABELS[key] ?? key;
}

function moduleCollapseTitle(mod: LandingModuleItem): string {
  const label = moduleLabel(mod);
  const summary = moduleSummary(mod);
  if (!summary) return label;
  const short = summary.length > 80 ? `${summary.slice(0, 80)}…` : summary;
  return `${label} · ${short}`;
}

function moduleSummary(mod: LandingModuleItem): string {
  const content = (mod.content ?? mod) as Record<string, unknown>;
  const key = moduleKey(mod);
  if (key === 'hero') {
    return [content.headline, content.subtitle].filter(Boolean).join(' — ');
  }
  if (key === 'itinerary') {
    const days = content.days;
    if (Array.isArray(days)) return `${days.length} 天行程`;
  }
  if (key === 'faq') {
    const items = content.items;
    if (Array.isArray(items)) return `${items.length} 个问题`;
  }
  if (key === 'traveler_reviews' || key === 'reviews') {
    const reviews = content.reviews ?? content.items;
    if (Array.isArray(reviews)) return `${reviews.length} 条评价`;
  }
  if (content.headline) return String(content.headline);
  if (content.body) return String(content.body);
  return '';
}

function canGenerate(row: LandingPageVo): boolean {
  return row.status === 'DRAFT' || row.status === 'EDITING';
}

function canPreview(row: LandingPageVo): boolean {
  return (row.moduleCount ?? 0) > 0;
}

function syncProjectFromRoute() {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) {
    projectStore.setCurrentProject(fromRoute);
  }
}

async function refreshKnownSlugs() {
  const pid = projectId.value;
  if (!pid) return;
  try {
    const res = await listLandingPages(pid, { pageNum: 1, pageSize: 200 });
    knownSlugs.value = new Set(res.rows.map((r) => r.slug));
  } catch {
    knownSlugs.value = new Set(pageList.value.map((r) => r.slug));
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
    pageList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listLandingPages(pid, {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status,
      templateType: queryParams.templateType
    });
    pageList.value = res.rows;
    total.value = res.total;
    knownSlugs.value = new Set(res.rows.map((r) => r.slug));
  } catch {
    pageList.value = [];
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
  queryParams.title = '';
  queryParams.slug = '';
  queryParams.templateType = '';
  queryParams.status = '';
  queryParams.keyword = '';
  queryParams.pageNum = 1;
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectLandingPages') {
    router.replace({ name: 'ProjectLandingPages', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function resetCreateForm() {
  Object.assign(createForm, defaultCreateForm());
  createForm.targetMarket = marketOptions.value[0] ?? 'US';
  slugHint.value = '';
}

function suggestSlugFromTitle() {
  if (!createForm.slug && createForm.title.trim()) {
    createForm.slug = slugifyTitle(createForm.title);
    validateSlug();
  }
}

function validateSlug() {
  const slug = createForm.slug.trim();
  if (!slug) {
    slugHint.value = '';
    return;
  }
  if (!isValidSlug(slug)) {
    slugHint.value = 'invalid';
    return;
  }
  if (knownSlugs.value.has(slug)) {
    slugHint.value = 'conflict';
    return;
  }
  slugHint.value = 'ok';
}

function onKeywordChange(keywordId: number) {
  const kw = keywordOptions.value.find((k) => k.id === keywordId);
  if (!kw) return;
  if (!createForm.title.trim()) {
    createForm.title = `${kw.keyword} Landing Page`;
  }
  if (!createForm.slug.trim()) {
    createForm.slug = slugifyTitle(kw.keywordEn || kw.keyword);
    validateSlug();
  }
  createForm.targetMarket = kw.market || marketOptions.value[0] || 'US';
}

async function openCreateDialog(keywordId?: number) {
  resetCreateForm();
  createVisible.value = true;
  await refreshKnownSlugs();
  const keywords = await loadKeywordOptions();
  if (keywordId) {
    const found = keywords.find((k) => k.id === keywordId);
    if (found) {
      createForm.keywordId = found.id;
      onKeywordChange(found.id);
    } else {
      ElMessage.warning('关键词不存在或已删除，请重新选择');
    }
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
  openCreateDialog(keywordId);
}

async function submitCreate(andGenerate: boolean) {
  const pid = projectId.value;
  if (!pid || !createFormRef.value) return;
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return;
    validateSlug();
    if (slugHint.value === 'conflict' || slugHint.value === 'invalid') return;
    createSubmitting.value = true;
    try {
      const pageId = await createLandingPage(pid, {
        keywordId: createForm.keywordId!,
        templateType: createForm.templateType,
        title: createForm.title.trim(),
        slug: createForm.slug.trim(),
        language: createForm.language,
        targetMarket: createForm.targetMarket
      });
      ElMessage.success(andGenerate ? '页面已创建，开始 AI 生成…' : '页面已创建');
      createVisible.value = false;
      await getList();
      if (andGenerate) {
        const row = pageList.value.find((p) => p.id === pageId);
        if (row) await handleGenerate(row);
        else await handleGenerateById(pageId);
      }
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg.includes('slug') || msg.includes('Slug') || msg.includes('唯一')) {
        slugHint.value = 'conflict';
      }
      if (msg && msg !== 'error') ElMessage.error(msg);
    } finally {
      createSubmitting.value = false;
    }
  });
}

async function handleGenerateById(pageId: number) {
  const pid = projectId.value;
  if (!pid) return;
  generating.value = true;
  generatingPageId.value = pageId;
  try {
    await generateLandingPage(pid, pageId, { useRag: false });
    ElMessage.success('页面已生成，请预览并人工确认');
    await getList();
    await openPreviewById(pageId);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') ElMessage.error(msg);
    await getList();
  } finally {
    generating.value = false;
    generatingPageId.value = null;
  }
}

async function handleGenerate(row: LandingPageVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(`为「${row.title}」生成 AI 落地页文案？`, 'AI 生成', {
      confirmButtonText: '开始生成',
      cancelButtonText: '取消',
      type: 'info'
    });
  } catch {
    return;
  }
  await handleGenerateById(row.id);
}

async function openPreviewById(pageId: number) {
  const pid = projectId.value;
  if (!pid) return;
  previewVisible.value = true;
  previewLoading.value = true;
  previewDetail.value = null;
  try {
    previewDetail.value = await getLandingPage(pid, pageId);
  } catch {
    ElMessage.error('加载落地页详情失败');
    previewVisible.value = false;
  } finally {
    previewLoading.value = false;
  }
}

async function openPreview(row: LandingPageVo) {
  if (!canPreview(row) && row.status === 'DRAFT') {
    previewVisible.value = true;
    previewLoading.value = true;
    previewDetail.value = row as LandingPageDetailVo;
    try {
      previewDetail.value = await getLandingPage(projectId.value!, row.id);
    } catch {
      previewDetail.value = row as LandingPageDetailVo;
    } finally {
      previewLoading.value = false;
    }
    return;
  }
  await openPreviewById(row.id);
}

async function handleDelete(row: LandingPageVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(`确定删除落地页「${row.title}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteLandingPage(pid, row.id);
    ElMessage.success('已删除');
    await getList();
  } catch {
    /* cancelled */
  }
}

async function copySlug(slug: string) {
  try {
    await navigator.clipboard.writeText(`/${slug}`);
    ElMessage.success('已复制 slug');
  } catch {
    ElMessage.info(`/${slug}`);
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
    openCreateDialog(Number.isFinite(keywordId) && keywordId > 0 ? keywordId : undefined);
    const q = { ...route.query };
    delete q.action;
    delete q.keywordId;
    router.replace({ query: q });
  }
}

watch(
  () => route.params.projectId,
  () => {
    syncProjectFromRoute();
    getList();
    loadKeywordOptions();
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
  await loadKeywordOptions();
  await getList();
});
</script>

<style scoped lang="scss">
.tg-landing-pages {
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

  .title-link {
    font-weight: 500;
    padding: 0;
    height: auto;
  }

  .slug-mono {
    font-family: ui-monospace, monospace;
    font-size: var(--tg-font-size-sm, 13px);
  }

  .text-muted {
    color: var(--tg-color-text-placeholder, #9ca3af);
  }

  .text-secondary {
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .filter-hint {
    margin: var(--tg-space-3, 12px) 0 0;
    font-size: var(--tg-font-size-xs, 12px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .slug-hint {
    margin: 4px 0 0;
    font-size: var(--tg-font-size-xs, 12px);

    &--ok {
      color: var(--el-color-success);
    }

    &--err {
      color: var(--el-color-danger);
    }
  }

  .preview-body {
    min-height: 200px;
  }

  .preview-header {
    margin-bottom: var(--tg-space-4, 16px);
  }

  .preview-section {
    margin-bottom: var(--tg-space-4, 16px);

    h4 {
      margin: 0 0 var(--tg-space-2, 8px);
      font-size: var(--tg-font-size-sm, 13px);
      color: var(--tg-color-text-secondary, #6b7280);
    }
  }

  .preview-h1 {
    margin: 0 0 var(--tg-space-2, 8px);
    font-size: var(--tg-font-size-lg, 18px);
    font-weight: 600;
  }

  .preview-json {
    margin: 0;
    padding: var(--tg-space-2, 8px);
    background: var(--tg-color-bg-muted, #f3f4f6);
    border-radius: var(--tg-radius-sm, 4px);
    font-size: 11px;
    line-height: 1.5;
    overflow: auto;
    max-height: 240px;
  }

  .module-summary {
    margin: 0 0 var(--tg-space-2, 8px);
    line-height: 1.6;
    white-space: pre-wrap;
  }

  .form-tags {
    display: flex;
    flex-wrap: wrap;
  }

  .ml-2 {
    margin-left: 8px;
  }

  .mr-1 {
    margin-right: 4px;
  }

  .mb-1 {
    margin-bottom: 4px;
  }

  .mt-2 {
    margin-top: 8px;
  }
}

@media (max-width: 768px) {
  .tg-landing-pages :deep(.hidden-md-only) {
    display: none;
  }
}

@media (max-width: 992px) {
  .tg-landing-pages :deep(.hidden-sm-only) {
    display: none;
  }
}

@media (max-width: 768px) {
  .landing-preview-drawer :deep(.el-drawer) {
    width: 100% !important;
  }
}
</style>
