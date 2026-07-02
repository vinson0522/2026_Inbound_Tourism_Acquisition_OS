<template>
  <div class="p-2 tg-materials">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">爆款拆解</h1>
        <p class="tg-page-sub">内容 Agent · 参考素材上传与七维结构拆解（FR-401~403）</p>
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
          <el-button type="primary" icon="Upload" @click="openUploadDialog">上传素材</el-button>
          <el-button icon="Refresh" :loading="loading" @click="getList()">刷新</el-button>
          <el-form :inline="true" class="toolbar-form" @submit.prevent="handleQuery">
            <el-form-item label="类型">
              <el-select v-model="queryParams.type" placeholder="全部" clearable style="width: 110px">
                <el-option label="全部" value="" />
                <el-option v-for="t in MATERIAL_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="拆解状态">
              <el-select v-model="queryParams.breakdownStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option
                  v-for="(meta, key) in MATERIAL_BREAKDOWN_STATUS_META"
                  :key="key"
                  :label="meta.label"
                  :value="key"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="版权">
              <el-select v-model="queryParams.copyrightStatus" placeholder="全部" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option
                  v-for="c in MATERIAL_COPYRIGHT_OPTIONS"
                  :key="c.value"
                  :label="c.label"
                  :value="c.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <el-alert
        v-if="hasProcessingRows"
        type="info"
        show-icon
        :closable="false"
        title="有素材正在拆解中，列表每 5 秒自动刷新"
        class="mb-3"
      />

      <el-card shadow="hover">
        <el-table v-loading="loading" border :data="materialList">
          <template #empty>
            <el-empty :description="hasActiveFilters ? '未找到匹配素材' : '暂无素材'">
              <el-button type="primary" @click="openUploadDialog">上传素材</el-button>
              <el-button v-if="hasActiveFilters" link type="primary" @click="resetQuery">清除筛选</el-button>
            </el-empty>
          </template>

          <el-table-column label="缩略图" width="88" align="center">
            <template #default="{ row }">
              <el-image
                v-if="rowThumbnail(row)"
                :src="rowThumbnail(row)"
                fit="cover"
                class="material-thumb"
                lazy
                :preview-src-list="[rowThumbnail(row)!]"
                preview-teleported
              />
              <div v-else class="material-thumb material-thumb--empty">
                <el-icon><VideoCamera /></el-icon>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="素材" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.fileName || fileNameFromUrl(row.url) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="materialTypeMeta(row.type).type">
                {{ materialTypeMeta(row.type).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="版权" width="100" align="center" class-name="hidden-md-only">
            <template #default="{ row }">
              <el-tag size="small" :type="materialCopyrightMeta(row.copyrightStatus).type">
                {{ materialCopyrightMeta(row.copyrightStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="拆解状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="breakdownStatusMeta(row.breakdownStatus).type">
                {{ breakdownStatusMeta(row.breakdownStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="帧数" width="70" align="center" class-name="hidden-md-only">
            <template #default="{ row }">
              {{ row.frameCount != null && row.frameCount > 0 ? row.frameCount : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160" class-name="hidden-sm-only">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="canStartBreakdown(row)"
                link
                type="primary"
                :loading="breakingId === row.id"
                @click="handleStartBreakdown(row)"
              >
                开始拆解
              </el-button>
              <el-tooltip v-else-if="row.type !== 'VIDEO'" content="M1 优先支持视频拆解" placement="top">
                <el-button link type="primary" disabled>开始拆解</el-button>
              </el-tooltip>
              <el-tooltip v-else-if="row.breakdownStatus === 'PROCESSING'" content="拆解进行中" placement="top">
                <el-button link type="primary" disabled>继续等待</el-button>
              </el-tooltip>
              <el-button
                v-if="row.breakdownStatus === 'SUCCESS' && row.breakdownId"
                link
                type="primary"
                @click="openDetail(row)"
              >
                查看拆解
              </el-button>
              <el-button v-if="row.url" link type="primary" @click="previewUrl(row.url)">预览</el-button>
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
      </el-card>

      <el-alert
        type="warning"
        show-icon
        :closable="false"
        :title="COPYRIGHT_FOOTNOTE"
        class="mt-3 copyright-alert"
      />
    </template>

    <!-- 上传 dialog -->
    <el-dialog v-model="uploadVisible" title="上传参考素材" width="520px" destroy-on-close @closed="resetUploadForm">
      <el-form ref="uploadFormRef" :model="uploadForm" label-width="90px">
        <el-form-item label="文件" required>
          <el-upload
            drag
            :limit="1"
            :auto-upload="false"
            :accept="MATERIAL_UPLOAD_ACCEPT"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :file-list="uploadFileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">支持 MP4 / MOV / WebM · JPG / PNG / WebP · 单文件 ≤ {{ MATERIAL_MAX_FILE_MB }}MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="素材类型">
          <el-select v-model="uploadForm.type" style="width: 100%">
            <el-option v-for="t in MATERIAL_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="版权状态">
          <el-select v-model="uploadForm.copyrightStatus" style="width: 100%">
            <el-option
              v-for="c in MATERIAL_COPYRIGHT_OPTIONS"
              :key="c.value"
              :label="c.label"
              :value="c.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源说明">
          <el-input v-model="uploadForm.source" maxlength="200" placeholder="TikTok @creator / 竞品广告…" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          上传后保存至项目素材库，可发起七维拆解。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadSubmitting" @click="submitUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 拆解详情 drawer -->
    <el-drawer
      v-model="detailVisible"
      :title="detailTitle"
      size="720px"
      destroy-on-close
      @closed="resetDetail"
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailMaterial">
          <div class="detail-preview">
            <video
              v-if="detailMaterial.type === 'VIDEO' && detailMaterial.url"
              :src="detailMaterial.url"
              controls
              class="detail-video"
            />
            <el-image
              v-else-if="detailMaterial.url"
              :src="detailMaterial.url"
              fit="contain"
              class="detail-image"
            />
            <div class="detail-preview__meta">
              <el-tag size="small" :type="materialTypeMeta(detailMaterial.type).type">
                {{ materialTypeMeta(detailMaterial.type).label }}
              </el-tag>
              <el-tag v-if="detailBreakdown?.breakdownStatus" size="small" :type="breakdownStatusMeta(detailBreakdown.breakdownStatus).type">
                {{ breakdownStatusMeta(detailBreakdown.breakdownStatus).label }}
              </el-tag>
              <el-tag v-if="detailBreakdown?.frames?.length" size="small" type="info">
                {{ detailBreakdown.frames.length }} 帧
              </el-tag>
              <el-tooltip v-if="detailBreakdown?.needsHumanReview" :content="NEEDS_REVIEW_TOOLTIP">
                <el-tag type="warning" size="small">待人工确认</el-tag>
              </el-tooltip>
            </div>
          </div>

          <el-result
            v-if="detailMaterial.breakdownStatus === 'FAILED'"
            icon="error"
            title="拆解失败"
            sub-title="请返回列表重新发起拆解"
          />

          <template v-else-if="detailMaterial.breakdownStatus === 'PROCESSING' && !detailBreakdown?.dimensions">
            <el-skeleton :rows="6" animated class="mt-4" />
            <p class="text-muted mt-2">AI 正在拆帧与分析，请稍候…</p>
          </template>

          <template v-else-if="detailBreakdown">
            <section class="detail-section">
              <h4>七维拆解</h4>
              <el-descriptions :column="1" border>
                <el-descriptions-item
                  v-for="dim in BREAKDOWN_DIMENSIONS"
                  :key="dim.key"
                  :label="dim.label"
                >
                  {{ dimensionText(dim.key) || '—' }}
                </el-descriptions-item>
              </el-descriptions>
            </section>

            <section v-if="detailBreakdown.reusableStructure" class="detail-section">
              <h4>可复用结构摘要</h4>
              <el-alert type="info" :closable="false" show-icon>
                <p class="reusable-text">{{ detailBreakdown.reusableStructure }}</p>
              </el-alert>
            </section>

            <section v-if="detailBreakdown.frames?.length" class="detail-section">
              <h4>拆帧网格</h4>
              <el-row :gutter="8">
                <el-col
                  v-for="(frame, idx) in detailBreakdown.frames"
                  :key="idx"
                  :xs="12"
                  :sm="8"
                  :md="6"
                >
                  <div class="frame-card">
                    <el-image
                      v-if="frame.thumbnailUrl"
                      :src="frame.thumbnailUrl"
                      fit="cover"
                      class="frame-thumb"
                      :preview-src-list="framePreviewList"
                      :initial-index="idx"
                      preview-teleported
                    />
                    <div v-else class="frame-thumb frame-thumb--empty">{{ frame.timestampLabel || idx }}</div>
                    <div class="frame-time">{{ frame.timestampLabel || formatFrameTime(frame.timestamp) }}</div>
                    <el-tooltip v-if="frame.caption" :content="frame.caption" placement="top">
                      <p class="frame-caption">{{ frame.caption }}</p>
                    </el-tooltip>
                  </div>
                </el-col>
              </el-row>
            </section>
          </template>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button disabled title="FR-406 M2">转为内容参考</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="MaterialList" lang="ts">
import {
  getMaterialBreakdown,
  listMaterials,
  triggerMaterialBreakdown,
  uploadMaterial
} from '@/api/tourgeo/material';
import type { MaterialAssetVo, VideoBreakdownVo } from '@/api/tourgeo/types';
import ProjectSelector from '@/components/tourgeo/ProjectSelector.vue';
import {
  BREAKDOWN_DIMENSIONS,
  COPYRIGHT_FOOTNOTE,
  MATERIAL_BREAKDOWN_STATUS_META,
  MATERIAL_COPYRIGHT_OPTIONS,
  MATERIAL_MAX_FILE_MB,
  MATERIAL_TYPES,
  MATERIAL_UPLOAD_ACCEPT,
  NEEDS_REVIEW_TOOLTIP,
  breakdownStatusMeta,
  inferMaterialType,
  materialCopyrightMeta,
  materialTypeMeta
} from '@/constants/material';
import { useProjectStore } from '@/store/modules/project';
import { UploadFilled, VideoCamera } from '@element-plus/icons-vue';
import type { FormInstance, UploadFile, UploadUserFile } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const materialList = ref<MaterialAssetVo[]>([]);
const total = ref(0);
const breakingId = ref<number | null>(null);

const uploadVisible = ref(false);
const uploadSubmitting = ref(false);
const uploadFormRef = ref<FormInstance>();
const uploadFileList = ref<UploadUserFile[]>([]);
const uploadForm = reactive({
  type: 'VIDEO',
  copyrightStatus: 'external',
  source: ''
});

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailMaterial = ref<MaterialAssetVo | null>(null);
const detailBreakdown = ref<VideoBreakdownVo | null>(null);

let pollTimer: ReturnType<typeof setInterval> | null = null;

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  type: '' as '' | 'VIDEO' | 'IMAGE',
  breakdownStatus: '' as '' | keyof typeof MATERIAL_BREAKDOWN_STATUS_META,
  copyrightStatus: ''
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

const hasActiveFilters = computed(
  () => Boolean(queryParams.type || queryParams.breakdownStatus || queryParams.copyrightStatus)
);

const hasProcessingRows = computed(() =>
  materialList.value.some((row) => row.breakdownStatus === 'PROCESSING')
);

const detailTitle = computed(() => {
  if (!detailMaterial.value) return '拆解详情';
  return `拆解详情 · ${detailMaterial.value.fileName || fileNameFromUrl(detailMaterial.value.url)}`;
});

const framePreviewList = computed(() =>
  (detailBreakdown.value?.frames ?? [])
    .map((f) => f.thumbnailUrl)
    .filter((url): url is string => Boolean(url))
);

function formatTime(iso?: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatFrameTime(seconds?: number): string {
  if (seconds == null) return '—';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function fileNameFromUrl(url?: string): string {
  if (!url) return '—';
  const path = url.split('?')[0];
  const slash = path.lastIndexOf('/');
  return slash >= 0 ? path.slice(slash + 1) : path;
}

function rowThumbnail(row: MaterialAssetVo): string | undefined {
  return row.thumbnailUrl || undefined;
}

function canStartBreakdown(row: MaterialAssetVo): boolean {
  if (row.type !== 'VIDEO') return false;
  return row.breakdownStatus === 'NONE' || row.breakdownStatus === 'FAILED' || !row.breakdownStatus;
}

function dimensionText(key: string): string {
  const val = detailBreakdown.value?.dimensions?.[key];
  return val != null ? String(val) : '';
}

function syncProjectFromRoute() {
  const fromRoute = Number(route.params.projectId);
  if (!Number.isNaN(fromRoute) && fromRoute > 0) {
    projectStore.setCurrentProject(fromRoute);
  }
}

function syncPolling() {
  if (hasProcessingRows.value && !pollTimer) {
    pollTimer = setInterval(() => {
      getList(true);
    }, 5000);
  } else if (!hasProcessingRows.value && pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

async function getList(silent = false) {
  const pid = projectId.value;
  if (!pid) {
    materialList.value = [];
    total.value = 0;
    syncPolling();
    return;
  }
  if (!silent) loading.value = true;
  try {
    const res = await listMaterials(pid, {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      type: queryParams.type,
      copyrightStatus: queryParams.copyrightStatus,
      breakdownStatus: queryParams.breakdownStatus
    });
    materialList.value = res.rows;
    total.value = res.total;
  } catch {
    if (!silent) {
      materialList.value = [];
      total.value = 0;
    }
  } finally {
    if (!silent) loading.value = false;
    syncPolling();
    if (detailVisible.value && detailMaterial.value) {
      const updated = materialList.value.find((r) => r.id === detailMaterial.value!.id);
      if (updated && updated.breakdownStatus === 'SUCCESS' && updated.breakdownId) {
        detailMaterial.value = updated;
        loadBreakdownDetail(updated);
      }
    }
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.type = '';
  queryParams.breakdownStatus = '';
  queryParams.copyrightStatus = '';
  queryParams.pageNum = 1;
  getList();
}

function handleProjectChange(id: number | null) {
  if (id == null) return;
  if (route.name === 'ProjectMaterials') {
    router.replace({ name: 'ProjectMaterials', params: { projectId: id }, query: route.query });
  }
  queryParams.pageNum = 1;
  getList();
}

function openUploadDialog() {
  resetUploadForm();
  uploadVisible.value = true;
}

function resetUploadForm() {
  uploadFileList.value = [];
  uploadForm.type = 'VIDEO';
  uploadForm.copyrightStatus = 'external';
  uploadForm.source = '';
}

function handleFileChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) return;
  const err = validateUploadFile(uploadFile.raw);
  if (err) {
    ElMessage.warning(err);
    uploadFileList.value = [];
    return;
  }
  uploadFileList.value = [uploadFile as UploadUserFile];
  uploadForm.type = inferMaterialType(uploadFile.name, uploadFile.raw.type);
}

function handleFileRemove() {
  uploadFileList.value = [];
}

function validateUploadFile(file: File): string | null {
  const ext = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.')).toLowerCase() : '';
  const allowed = MATERIAL_UPLOAD_ACCEPT.split(',');
  if (!allowed.includes(ext)) {
    return `不支持的文件格式：${ext || file.name}`;
  }
  if (file.size > MATERIAL_MAX_FILE_MB * 1024 * 1024) {
    return `文件不能超过 ${MATERIAL_MAX_FILE_MB}MB`;
  }
  return null;
}

async function submitUpload() {
  const pid = projectId.value;
  const file = uploadFileList.value[0]?.raw;
  if (!pid || !file) {
    ElMessage.warning('请选择要上传的文件');
    return;
  }
  const err = validateUploadFile(file);
  if (err) {
    ElMessage.warning(err);
    return;
  }
  uploadSubmitting.value = true;
  try {
    await uploadMaterial(pid, {
      file,
      type: uploadForm.type,
      copyrightStatus: uploadForm.copyrightStatus,
      source: uploadForm.source.trim() || undefined
    });
    ElMessage.success('素材已上传');
    uploadVisible.value = false;
    await getList();
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') ElMessage.error(msg);
  } finally {
    uploadSubmitting.value = false;
  }
}

async function handleStartBreakdown(row: MaterialAssetVo) {
  const pid = projectId.value;
  if (!pid) return;
  try {
    await ElMessageBox.confirm(
      `对「${row.fileName || fileNameFromUrl(row.url)}」发起七维拆解？`,
      '开始拆解',
      { confirmButtonText: '开始', cancelButtonText: '取消', type: 'info' }
    );
  } catch {
    return;
  }
  breakingId.value = row.id;
  try {
    const result = await triggerMaterialBreakdown(pid, row.id);
    ElMessage.success('拆解任务已提交');
    await getList();
    const updated = materialList.value.find((r) => r.id === row.id);
    if (updated) {
      updated.breakdownId = result.breakdownId;
      updated.breakdownStatus = 'PROCESSING';
      openDetail(updated);
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') ElMessage.error(msg);
    await getList();
  } finally {
    breakingId.value = null;
  }
}

async function loadBreakdownDetail(row: MaterialAssetVo) {
  const pid = projectId.value;
  if (!pid || !row.breakdownId) {
    detailBreakdown.value = null;
    return;
  }
  detailLoading.value = true;
  try {
    detailBreakdown.value = await getMaterialBreakdown(pid, row.breakdownId);
  } catch {
    detailBreakdown.value = null;
    ElMessage.error('加载拆解详情失败');
  } finally {
    detailLoading.value = false;
  }
}

async function openDetail(row: MaterialAssetVo) {
  detailMaterial.value = row;
  detailBreakdown.value = null;
  detailVisible.value = true;
  if (row.breakdownId && row.breakdownStatus !== 'NONE') {
    await loadBreakdownDetail(row);
  }
}

function resetDetail() {
  detailMaterial.value = null;
  detailBreakdown.value = null;
}

function previewUrl(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
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
  await getList();
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<style scoped lang="scss">
.tg-materials {
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

  .material-thumb {
    width: 64px;
    height: 64px;
    border-radius: var(--tg-radius-sm, 4px);

    &--empty {
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--tg-color-bg-muted, #f3f4f6);
      color: var(--tg-color-text-placeholder, #9ca3af);
    }
  }

  .copyright-alert {
    :deep(.el-alert__title) {
      line-height: 1.5;
      font-size: var(--tg-font-size-sm, 13px);
    }
  }

  .text-muted {
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: var(--tg-font-size-sm, 13px);
  }

  .detail-body {
    min-height: 240px;
  }

  .detail-preview {
    margin-bottom: var(--tg-space-4, 16px);

    &__meta {
      display: flex;
      flex-wrap: wrap;
      gap: var(--tg-space-2, 8px);
      margin-top: var(--tg-space-2, 8px);
    }
  }

  .detail-video {
    width: 100%;
    max-height: 280px;
    border-radius: var(--tg-radius-sm, 4px);
    background: #000;
  }

  .detail-image {
    width: 100%;
    max-height: 280px;
    border-radius: var(--tg-radius-sm, 4px);
  }

  .detail-section {
    margin-bottom: var(--tg-space-4, 16px);

    h4 {
      margin: 0 0 var(--tg-space-2, 8px);
      font-size: var(--tg-font-size-sm, 13px);
      color: var(--tg-color-text-secondary, #6b7280);
    }
  }

  .reusable-text {
    margin: 0;
    white-space: pre-wrap;
    line-height: 1.6;
  }

  .frame-card {
    margin-bottom: var(--tg-space-2, 8px);
  }

  .frame-thumb {
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: var(--tg-radius-sm, 4px);

    &--empty {
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--tg-color-bg-muted, #f3f4f6);
      font-size: var(--tg-font-size-xs, 12px);
      color: var(--tg-color-text-secondary, #6b7280);
    }
  }

  .frame-time {
    margin-top: 4px;
    font-size: var(--tg-font-size-xs, 12px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .frame-caption {
    margin: 2px 0 0;
    font-size: var(--tg-font-size-xs, 12px);
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

@media (max-width: 768px) {
  .tg-materials :deep(.hidden-md-only) {
    display: none;
  }
}

@media (max-width: 992px) {
  .tg-materials :deep(.hidden-sm-only) {
    display: none;
  }
}
</style>
