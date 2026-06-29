<template>
  <div v-loading="pageLoading" class="p-2 tg-project-detail">
    <el-page-header @back="goBack">
      <template #content>
        <span class="breadcrumb-text">客户项目 / {{ project?.name || '…' }}</span>
      </template>
    </el-page-header>

    <el-result v-if="loadError === '404'" icon="warning" title="项目不存在">
      <template #extra>
        <el-button type="primary" @click="goBack">返回列表</el-button>
      </template>
    </el-result>

    <template v-else-if="project">
      <el-card shadow="hover" class="header-card">
        <div class="header-row">
          <div class="header-main">
            <h1 class="project-title">{{ project.brandName }}</h1>
            <el-tag :type="statusMeta(project.status).type" effect="light">{{ statusMeta(project.status).label }}</el-tag>
          </div>
          <div class="header-actions">
            <el-button plain @click="setCurrent">设为当前项目</el-button>
            <el-button type="primary" @click="goDashboard">进入工作台</el-button>
          </div>
        </div>
        <p class="project-sub">{{ project.name }}</p>
        <div class="meta-tags">
          <el-tag v-for="m in project.targetMarkets" :key="m" size="small" class="mr-1">{{ m }}</el-tag>
          <el-tag v-for="lang in project.languages" :key="lang" size="small" type="info" class="mr-1">{{ lang }}</el-tag>
          <span v-if="project.updatedAt" class="meta-time">更新于 {{ formatTime(project.updatedAt) }}</span>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="onTabChange">
        <el-tab-pane label="品牌信息" name="brand">
          <el-form ref="brandFormRef" :model="brandForm" :rules="brandRules" label-width="110px" class="brand-form">
            <el-row :gutter="20">
              <el-col :lg="12" :span="24">
                <el-form-item label="项目名称" prop="name">
                  <el-input v-model="brandForm.name" maxlength="200" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="品牌名" prop="brandName">
                  <el-input v-model="brandForm.brandName" maxlength="200" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="官网" prop="website">
                  <el-input v-model="brandForm.website" placeholder="https://..." />
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="行业" prop="industry">
                  <el-select v-model="brandForm.industry" style="width: 100%">
                    <el-option v-for="opt in INDUSTRY_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="目标市场" prop="targetMarkets">
                  <el-select v-model="brandForm.targetMarkets" multiple style="width: 100%">
                    <el-option v-for="m in MARKET_OPTIONS" :key="m" :label="m" :value="m" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="服务语言" prop="languages">
                  <el-select v-model="brandForm.languages" multiple style="width: 100%">
                    <el-option v-for="lang in LANGUAGE_OPTIONS" :key="lang" :label="lang" :value="lang" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :lg="12" :span="24">
                <el-form-item label="状态" prop="status">
                  <el-select v-model="brandForm.status" style="width: 100%">
                    <el-option v-for="opt in PROJECT_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" :loading="brandSaving" @click="saveBrand">保存品牌信息</el-button>
            </el-form-item>
          </el-form>

          <el-divider content-position="left">产品路线（FR-003）</el-divider>
          <p class="section-hint">路线供内容 Agent / 落地页 Agent 引用</p>
          <div class="toolbar-row">
            <el-button type="primary" plain icon="Plus" @click="openProductDrawer()">添加路线</el-button>
          </div>
          <el-table v-loading="productsLoading" border :data="products" empty-text="暂无路线">
            <el-table-column label="路线名称" prop="name" min-width="200" show-overflow-tooltip />
            <el-table-column label="目的地" min-width="160">
              <template #default="{ row }">
                <el-tag v-for="d in row.destinations" :key="d" size="small" class="mr-1">{{ d }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="天数" prop="days" width="80" align="center" />
            <el-table-column label="价格区间" prop="priceRange" width="120" show-overflow-tooltip />
            <el-table-column label="适合人群" prop="suitableFor" width="140" show-overflow-tooltip class-name="hidden-md-only" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openProductDrawer(row)">编辑</el-button>
                <el-popconfirm title="确认删除该路线？" @confirm="removeProduct(row.id)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无路线">
                <el-button type="primary" @click="openProductDrawer()">添加第一条路线</el-button>
              </el-empty>
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="竞品" name="competitors">
          <el-alert type="info" show-icon :closable="false" title="建议至少维护 5 个竞品，用于 GEO 诊断对比（FR-002）" class="mb-3" />
          <div class="toolbar-row">
            <span class="competitor-count">
              已添加 {{ competitors.length }} 个竞品
              <el-tag v-if="competitors.length < 5" type="warning" size="small" class="ml-2">建议 ≥5</el-tag>
            </span>
            <el-button type="primary" plain icon="Plus" @click="openCompetitorDrawer()">添加竞品</el-button>
          </div>
          <el-table v-loading="competitorsLoading" border :data="competitors">
            <el-table-column label="竞品名称" prop="name" min-width="160" show-overflow-tooltip />
            <el-table-column label="官网" min-width="140">
              <template #default="{ row }">
                <el-link v-if="row.website" :href="row.website" target="_blank" type="primary">{{ row.website }}</el-link>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="社媒" width="160">
              <template #default="{ row }">
                <template v-for="p in SOCIAL_PLATFORMS" :key="p.key">
                  <el-tooltip v-if="row.socialLinks?.[p.key]" :content="row.socialLinks[p.key]" placement="top">
                    <el-button link type="primary" @click="openUrl(row.socialLinks[p.key])">{{ p.label }}</el-button>
                  </el-tooltip>
                </template>
                <span v-if="!hasSocial(row)">—</span>
              </template>
            </el-table-column>
            <el-table-column label="主推产品" prop="mainProducts" min-width="160" show-overflow-tooltip />
            <el-table-column label="备注" prop="notes" width="120" show-overflow-tooltip class-name="hidden-md-only" />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCompetitorDrawer(row)">编辑</el-button>
                <el-popconfirm title="确认删除该竞品？" @confirm="removeCompetitor(row.id)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无竞品">
                <el-button type="primary" @click="openCompetitorDrawer()">添加第一个竞品</el-button>
              </el-empty>
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="知识库" name="knowledge">
          <div class="toolbar-row">
            <el-button type="primary" icon="Upload" @click="openUploadDialog">上传资料</el-button>
            <el-button icon="Refresh" @click="loadKnowledge">刷新</el-button>
          </div>
          <el-form :inline="true" class="filter-form mb-2">
            <el-form-item label="标题">
              <el-input v-model="knowledgeQuery.title" clearable placeholder="模糊搜索" @keyup.enter="loadKnowledge" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="knowledgeQuery.type" clearable placeholder="全部" style="width: 130px">
                <el-option v-for="opt in KNOWLEDGE_ASSET_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="向量化">
              <el-select v-model="knowledgeQuery.vectorStatus" clearable placeholder="全部" style="width: 120px">
                <el-option v-for="opt in VECTOR_STATUS_OPTIONS" :key="opt.value || 'all'" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadKnowledge">搜索</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="knowledgeLoading" border :data="knowledgeList">
            <el-table-column label="标题" min-width="200">
              <template #default="{ row }">
                <el-link v-if="row.fileUrl" type="primary" :href="row.fileUrl" target="_blank">{{ row.title }}</el-link>
                <span v-else>{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="{ row }">
                <el-tag size="small">{{ KNOWLEDGE_ASSET_TYPE_META[row.type] || row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标签" min-width="120">
              <template #default="{ row }">
                <el-tag v-for="t in row.tags" :key="t" size="small" type="info" class="mr-1">{{ t }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="向量化" width="120">
              <template #default="{ row }">
                <el-tag :type="vectorMeta(row.vectorStatus).type" size="small">{{ vectorMeta(row.vectorStatus).label }}</el-tag>
                <el-progress v-if="row.vectorStatus === 'INDEXING'" :percentage="50" :show-text="false" status="success" class="indexing-bar" />
              </template>
            </el-table-column>
            <el-table-column label="上传时间" prop="createdAt" width="170" />
            <el-table-column label="操作" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openKnowledgeMeta(row)">编辑</el-button>
                <el-tooltip content="向量化完成后可用（EPIC-10）" placement="top">
                  <el-button link disabled>检索预览</el-button>
                </el-tooltip>
                <el-popconfirm title="确认删除该资料？" @confirm="removeKnowledge(row.id)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="knowledgeTotal > 0"
            v-model:page="knowledgeQuery.pageNum"
            v-model:limit="knowledgeQuery.pageSize"
            :total="knowledgeTotal"
            @pagination="loadKnowledge"
          />
          <el-alert
            type="info"
            show-icon
            :closable="false"
            class="mt-3"
            title="资料将向量化供 RAG 引用；生成内容将标注 chunk 来源。"
          />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 路线抽屉 -->
    <el-drawer v-model="productDrawerVisible" :title="productDrawerTitle" size="480px" destroy-on-close>
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="100px">
        <el-form-item label="路线名称" prop="name">
          <el-input v-model="productForm.name" maxlength="300" />
        </el-form-item>
        <el-form-item label="目的地" prop="destinations">
          <el-select v-model="productForm.destinations" multiple filterable allow-create style="width: 100%">
            <el-option v-for="d in DESTINATION_OPTIONS" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="行程天数" prop="days">
          <el-input-number v-model="productForm.days" :min="1" :max="365" controls-position="right" />
        </el-form-item>
        <el-form-item label="价格区间" prop="priceRange">
          <el-input v-model="productForm.priceRange" placeholder="如 $2000–$3500" />
        </el-form-item>
        <el-form-item label="适合人群" prop="suitableFor">
          <el-input v-model="productForm.suitableFor" />
        </el-form-item>
        <el-form-item label="亮点" prop="highlights">
          <el-input v-model="productForm.highlights" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="服务包含" prop="inclusions">
          <el-input v-model="productForm.inclusions" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="productSaving" @click="submitProduct">保存</el-button>
      </template>
    </el-drawer>

    <!-- 竞品抽屉 -->
    <el-drawer v-model="competitorDrawerVisible" :title="competitorDrawerTitle" size="480px" destroy-on-close>
      <el-form ref="competitorFormRef" :model="competitorForm" :rules="competitorRules" label-width="100px">
        <el-form-item label="竞品名称" prop="name">
          <el-input v-model="competitorForm.name" maxlength="200" />
        </el-form-item>
        <el-form-item label="官网" prop="website">
          <el-input v-model="competitorForm.website" placeholder="https://..." />
        </el-form-item>
        <el-form-item v-for="p in SOCIAL_PLATFORMS" :key="p.key" :label="p.label">
          <el-input v-model="competitorForm.socialLinks[p.key]" placeholder="URL" />
        </el-form-item>
        <el-form-item label="主推产品" prop="mainProducts">
          <el-input v-model="competitorForm.mainProducts" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input v-model="competitorForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="competitorDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="competitorSaving" @click="submitCompetitor">保存</el-button>
      </template>
    </el-drawer>

    <!-- 上传 dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传知识资料" width="560px" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="文件">
          <el-upload
            drag
            :limit="1"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :before-upload="beforeUpload"
            :on-success="onUploadSuccess"
            :on-error="onUploadError"
            accept=".doc,.docx,.pdf,.txt,.png,.jpg,.jpeg"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽或点击上传（≤20MB）</div>
          </el-upload>
          <p v-if="uploadForm.fileUrl" class="upload-hint">已上传：{{ uploadForm.fileUrl }}</p>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="uploadForm.title" maxlength="500" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="uploadForm.type" style="width: 100%">
            <el-option v-for="opt in KNOWLEDGE_ASSET_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="uploadForm.tags" multiple filterable allow-create style="width: 100%" />
        </el-form-item>
        <el-form-item label="文本内容" prop="content">
          <el-input v-model="uploadForm.content" type="textarea" :rows="4" placeholder="TXT 直传或粘贴；上传文件时可留空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadSaving" @click="submitUpload">提交</el-button>
      </template>
    </el-dialog>

    <!-- 知识库元数据 -->
    <el-drawer v-model="knowledgeMetaVisible" title="编辑资料元数据" size="480px" destroy-on-close>
      <el-form ref="knowledgeMetaRef" :model="knowledgeMetaForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="knowledgeMetaForm.title" maxlength="500" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="knowledgeMetaForm.type" style="width: 100%">
            <el-option v-for="opt in KNOWLEDGE_ASSET_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select v-model="knowledgeMetaForm.tags" multiple filterable allow-create style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="knowledgeMetaVisible = false">取消</el-button>
        <el-button type="primary" :loading="knowledgeMetaSaving" @click="submitKnowledgeMeta">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="ProjectDetail" lang="ts">
import { UploadFilled } from '@element-plus/icons-vue';
import {
  createCompetitor,
  createKnowledgeAsset,
  createTravelProduct,
  deleteCompetitor,
  deleteKnowledgeAsset,
  deleteTravelProduct,
  getProject,
  listCompetitors,
  listKnowledgeAssets,
  listTravelProducts,
  updateCompetitor,
  updateKnowledgeAsset,
  updateProject,
  updateTravelProduct
} from '@/api/tourgeo/project';
import type {
  CompetitorForm,
  CompetitorVo,
  CustomerProjectForm,
  CustomerProjectVo,
  KnowledgeAssetForm,
  KnowledgeAssetVo,
  TravelProductForm,
  TravelProductVo
} from '@/api/tourgeo/types';
import {
  DESTINATION_OPTIONS,
  ENTITY_STATUS_META,
  INDUSTRY_OPTIONS,
  KNOWLEDGE_ASSET_TYPE_META,
  KNOWLEDGE_ASSET_TYPE_OPTIONS,
  LANGUAGE_OPTIONS,
  MARKET_OPTIONS,
  PROJECT_STATUS_OPTIONS,
  SOCIAL_PLATFORMS,
  VECTOR_STATUS_META,
  VECTOR_STATUS_OPTIONS
} from '@/constants/project';
import { useProjectStore } from '@/store/modules/project';
import { globalHeaders } from '@/utils/request';

type DetailTab = 'brand' | 'competitors' | 'knowledge';

const POLL_MS = 30000;

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const route = useRoute();
const projectStore = useProjectStore();

const projectId = computed(() => Number(route.params.projectId));
const pageLoading = ref(true);
const loadError = ref<'404' | null>(null);
const project = ref<CustomerProjectVo | null>(null);

const activeTab = ref<DetailTab>('brand');
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null);

const brandFormRef = ref<ElFormInstance>();
const brandSaving = ref(false);
const brandForm = reactive<CustomerProjectForm & { status?: string }>({
  name: '',
  brandName: '',
  website: '',
  industry: 'inbound_tourism',
  targetMarkets: [],
  languages: [],
  status: 'ACTIVE'
});

const brandRules: ElFormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  brandName: [{ required: true, message: '请输入品牌名', trigger: 'blur' }],
  targetMarkets: [{ required: true, type: 'array', min: 1, message: '至少选择一个市场', trigger: 'change' }],
  languages: [{ required: true, type: 'array', min: 1, message: '至少选择一种语言', trigger: 'change' }]
};

const products = ref<TravelProductVo[]>([]);
const productsLoading = ref(false);
const productDrawerVisible = ref(false);
const productSaving = ref(false);
const editingProductId = ref<number | null>(null);
const productFormRef = ref<ElFormInstance>();
const defaultProductForm = (): TravelProductForm => ({
  name: '',
  destinations: [],
  days: undefined,
  priceRange: '',
  suitableFor: '',
  highlights: '',
  inclusions: ''
});
const productForm = reactive<TravelProductForm>(defaultProductForm());
const productRules: ElFormRules = {
  name: [{ required: true, message: '请输入路线名称', trigger: 'blur' }],
  destinations: [{ required: true, type: 'array', min: 1, message: '至少选择一个目的地', trigger: 'change' }]
};
const productDrawerTitle = computed(() => (editingProductId.value ? '编辑路线' : '添加路线'));

const competitors = ref<CompetitorVo[]>([]);
const competitorsLoading = ref(false);
const competitorDrawerVisible = ref(false);
const competitorSaving = ref(false);
const editingCompetitorId = ref<number | null>(null);
const competitorFormRef = ref<ElFormInstance>();
const defaultCompetitorForm = (): CompetitorForm => ({
  name: '',
  website: '',
  socialLinks: { instagram: '', facebook: '', youtube: '', tiktok: '' },
  mainProducts: '',
  notes: ''
});
const competitorForm = reactive<CompetitorForm>(defaultCompetitorForm());
const competitorRules: ElFormRules = {
  name: [{ required: true, message: '请输入竞品名称', trigger: 'blur' }]
};
const competitorDrawerTitle = computed(() => (editingCompetitorId.value ? '编辑竞品' : '添加竞品'));

const knowledgeList = ref<KnowledgeAssetVo[]>([]);
const knowledgeTotal = ref(0);
const knowledgeLoading = ref(false);
const knowledgeQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  type: '' as KnowledgeAssetVo['type'] | '',
  vectorStatus: '' as KnowledgeAssetVo['vectorStatus'] | ''
});

const uploadDialogVisible = ref(false);
const uploadSaving = ref(false);
const uploadFormRef = ref<ElFormInstance>();
const uploadForm = reactive<KnowledgeAssetForm>({
  title: '',
  type: 'DOCUMENT',
  content: '',
  fileUrl: '',
  tags: []
});
const uploadRules: ElFormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
};

const baseUrl = import.meta.env.VITE_APP_BASE_API;
const uploadUrl = `${baseUrl}/resource/oss/upload`;
const uploadHeaders = globalHeaders();

const knowledgeMetaVisible = ref(false);
const knowledgeMetaSaving = ref(false);
const knowledgeMetaRef = ref<ElFormInstance>();
const editingAssetId = ref<number | null>(null);
const knowledgeMetaForm = reactive<KnowledgeAssetForm>({
  title: '',
  type: 'DOCUMENT',
  tags: []
});

const needsKnowledgePoll = computed(() =>
  knowledgeList.value.some((a) => a.vectorStatus === 'PENDING' || a.vectorStatus === 'INDEXING')
);

function statusMeta(status: string) {
  return ENTITY_STATUS_META[status] ?? { label: status, type: 'info' as const };
}

function vectorMeta(status: string) {
  return VECTOR_STATUS_META[status] ?? { label: status, type: 'info' as const };
}

function formatTime(iso: string) {
  return iso.replace('T', ' ').slice(0, 16);
}

function hasSocial(row: CompetitorVo) {
  const links = row.socialLinks ?? {};
  return SOCIAL_PLATFORMS.some((p) => links[p.key]);
}

function openUrl(url: string) {
  window.open(url, '_blank');
}

function goBack() {
  router.push({ name: 'ProjectList' });
}

function goDashboard() {
  router.push('/dashboard');
}

function setCurrent() {
  if (project.value) {
    projectStore.setCurrentProject(project.value.id);
    proxy?.$modal.msgSuccess('已设为当前项目');
  }
}

function onTabChange(name: string | number) {
  router.replace({ query: { ...route.query, tab: name } });
}

function fillBrandForm(p: CustomerProjectVo) {
  Object.assign(brandForm, {
    name: p.name,
    brandName: p.brandName,
    website: p.website ?? '',
    industry: p.industry ?? 'inbound_tourism',
    targetMarkets: [...p.targetMarkets],
    languages: [...p.languages],
    status: p.status
  });
}

async function loadProject() {
  if (!projectId.value || Number.isNaN(projectId.value)) {
    loadError.value = '404';
    return;
  }
  try {
    const res = await getProject(projectId.value);
    project.value = res.data;
    fillBrandForm(res.data);
    loadError.value = null;
  } catch {
    project.value = null;
    loadError.value = '404';
  }
}

async function saveBrand() {
  await brandFormRef.value?.validate();
  brandSaving.value = true;
  try {
    await updateProject(projectId.value, { ...brandForm });
    proxy?.$modal.msgSuccess('品牌信息已保存');
    await loadProject();
    await projectStore.fetchProjects();
  } finally {
    brandSaving.value = false;
  }
}

async function loadProducts() {
  productsLoading.value = true;
  try {
    products.value = await listTravelProducts(projectId.value);
  } finally {
    productsLoading.value = false;
  }
}

function openProductDrawer(row?: TravelProductVo) {
  if (row) {
    editingProductId.value = row.id;
    Object.assign(productForm, {
      name: row.name,
      destinations: [...row.destinations],
      days: row.days,
      priceRange: row.priceRange ?? '',
      suitableFor: row.suitableFor ?? '',
      highlights: row.highlights ?? '',
      inclusions: row.inclusions ?? ''
    });
  } else {
    editingProductId.value = null;
    Object.assign(productForm, defaultProductForm());
  }
  productDrawerVisible.value = true;
}

async function submitProduct() {
  await productFormRef.value?.validate();
  productSaving.value = true;
  try {
    if (editingProductId.value) {
      await updateTravelProduct(projectId.value, editingProductId.value, { ...productForm });
      proxy?.$modal.msgSuccess('路线已更新');
    } else {
      await createTravelProduct(projectId.value, { ...productForm });
      proxy?.$modal.msgSuccess('路线已添加');
    }
    productDrawerVisible.value = false;
    await loadProducts();
  } finally {
    productSaving.value = false;
  }
}

async function removeProduct(id: number) {
  await deleteTravelProduct(projectId.value, id);
  proxy?.$modal.msgSuccess('已删除');
  await loadProducts();
}

async function loadCompetitors() {
  competitorsLoading.value = true;
  try {
    competitors.value = await listCompetitors(projectId.value);
  } finally {
    competitorsLoading.value = false;
  }
}

function openCompetitorDrawer(row?: CompetitorVo) {
  if (row) {
    editingCompetitorId.value = row.id;
    Object.assign(competitorForm, {
      name: row.name,
      website: row.website ?? '',
      socialLinks: {
        instagram: row.socialLinks?.instagram ?? '',
        facebook: row.socialLinks?.facebook ?? '',
        youtube: row.socialLinks?.youtube ?? '',
        tiktok: row.socialLinks?.tiktok ?? ''
      },
      mainProducts: row.mainProducts ?? '',
      notes: row.notes ?? ''
    });
  } else {
    editingCompetitorId.value = null;
    Object.assign(competitorForm, defaultCompetitorForm());
  }
  competitorDrawerVisible.value = true;
}

async function submitCompetitor() {
  await competitorFormRef.value?.validate();
  competitorSaving.value = true;
  try {
    const links = competitorForm.socialLinks ?? {};
    const socialLinks = Object.fromEntries(Object.entries(links).filter(([, v]) => v && String(v).trim()));
    const payload = { ...competitorForm, socialLinks };
    if (editingCompetitorId.value) {
      await updateCompetitor(projectId.value, editingCompetitorId.value, payload);
      proxy?.$modal.msgSuccess('竞品已更新');
    } else {
      await createCompetitor(projectId.value, payload);
      proxy?.$modal.msgSuccess('竞品已添加');
    }
    competitorDrawerVisible.value = false;
    await loadCompetitors();
  } finally {
    competitorSaving.value = false;
  }
}

async function removeCompetitor(id: number) {
  await deleteCompetitor(projectId.value, id);
  proxy?.$modal.msgSuccess('已删除');
  await loadCompetitors();
}

async function loadKnowledge() {
  knowledgeLoading.value = true;
  try {
    const res = await listKnowledgeAssets(projectId.value, knowledgeQuery);
    knowledgeList.value = res.rows ?? [];
    knowledgeTotal.value = res.total ?? 0;
  } finally {
    knowledgeLoading.value = false;
  }
}

function openUploadDialog() {
  Object.assign(uploadForm, {
    title: '',
    type: 'DOCUMENT',
    content: '',
    fileUrl: '',
    tags: []
  });
  uploadDialogVisible.value = true;
}

function beforeUpload(file: UploadRawFile) {
  const ext = file.name.split('.').pop()?.toLowerCase();
  const allowed = ['doc', 'docx', 'pdf', 'txt', 'png', 'jpg', 'jpeg'];
  if (!ext || !allowed.includes(ext)) {
    proxy?.$modal.msgError('文件格式不支持');
    return false;
  }
  if (file.size / 1024 / 1024 > 20) {
    proxy?.$modal.msgError('文件不能超过 20MB');
    return false;
  }
  if (!uploadForm.title) {
    uploadForm.title = file.name.replace(/\.[^.]+$/, '');
  }
  proxy?.$modal.loading('正在上传…');
  return true;
}

function onUploadSuccess(res: { code: number; data?: { url?: string }; msg?: string }) {
  proxy?.$modal.closeLoading();
  if (res.code === 200 && res.data?.url) {
    uploadForm.fileUrl = res.data.url;
    proxy?.$modal.msgSuccess('文件上传成功');
  } else {
    proxy?.$modal.msgError(res.msg || '上传失败');
  }
}

function onUploadError() {
  proxy?.$modal.closeLoading();
  proxy?.$modal.msgError('上传失败');
}

async function submitUpload() {
  await uploadFormRef.value?.validate();
  if (!uploadForm.fileUrl && !uploadForm.content?.trim()) {
    proxy?.$modal.msgError('请上传文件或填写文本内容');
    return;
  }
  uploadSaving.value = true;
  try {
    await createKnowledgeAsset(projectId.value, { ...uploadForm });
    proxy?.$modal.msgSuccess('已提交，向量化处理中');
    uploadDialogVisible.value = false;
    knowledgeQuery.pageNum = 1;
    await loadKnowledge();
    startKnowledgePoll();
  } finally {
    uploadSaving.value = false;
  }
}

function openKnowledgeMeta(row: KnowledgeAssetVo) {
  editingAssetId.value = row.id;
  Object.assign(knowledgeMetaForm, {
    title: row.title,
    type: row.type,
    tags: [...(row.tags ?? [])]
  });
  knowledgeMetaVisible.value = true;
}

async function submitKnowledgeMeta() {
  await knowledgeMetaRef.value?.validate();
  if (!editingAssetId.value) return;
  knowledgeMetaSaving.value = true;
  try {
    await updateKnowledgeAsset(projectId.value, editingAssetId.value, { ...knowledgeMetaForm });
    proxy?.$modal.msgSuccess('已保存');
    knowledgeMetaVisible.value = false;
    await loadKnowledge();
  } finally {
    knowledgeMetaSaving.value = false;
  }
}

async function removeKnowledge(id: number) {
  await deleteKnowledgeAsset(projectId.value, id);
  proxy?.$modal.msgSuccess('已删除');
  await loadKnowledge();
}

function startKnowledgePoll() {
  stopKnowledgePoll();
  if (activeTab.value !== 'knowledge' || !needsKnowledgePoll.value) return;
  pollTimer.value = setInterval(() => {
    if (activeTab.value === 'knowledge' && needsKnowledgePoll.value) {
      loadKnowledge();
    } else {
      stopKnowledgePoll();
    }
  }, POLL_MS);
}

function stopKnowledgePoll() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
}

async function loadTabData(tab: DetailTab) {
  if (tab === 'brand') await loadProducts();
  else if (tab === 'competitors') await loadCompetitors();
  else if (tab === 'knowledge') {
    await loadKnowledge();
    startKnowledgePoll();
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    const t = (tab as DetailTab) || 'brand';
    if (['brand', 'competitors', 'knowledge'].includes(t)) {
      activeTab.value = t;
    }
  },
  { immediate: true }
);

watch(activeTab, (tab) => {
  loadTabData(tab);
  if (tab !== 'knowledge') stopKnowledgePoll();
});

onMounted(async () => {
  pageLoading.value = true;
  await loadProject();
  if (project.value) {
    projectStore.setCurrentProject(project.value.id);
    await loadTabData(activeTab.value);
  }
  pageLoading.value = false;
});

onBeforeUnmount(() => {
  stopKnowledgePoll();
});
</script>

<style scoped lang="scss">
.breadcrumb-text {
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.header-card {
  margin-top: var(--tg-space-4);
}

.header-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--tg-space-3);
}

.header-main {
  display: flex;
  align-items: center;
  gap: var(--tg-space-3);
}

.project-title {
  margin: 0;
  font-size: var(--tg-font-size-lg);
  font-weight: 600;
}

.project-sub {
  margin: var(--tg-space-2) 0;
  color: var(--tg-color-text-secondary);
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--tg-space-1);
}

.meta-time {
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
  margin-left: var(--tg-space-2);
}

.detail-tabs {
  margin-top: var(--tg-space-4);
}

.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--tg-space-3);
  margin-bottom: var(--tg-space-3);
}

.section-hint {
  margin: 0 0 var(--tg-space-3);
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
}

.competitor-count {
  font-size: var(--tg-font-size-sm);
}

.indexing-bar {
  margin-top: 4px;
  max-width: 80px;
}

.upload-hint {
  margin-top: var(--tg-space-2);
  font-size: var(--tg-font-size-sm);
  color: var(--tg-color-text-secondary);
  word-break: break-all;
}

@media (max-width: 992px) {
  :deep(.hidden-md-only) {
    display: none;
  }
}
</style>
