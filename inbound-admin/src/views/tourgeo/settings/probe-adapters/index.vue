<template>
  <div class="p-2 tg-probe-adapters">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">平台 Adapter</h1>
        <p class="tg-page-sub">系统设置 · 浏览器扩展解析配置（FR-116）</p>
      </div>
    </div>

    <el-card shadow="hover" class="mb-3">
      <template #header>
        <div class="summary-header">
          <span class="card-title">平台 Adapter</span>
          <div class="toolbar-actions">
            <router-link to="/settings/probe-nodes">
              <el-button link type="primary">管理探针节点 →</el-button>
            </router-link>
            <el-button icon="Refresh" :loading="loading" @click="loadAdapters">刷新</el-button>
          </div>
        </div>
      </template>

      <p v-if="!loading || adapters.length" class="summary-text">
        已配置 <strong>{{ summary.total }}</strong> · 启用
        <strong class="enabled-count">{{ summary.enabled }}</strong> · 停用
        <strong>{{ summary.disabled }}</strong>
      </p>
      <p class="summary-hint">
        <el-icon><InfoFilled /></el-icon>
        扩展每 30s poll 拉取最新 enabled adapter；grounded-api 不读取本配置。
      </p>
    </el-card>

    <el-card v-loading="loading" shadow="hover" class="mb-3">
      <el-empty v-if="!loading && adapters.length === 0" description="暂无平台 Adapter">
        <p class="empty-hint">请执行 <code>002_seed_demo.sql</code> 初始化 perplexity / chatgpt adapter。</p>
      </el-empty>

      <el-table v-else :data="adapters" border stripe>
        <el-table-column label="平台" min-width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ probePlatformLabel(row.platform) }}</el-tag>
            <div class="platform-slug">{{ row.platform }}</div>
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="version" width="90">
          <template #default="{ row }">
            <span class="mono">{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="info" @click="copyAdapterJson(row)">复制 JSON</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="修改解析规则可能影响 citations/品牌识别准确性。保存前请在测试环境验证；错误配置将导致 browser-extension 子任务 FAILED。"
    />

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="640px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" label-width="88px" @submit.prevent>
        <el-form-item label="平台">
          <span class="mono">{{ editingPlatform }}</span>
        </el-form-item>
        <el-form-item label="版本">
          <span class="mono">{{ editForm.version }}</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>

        <el-divider content-position="left">DOM 选择器</el-divider>
        <el-form-item label="dom_selectors" :error="jsonErrors.domSelectorsJson">
          <div class="json-field">
            <el-input
              v-model="jsonText.domSelectorsJson"
              type="textarea"
              :rows="6"
              class="json-textarea"
              spellcheck="false"
            />
            <div class="json-actions">
              <el-button size="small" @click="formatJsonField('domSelectorsJson')">格式化</el-button>
              <el-button size="small" @click="validateJsonField('domSelectorsJson')">校验</el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider content-position="left">接口特征</el-divider>
        <el-form-item label="api_patterns" :error="jsonErrors.apiPatternsJson">
          <div class="json-field">
            <el-input
              v-model="jsonText.apiPatternsJson"
              type="textarea"
              :rows="5"
              class="json-textarea"
              spellcheck="false"
            />
            <div class="json-actions">
              <el-button size="small" @click="formatJsonField('apiPatternsJson')">格式化</el-button>
              <el-button size="small" @click="validateJsonField('apiPatternsJson')">校验</el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider content-position="left">解析规则</el-divider>
        <el-form-item label="parse_rules" :error="jsonErrors.parseRulesJson">
          <div class="json-field">
            <el-input
              v-model="jsonText.parseRulesJson"
              type="textarea"
              :rows="8"
              class="json-textarea"
              spellcheck="false"
            />
            <div class="json-actions">
              <el-button size="small" @click="formatJsonField('parseRulesJson')">格式化</el-button>
              <el-button size="small" @click="validateJsonField('parseRulesJson')">校验</el-button>
            </div>
          </div>
        </el-form-item>

        <p class="drawer-note">保存后扩展节点将在下次 poll（≤30s）获取新版本。</p>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="ProbeAdaptersSettings">
import { computed, onMounted, reactive, ref } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';
import type { FormInstance } from 'element-plus';
import { getPlatformAdapter, listPlatformAdapters, savePlatformAdapter } from '@/api/tourgeo/probe';
import type { PlatformAdapterSaveForm, PlatformAdapterVo } from '@/api/tourgeo/types';
import { probePlatformLabel } from '@/constants/probe';

type JsonField = 'domSelectorsJson' | 'apiPatternsJson' | 'parseRulesJson';

const loading = ref(false);
const saving = ref(false);
const adapters = ref<PlatformAdapterVo[]>([]);
const drawerVisible = ref(false);
const editingPlatform = ref('');
const formRef = ref<FormInstance>();

const editForm = reactive({
  version: '1.0',
  enabled: true
});

const jsonText = reactive<Record<JsonField, string>>({
  domSelectorsJson: '{}',
  apiPatternsJson: '{}',
  parseRulesJson: '{}'
});

const jsonErrors = reactive<Record<JsonField, string>>({
  domSelectorsJson: '',
  apiPatternsJson: '',
  parseRulesJson: ''
});

const summary = computed(() => {
  const total = adapters.value.length;
  const enabled = adapters.value.filter((a) => a.enabled).length;
  return { total, enabled, disabled: total - enabled };
});

const drawerTitle = computed(() =>
  editingPlatform.value
    ? `编辑平台 Adapter · ${probePlatformLabel(editingPlatform.value)}`
    : '编辑平台 Adapter'
);

function formatTime(value?: string): string {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('zh-CN', { hour12: false });
}

function parseJsonObject(text: string, field: JsonField): Record<string, unknown> | null {
  try {
    const parsed = JSON.parse(text) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      jsonErrors[field] = 'JSON 格式无效：须为 object';
      return null;
    }
    jsonErrors[field] = '';
    return parsed as Record<string, unknown>;
  } catch {
    jsonErrors[field] = 'JSON 格式无效';
    return null;
  }
}

function formatJsonField(field: JsonField) {
  const parsed = parseJsonObject(jsonText[field], field);
  if (parsed) {
    jsonText[field] = JSON.stringify(parsed, null, 2);
  }
}

function validateJsonField(field: JsonField): boolean {
  return parseJsonObject(jsonText[field], field) != null;
}

function validateAllJson(): PlatformAdapterSaveForm | null {
  const domSelectorsJson = parseJsonObject(jsonText.domSelectorsJson, 'domSelectorsJson');
  const apiPatternsJson = parseJsonObject(jsonText.apiPatternsJson, 'apiPatternsJson');
  const parseRulesJson = parseJsonObject(jsonText.parseRulesJson, 'parseRulesJson');
  if (!domSelectorsJson || !apiPatternsJson || !parseRulesJson) {
    return null;
  }
  return {
    version: editForm.version,
    enabled: editForm.enabled,
    domSelectorsJson,
    apiPatternsJson,
    parseRulesJson
  };
}

async function loadAdapters() {
  loading.value = true;
  try {
    adapters.value = await listPlatformAdapters();
  } catch (e: unknown) {
    adapters.value = [];
    const msg = e instanceof Error ? e.message : String(e);
    if (msg !== 'error') {
      ElMessage.error(msg || '加载 Adapter 失败');
    }
  } finally {
    loading.value = false;
  }
}

async function openEdit(row: PlatformAdapterVo) {
  editingPlatform.value = row.platform;
  drawerVisible.value = true;
  try {
    const detail = await getPlatformAdapter(row.platform);
    editForm.version = detail.version;
    editForm.enabled = detail.enabled;
    jsonText.domSelectorsJson = JSON.stringify(detail.domSelectorsJson ?? {}, null, 2);
    jsonText.apiPatternsJson = JSON.stringify(detail.apiPatternsJson ?? {}, null, 2);
    jsonText.parseRulesJson = JSON.stringify(detail.parseRulesJson ?? {}, null, 2);
    jsonErrors.domSelectorsJson = '';
    jsonErrors.apiPatternsJson = '';
    jsonErrors.parseRulesJson = '';
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载 Adapter 详情失败');
    drawerVisible.value = false;
  }
}

async function handleSave() {
  const payload = validateAllJson();
  if (!payload || !editingPlatform.value) return;
  saving.value = true;
  try {
    const saved = await savePlatformAdapter(editingPlatform.value, payload);
    ElMessage.success('Adapter 已保存');
    drawerVisible.value = false;
    const idx = adapters.value.findIndex((a) => a.platform === saved.platform);
    if (idx >= 0) {
      adapters.value[idx] = saved;
    } else {
      await loadAdapters();
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function copyAdapterJson(row: PlatformAdapterVo) {
  try {
    const detail = await getPlatformAdapter(row.platform);
    const bundle = {
      platform: detail.platform,
      version: detail.version,
      enabled: detail.enabled,
      domSelectorsJson: detail.domSelectorsJson,
      apiPatternsJson: detail.apiPatternsJson,
      parseRulesJson: detail.parseRulesJson
    };
    await navigator.clipboard.writeText(JSON.stringify(bundle, null, 2));
    ElMessage.success('JSON 已复制');
  } catch {
    ElMessage.error('复制失败');
  }
}

onMounted(() => {
  loadAdapters();
});
</script>

<style scoped lang="scss">
.tg-probe-adapters {
  .card-title {
    font-weight: 600;
  }

  .summary-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .summary-text {
    margin: 0 0 8px;
    font-size: 15px;

    .enabled-count {
      color: var(--el-color-success);
    }
  }

  .summary-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0;
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .toolbar-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tg-space-2, 8px);
  }

  .platform-slug {
    margin-top: 4px;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
    font-family: ui-monospace, monospace;
  }

  .mono {
    font-family: ui-monospace, monospace;
  }

  .empty-hint {
    font-size: 13px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .json-field {
    width: 100%;
  }

  .json-textarea :deep(textarea) {
    font-family: ui-monospace, monospace;
    font-size: 12px;
  }

  .json-actions {
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }

  .drawer-note {
    margin: 0;
    font-size: 13px;
    color: var(--tg-color-text-secondary, #6b7280);
  }
}
</style>
