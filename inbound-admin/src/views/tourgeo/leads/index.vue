<template>
  <div class="p-2 tg-leads">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">询盘线索</h1>
        <p class="tg-page-sub">线索与转化 · 落地页表单提交记录（FR-601）</p>
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
          <el-button disabled>导出线索</el-button>
          <el-form :inline="true" class="toolbar-form" @submit.prevent="handleQuery">
            <el-form-item label="姓名">
              <el-input v-model="queryParams.name" placeholder="模糊" clearable style="width: 110px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="queryParams.email" placeholder="模糊" clearable style="width: 130px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="电话" class="hidden-sm-only">
              <el-input v-model="queryParams.phone" placeholder="模糊" clearable style="width: 120px" @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="来源">
              <el-select v-model="queryParams.source" placeholder="全部" clearable style="width: 100px">
                <el-option label="全部" value="" />
                <el-option v-for="opt in LEAD_SOURCE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px">
                <el-option label="全部" value="" />
                <el-option v-for="opt in LEAD_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="落地页" class="hidden-md-only">
              <el-input
                v-model="queryParams.landingPage"
                placeholder="当前页筛选"
                clearable
                style="width: 120px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item label="关键词" class="hidden-md-only">
              <el-input
                v-model="queryParams.keyword"
                placeholder="当前页筛选"
                clearable
                style="width: 120px"
                @keyup.enter="handleQuery"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
        <p v-if="clientFilterActive" class="filter-hint">落地页 / 关键词为 M1 当前页客户端筛选；姓名 / 邮箱 / 电话 / 状态走服务端分页。</p>
      </el-card>

      <el-card shadow="hover">
        <el-table v-loading="loading" border :data="displayList">
          <template #empty>
            <el-empty :description="emptyDescription">
              <template v-if="!hasActiveFilters">
                <p class="empty-sub">发布落地页并开启表单后，提交将出现在此</p>
                <el-button type="primary" @click="goLandingPages">前往落地页</el-button>
              </template>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="姓名" width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" class="name-link" @click="openDetail(row)">
                {{ row.name?.trim() || '—' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ maskEmail(row.email) }}</template>
          </el-table-column>
          <el-table-column label="电话" width="130" class-name="hidden-md-only" show-overflow-tooltip>
            <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="sourceMeta(row.source).type">{{ sourceMeta(row.source).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="落地页" min-width="160" class-name="hidden-sm-only">
            <template #default="{ row }">
              <div v-if="row.landingPageTitle" class="landing-cell">
                <span class="landing-title">{{ row.landingPageTitle }}</span>
                <span v-if="row.landingPageSlug" class="landing-slug">/{{ row.landingPageSlug }}</span>
              </div>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="关键词" min-width="140" class-name="hidden-md-only" show-overflow-tooltip>
            <template #default="{ row }">{{ row.keywordText || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

        <el-alert type="info" closable show-icon class="mt-3 compliance-alert" title="线索含个人信息，请勿外传；导出需授权。" />
        <p class="page-hint">线索由落地页公开表单写入；Admin 不可手工创建。</p>
      </el-card>
    </template>

    <el-drawer
      v-model="detailVisible"
      :title="detailTitle"
      size="640px"
      destroy-on-close
      class="lead-detail-drawer"
      @closed="resetDetail"
    >
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <el-tag :type="statusMeta(detail.status).type">{{ statusMeta(detail.status).label }}</el-tag>
            <span class="detail-time">提交于 {{ formatTime(detail.createdAt) }}</span>
            <span class="detail-id">#{{ detail.id }}</span>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <el-tab-pane label="CRM 跟进" name="crm">
              <div class="crm-section">
                <div class="crm-row">
                  <span class="crm-label">状态</span>
                  <el-tooltip v-if="isTerminal" content="终态不可变更" placement="top">
                    <el-select v-model="editStatus" disabled style="width: 160px">
                      <el-option
                        v-for="opt in statusOptions"
                        :key="opt"
                        :label="statusMeta(opt).label"
                        :value="opt"
                      />
                    </el-select>
                  </el-tooltip>
                  <el-select v-else v-model="editStatus" style="width: 160px" :disabled="statusSaving">
                    <el-option
                      v-for="opt in statusOptions"
                      :key="opt"
                      :label="statusMeta(opt).label"
                      :value="opt"
                    />
                  </el-select>
                  <el-button
                    type="primary"
                    plain
                    :disabled="!statusDirty || isTerminal"
                    :loading="statusSaving"
                    @click="saveStatus"
                  >
                    保存状态
                  </el-button>
                  <span v-if="statusDirty && !isTerminal" class="unsaved-hint">未保存</span>
                </div>

                <div class="crm-row">
                  <span class="crm-label">负责人</span>
                  <span class="assignee-name">{{ detail.assigneeName?.trim() || '—' }}</span>
                  <el-button link type="primary" :loading="assigneeSaving" @click="assignToMe">指派给我</el-button>
                </div>
                <p v-if="!detail.assigneeName" class="crm-hint">尚未指派负责人，可点击「指派给我」</p>
              </div>

              <h4 class="section-title">添加跟进</h4>
              <el-form label-position="top" class="followup-form" @submit.prevent="submitFollowup">
                <el-form-item label="跟进内容" required>
                  <el-input
                    v-model="followupForm.content"
                    type="textarea"
                    :rows="3"
                    maxlength="2000"
                    show-word-limit
                    placeholder="记录本次沟通要点…"
                    :disabled="isTerminal || followupSaving"
                  />
                </el-form-item>
                <el-form-item label="渠道">
                  <el-select
                    v-model="followupForm.channel"
                    clearable
                    placeholder="未指定"
                    style="width: 160px"
                    :disabled="isTerminal || followupSaving"
                  >
                    <el-option
                      v-for="opt in LEAD_FOLLOWUP_CHANNEL_OPTIONS"
                      :key="opt.value || 'none'"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </el-form-item>
                <el-button
                  type="primary"
                  :loading="followupSaving"
                  :disabled="isTerminal"
                  @click="submitFollowup"
                >
                  添加跟进记录
                </el-button>
                <p v-if="isTerminal" class="crm-hint">终态线索不可新增跟进</p>
              </el-form>

              <h4 class="section-title">跟进时间线</h4>
              <el-timeline v-if="timelineItems.length" class="followup-timeline">
                <el-timeline-item
                  v-for="(item, idx) in timelineItems"
                  :key="item.key ?? idx"
                  :timestamp="formatTime(item.createdAt)"
                  placement="top"
                >
                  <div class="timeline-head">
                    <span class="timeline-operator">{{ item.operatorName || '—' }}</span>
                    <el-tag v-if="item.channelLabel" size="small" type="info">{{ item.channelLabel }}</el-tag>
                  </div>
                  <p class="timeline-content">{{ item.content }}</p>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无跟进记录，请添加第一条跟进" :image-size="48" />
            </el-tab-pane>

            <el-tab-pane label="线索信息" name="info">
              <h4 class="section-title">联系人</h4>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="姓名">{{ detail.name?.trim() || '—' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">
                  <span v-if="detail.email">
                    <el-link :href="`mailto:${detail.email}`" type="primary">{{ detail.email }}</el-link>
                    <el-button link type="primary" class="copy-btn" @click="copyText(detail.email, '邮箱')">复制</el-button>
                  </span>
                  <span v-else>—</span>
                </el-descriptions-item>
                <el-descriptions-item label="电话">
                  <span v-if="detail.phone">
                    {{ detail.phone }}
                    <el-button link type="primary" class="copy-btn" @click="copyText(detail.phone!, '电话')">复制</el-button>
                  </span>
                  <span v-else>—</span>
                </el-descriptions-item>
              </el-descriptions>

              <h4 class="section-title">行程需求</h4>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="出行日期">{{ formatDate(detail.travelDate) }}</el-descriptions-item>
                <el-descriptions-item label="人数">
                  {{ detail.partySize != null ? `${detail.partySize} 人` : '—' }}
                </el-descriptions-item>
                <el-descriptions-item label="预算">{{ detail.budget?.trim() || '—' }}</el-descriptions-item>
                <el-descriptions-item label="留言">
                  <el-input
                    v-if="detail.message?.trim()"
                    :model-value="detail.message"
                    type="textarea"
                    :rows="4"
                    readonly
                  />
                  <span v-else>—</span>
                </el-descriptions-item>
              </el-descriptions>

              <h4 class="section-title">来源归因</h4>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="来源渠道">
                  <el-tag size="small" :type="sourceMeta(detail.source).type">
                    {{ sourceMeta(detail.source).label }}
                  </el-tag>
                  <span v-if="detail.source" class="source-raw">{{ detail.source }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="落地页">
                  <div v-if="detail.landingPageTitle">
                    <div>{{ detail.landingPageTitle }}</div>
                    <div v-if="detail.landingPageSlug" class="landing-slug">/{{ detail.landingPageSlug }}</div>
                  </div>
                  <span v-else>—</span>
                </el-descriptions-item>
                <el-descriptions-item label="关键词">
                  <span v-if="detail.keywordText">
                    {{ detail.keywordText }}
                    <el-tag v-if="detail.keywordMarket" size="small" type="info" class="ml-1">{{ detail.keywordMarket }}</el-tag>
                  </span>
                  <span v-else>—</span>
                </el-descriptions-item>
              </el-descriptions>

              <h4 class="section-title">UTM</h4>
              <template v-if="hasUtm">
                <el-descriptions :column="2" border size="small" class="utm-grid">
                  <el-descriptions-item v-for="item in utmItems" :key="item.key" :label="item.label">
                    {{ item.value }}
                  </el-descriptions-item>
                </el-descriptions>
              </template>
              <el-empty v-else description="无 UTM 参数" :image-size="48" />

              <h4 class="section-title">设备</h4>
              <el-tooltip v-if="detail.device" :content="detail.device" placement="top" :show-after="400">
                <p class="device-text">{{ detail.device }}</p>
              </el-tooltip>
              <span v-else>—</span>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-tooltip content="FR-603 M3" placement="top">
          <el-button disabled>AI 跟进建议</el-button>
        </el-tooltip>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="LeadsList" lang="ts">
import { createFollowup, getLead, listLeads, patchLead } from '@/api/tourgeo/lead';
import type { LeadDetailVo, LeadStatus, LeadVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  getAllowedNextStatuses,
  isTerminalLeadStatus,
  LEAD_FOLLOWUP_CHANNEL_OPTIONS,
  LEAD_SOURCE_OPTIONS,
  LEAD_STATUS_OPTIONS,
  leadFollowupChannelLabel,
  leadSourceMeta,
  leadStatusMeta
} from '@/constants/lead';
import { useProjectStore } from '@/store/modules/project';
import { useUserStore } from '@/store/modules/user';
import { maskEmail, maskPhone } from '@/utils/maskPii';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();
const userStore = useUserStore();

const loading = ref(false);
const leadList = ref<LeadVo[]>([]);
const total = ref(0);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<LeadDetailVo | null>(null);
const activeLeadId = ref<number | null>(null);
const activeTab = ref<'crm' | 'info'>('crm');
const editStatus = ref<LeadStatus>('NEW');
const statusSaving = ref(false);
const assigneeSaving = ref(false);
const followupSaving = ref(false);
const followupForm = reactive({
  content: '',
  channel: ''
});

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  email: '',
  phone: '',
  source: '',
  status: '' as '' | LeadVo['status'],
  landingPage: '',
  keyword: '',
  landingPageId: '' as string
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

const clientFilterActive = computed(() => Boolean(queryParams.landingPage?.trim() || queryParams.keyword?.trim() || queryParams.landingPageId));

const hasActiveFilters = computed(
  () =>
    Boolean(
      queryParams.name ||
        queryParams.email ||
        queryParams.phone ||
        queryParams.source ||
        queryParams.status ||
        queryParams.landingPage ||
        queryParams.keyword ||
        queryParams.landingPageId
    )
);

const emptyDescription = computed(() => (hasActiveFilters.value ? '未找到匹配线索' : '暂无询盘'));

const displayList = computed(() => {
  let rows = leadList.value;
  const lp = queryParams.landingPage?.trim().toLowerCase();
  const kw = queryParams.keyword?.trim().toLowerCase();
  const lpId = queryParams.landingPageId?.trim();
  if (lpId) {
    const id = Number(lpId);
    if (!Number.isNaN(id)) rows = rows.filter((r) => r.landingPageId === id);
  }
  if (lp) {
    rows = rows.filter(
      (r) =>
        (r.landingPageTitle ?? '').toLowerCase().includes(lp) ||
        (r.landingPageSlug ?? '').toLowerCase().includes(lp)
    );
  }
  if (kw) rows = rows.filter((r) => (r.keywordText ?? '').toLowerCase().includes(kw));
  return rows;
});

const detailTitle = computed(() => (detail.value ? `线索详情 · #${detail.value.id}` : '线索详情'));

const UTM_FIELDS = [
  { key: 'utm_source', label: 'Source' },
  { key: 'utm_medium', label: 'Medium' },
  { key: 'utm_campaign', label: 'Campaign' },
  { key: 'utm_content', label: 'Content' },
  { key: 'utm_term', label: 'Term' }
] as const;

const utmItems = computed(() => {
  const utm = detail.value?.utm ?? {};
  return UTM_FIELDS.map(({ key, label }) => ({
    key,
    label,
    value: utm[key] || '—'
  })).filter((item) => item.value !== '—');
});

const hasUtm = computed(() => utmItems.value.length > 0);

const isTerminal = computed(() => (detail.value ? isTerminalLeadStatus(detail.value.status) : false));

const statusOptions = computed(() =>
  detail.value ? getAllowedNextStatuses(detail.value.status) : (['NEW'] as LeadStatus[])
);

const statusDirty = computed(() => detail.value != null && editStatus.value !== detail.value.status);

type TimelineItem = {
  key: string;
  content: string;
  createdAt?: string;
  operatorName?: string;
  channelLabel?: string;
};

const timelineItems = computed((): TimelineItem[] => {
  if (!detail.value) return [];
  const items: TimelineItem[] = [...(detail.value.followups ?? [])]
    .reverse()
    .map((row) => ({
      key: `fu-${row.id}`,
      content: row.content,
      createdAt: row.createdAt,
      operatorName: row.operatorName || '—',
      channelLabel: leadFollowupChannelLabel(row.channel)
    }));
  if (detail.value.createdAt) {
    items.push({
      key: 'system-created',
      content: '线索由落地页表单创建。',
      createdAt: detail.value.createdAt,
      operatorName: '系统'
    });
  }
  return items;
});

function statusMeta(status: string) {
  return leadStatusMeta(status);
}

function sourceMeta(source?: string) {
  return leadSourceMeta(source ?? 'form');
}

function formatTime(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatDate(value?: string): string {
  if (!value) return '—';
  return value.slice(0, 10);
}

async function copyText(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(`${label}已复制`);
  } catch {
    ElMessage.error('复制失败');
  }
}

function syncProjectFromRoute() {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) {
    projectStore.setCurrentProject(fromRoute);
  }
}

function initFiltersFromQuery() {
  const status = route.query.status;
  const landingPageId = route.query.landingPageId;
  if (typeof status === 'string' && status) {
    queryParams.status = status as LeadVo['status'];
  }
  if (typeof landingPageId === 'string' && landingPageId) {
    queryParams.landingPageId = landingPageId;
  }
}

async function getList() {
  const pid = projectId.value;
  if (!pid) {
    leadList.value = [];
    total.value = 0;
    return;
  }
  loading.value = true;
  try {
    const res = await listLeads(pid, {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      name: queryParams.name || undefined,
      email: queryParams.email || undefined,
      phone: queryParams.phone || undefined,
      source: queryParams.source || undefined,
      status: queryParams.status || undefined
    });
    leadList.value = res.rows;
    total.value = res.total;
  } catch {
    leadList.value = [];
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
  queryParams.name = '';
  queryParams.email = '';
  queryParams.phone = '';
  queryParams.source = '';
  queryParams.status = '';
  queryParams.landingPage = '';
  queryParams.keyword = '';
  queryParams.landingPageId = '';
  queryParams.pageNum = 1;
  router.replace({ query: { ...route.query, status: undefined, landingPageId: undefined } });
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectLeads' || route.name === 'LeadsList') {
    router.replace({ name: 'ProjectLeads', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function goLandingPages() {
  const pid = projectId.value;
  if (pid) {
    router.push({ name: 'ProjectLandingPages', params: { projectId: pid } });
  } else {
    router.push({ name: 'LandingPagesList' });
  }
}

async function openDetail(row: LeadVo) {
  const pid = projectId.value;
  if (!pid || !row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = null;
  activeLeadId.value = row.id;
  activeTab.value = 'crm';
  try {
    detail.value = await getLead(pid, row.id);
    syncCrmFormFromDetail();
  } catch {
    ElMessage.error('线索不存在或无权访问');
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
}

function syncCrmFormFromDetail() {
  if (!detail.value) return;
  editStatus.value = detail.value.status;
  followupForm.content = '';
  followupForm.channel = '';
}

async function refreshDetail() {
  const pid = projectId.value;
  const leadId = activeLeadId.value;
  if (!pid || !leadId) return;
  detail.value = await getLead(pid, leadId);
  syncCrmFormFromDetail();
}

async function saveStatus() {
  const pid = projectId.value;
  const leadId = activeLeadId.value;
  if (!pid || !leadId || !detail.value || !statusDirty.value) return;
  statusSaving.value = true;
  try {
    detail.value = await patchLead(pid, leadId, { status: editStatus.value });
    syncCrmFormFromDetail();
    ElMessage.success('状态已更新');
    await getList();
  } catch {
    if (detail.value) {
      editStatus.value = detail.value.status;
    }
  } finally {
    statusSaving.value = false;
  }
}

async function assignToMe() {
  const pid = projectId.value;
  const leadId = activeLeadId.value;
  const uid = Number(userStore.userId);
  if (!pid || !leadId) return;
  if (!uid) {
    ElMessage.warning('无法获取当前用户');
    return;
  }
  assigneeSaving.value = true;
  try {
    detail.value = await patchLead(pid, leadId, { assigneeId: uid });
    syncCrmFormFromDetail();
    ElMessage.success('负责人已更新');
    await getList();
  } finally {
    assigneeSaving.value = false;
  }
}

async function submitFollowup() {
  const pid = projectId.value;
  const leadId = activeLeadId.value;
  if (!pid || !leadId || isTerminal.value) return;
  const content = followupForm.content.trim();
  if (content.length < 2) {
    ElMessage.warning('跟进内容至少 2 字');
    return;
  }
  followupSaving.value = true;
  try {
    await createFollowup(pid, leadId, {
      content,
      channel: followupForm.channel || undefined
    });
    followupForm.content = '';
    followupForm.channel = '';
    await refreshDetail();
    ElMessage.success('跟进记录已添加');
  } finally {
    followupSaving.value = false;
  }
}

function resetDetail() {
  detail.value = null;
  activeLeadId.value = null;
  activeTab.value = 'crm';
  editStatus.value = 'NEW';
  followupForm.content = '';
  followupForm.channel = '';
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
.tg-leads {
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

  .name-link {
    padding: 0;
    font-weight: 500;
  }

  .landing-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .landing-title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .landing-slug {
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .compliance-alert {
    margin-top: var(--tg-space-3, 12px);
  }

  .detail-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .detail-tabs {
    :deep(.el-tabs__header) {
      margin-bottom: 12px;
    }
  }

  .crm-section {
    margin-bottom: 8px;
  }

  .crm-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .crm-label {
    width: 56px;
    flex-shrink: 0;
    font-size: 13px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .assignee-name {
    min-width: 80px;
    font-weight: 500;
  }

  .unsaved-hint,
  .crm-hint {
    font-size: 12px;
    color: var(--tg-color-text-secondary, #9ca3af);
  }

  .crm-hint {
    margin: 0 0 8px 64px;
  }

  .followup-form {
    margin-bottom: 8px;
  }

  .followup-timeline {
    padding-left: 4px;
  }

  .timeline-head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }

  .timeline-operator {
    font-weight: 600;
    font-size: 13px;
  }

  .timeline-content {
    margin: 0;
    font-size: 14px;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .detail-time {
    font-size: 14px;
    color: var(--tg-color-text-regular, #4b5563);
  }

  .detail-id {
    font-size: 12px;
    color: var(--tg-color-text-secondary, #9ca3af);
  }

  .section-title {
    margin: 16px 0 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--tg-color-text-primary, #1f2937);
  }

  .copy-btn {
    margin-left: 8px;
  }

  .source-raw {
    margin-left: 6px;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .device-text {
    margin: 0;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-all;
  }

  @media (max-width: 1199px) {
    :deep(.hidden-md-only) {
      display: none;
    }
  }

  @media (max-width: 767px) {
    :deep(.hidden-sm-only) {
      display: none;
    }

    :deep(.lead-detail-drawer) {
      width: 100% !important;
    }
  }
}
</style>
