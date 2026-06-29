<template>
  <div class="p-2 tg-projects">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">客户项目</h1>
        <p class="tg-page-sub">管理租户内客户项目（FR-001）</p>
      </div>
      <el-button type="primary" icon="Plus" @click="openDrawer()">新建客户项目</el-button>
    </div>

    <transition :enter-active-class="proxy?.animate.searchAnimate.enter" :leave-active-class="proxy?.animate.searchAnimate.leave">
      <div v-show="showSearch" class="mb-[10px]">
        <el-card shadow="hover">
          <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="80px">
            <el-form-item label="项目名称" prop="name">
              <el-input v-model="queryParams.name" placeholder="模糊搜索" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="品牌名" prop="brandName">
              <el-input v-model="queryParams.brandName" placeholder="模糊搜索" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
                <el-option v-for="opt in statusOptions" :key="opt.value || 'all'" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="目标市场" prop="market">
              <el-select v-model="queryParams.market" placeholder="全部" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option v-for="m in MARKET_OPTIONS" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
            <el-form-item label="创建时间" prop="dateRange">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="-"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD HH:mm:ss"
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
            <el-button type="primary" icon="Plus" @click="openDrawer()">新建</el-button>
          </el-col>
          <right-toolbar v-model:show-search="showSearch" @query-table="getList" />
        </el-row>
      </template>

      <el-table v-loading="loading" border :data="projectList">
        <el-table-column label="项目名称" prop="name" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="enterProject(row)">{{ row.name }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="品牌名" prop="brandName" min-width="160" show-overflow-tooltip />
        <el-table-column label="目标市场" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="m in row.targetMarkets" :key="m" size="small" class="mr-1">{{ m }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务语言" min-width="100" class-name="hidden-sm-only">
          <template #default="{ row }">
            <el-tag v-for="lang in row.languages" :key="lang" size="small" type="info" class="mr-1">{{ lang }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="170" />
        <el-table-column label="操作" fixed="right" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="enterProject(row)">进入</el-button>
            <el-button link type="primary" @click="openDrawer(row)">编辑</el-button>
            <el-popconfirm title="确认软删除该项目？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无客户项目">
            <el-button type="primary" @click="openDrawer()">新建客户项目</el-button>
          </el-empty>
        </template>
      </el-table>

      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="品牌名" prop="brandName">
          <el-input v-model="form.brandName" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="官网" prop="website">
          <el-input v-model="form.website" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="行业" prop="industry">
          <el-select v-model="form.industry" style="width: 100%">
            <el-option v-for="opt in INDUSTRY_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标市场" prop="targetMarkets">
          <el-select v-model="form.targetMarkets" multiple style="width: 100%">
            <el-option v-for="m in MARKET_OPTIONS" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务语言" prop="languages">
          <el-select v-model="form.languages" multiple style="width: 100%">
            <el-option v-for="lang in LANGUAGE_OPTIONS" :key="lang" :label="lang" :value="lang" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button @click="submit(false)">仅保存</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submit(true)">保存并进入工作台</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="TourgeoProjects" lang="ts">
import { createProject, deleteProject, listProjects, updateProject } from '@/api/tourgeo/project';
import type { CustomerProjectForm, CustomerProjectQuery, CustomerProjectVo } from '@/api/tourgeo/types';
import { ENTITY_STATUS_META, ENTITY_STATUS_OPTIONS, INDUSTRY_OPTIONS, LANGUAGE_OPTIONS, MARKET_OPTIONS } from '@/constants/project';
import { useProjectStore } from '@/store/modules/project';
import { addDateRange } from '@/utils/ruoyi';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const route = useRoute();
const projectStore = useProjectStore();

const statusOptions = ENTITY_STATUS_OPTIONS;
const loading = ref(true);
const showSearch = ref(true);
const projectList = ref<CustomerProjectVo[]>([]);
const total = ref(0);
const drawerVisible = ref(false);
const submitLoading = ref(false);
const editingId = ref<number | null>(null);
const dateRange = ref<string[]>([]);

const queryFormRef = ref<ElFormInstance>();
const formRef = ref<ElFormInstance>();

const queryParams = reactive<CustomerProjectQuery>({
  pageNum: 1,
  pageSize: 10,
  name: '',
  brandName: '',
  status: '',
  market: ''
});

const defaultForm = (): CustomerProjectForm => ({
  name: '',
  brandName: '',
  website: '',
  industry: 'inbound_tourism',
  targetMarkets: ['US'],
  languages: ['en']
});

const form = reactive<CustomerProjectForm>(defaultForm());

const rules = ref<ElFormRules>({
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  brandName: [{ required: true, message: '请输入品牌名', trigger: 'blur' }],
  targetMarkets: [{ required: true, type: 'array', min: 1, message: '至少选择一个市场', trigger: 'change' }],
  languages: [{ required: true, type: 'array', min: 1, message: '至少选择一种语言', trigger: 'change' }]
});

const drawerTitle = computed(() => (editingId.value ? `编辑项目 · ${form.name}` : '新建客户项目'));

function statusMeta(status: string) {
  return ENTITY_STATUS_META[status] ?? { label: status, type: 'info' as const };
}

async function getList() {
  loading.value = true;
  try {
    const params = addDateRange({ ...queryParams }, dateRange.value, 'CreateTime');
    const res = await listProjects(params);
    projectList.value = res.rows ?? [];
    total.value = res.total ?? 0;
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
  dateRange.value = [];
  queryParams.pageNum = 1;
  getList();
}

function openDrawer(row?: CustomerProjectVo) {
  if (row) {
    editingId.value = row.id;
    Object.assign(form, {
      name: row.name,
      brandName: row.brandName,
      website: row.website ?? '',
      industry: row.industry ?? 'inbound_tourism',
      targetMarkets: [...row.targetMarkets],
      languages: [...row.languages]
    });
  } else {
    editingId.value = null;
    Object.assign(form, defaultForm());
  }
  drawerVisible.value = true;
}

async function submit(goDashboard: boolean) {
  await formRef.value?.validate();
  submitLoading.value = true;
  try {
    let projectId = editingId.value;
    if (editingId.value) {
      await updateProject(editingId.value, { ...form });
      proxy?.$modal.msgSuccess('更新成功');
    } else {
      const res = await createProject({ ...form });
      projectId = res.data;
      proxy?.$modal.msgSuccess('创建成功');
    }
    drawerVisible.value = false;
    await getList();
    await projectStore.fetchProjects();
    if (projectId != null) {
      projectStore.setCurrentProject(projectId);
    }
    if (goDashboard && projectId != null) {
      router.push('/dashboard');
    }
  } finally {
    submitLoading.value = false;
  }
}

function enterProject(row: CustomerProjectVo) {
  projectStore.setCurrentProject(row.id);
  router.push('/dashboard');
}

async function handleDelete(id: number) {
  await deleteProject(id);
  proxy?.$modal.msgSuccess('已删除');
  await getList();
  await projectStore.fetchProjects();
}

onMounted(async () => {
  await getList();
  if (route.query.create === '1') {
    openDrawer();
  }
});
</script>

<style scoped lang="scss">
.tg-page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--tg-space-4);
  margin-bottom: var(--tg-space-5);
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

@media (max-width: 992px) {
  :deep(.hidden-sm-only) {
    display: none;
  }
}
</style>
