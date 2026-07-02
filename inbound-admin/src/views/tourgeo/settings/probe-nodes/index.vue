<template>
  <div class="p-2 tg-probe-nodes">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">探针节点</h1>
        <p class="tg-page-sub">系统设置 · 浏览器扩展探针节点池（FR-113）</p>
      </div>
    </div>

    <el-result v-if="forbidden" icon="warning" title="无权访问" sub-title="需要租户管理员权限查看探针节点。" />

    <template v-else>
      <el-alert
        v-if="showNoOnlineWarning"
        type="warning"
        show-icon
        :closable="false"
        class="mb-3"
        title="当前无在线节点，browser-extension 诊断任务将无法派发。"
      />

      <el-card shadow="hover" class="mb-3">
        <template #header>
          <div class="summary-header">
            <span class="card-title">探针节点池</span>
            <div class="toolbar-actions">
              <router-link to="/settings/probe-adapters">
                <el-button link type="primary">配置 Adapter →</el-button>
              </router-link>
              <el-button @click="installDrawerVisible = true">查看安装说明</el-button>
              <el-tooltip content="由扩展自动注册 · FR-113" placement="top">
                <el-button disabled>注册节点</el-button>
              </el-tooltip>
              <el-button icon="Refresh" :loading="loading" @click="loadNodes">刷新</el-button>
            </div>
          </div>
        </template>

        <p v-if="!loading || nodes.length" class="summary-text">
          已注册 <strong>{{ summary.total }}</strong> · 在线
          <strong class="online-count">{{ summary.online }}</strong> · 离线
          <strong>{{ summary.offline }}</strong>
        </p>
        <p class="summary-hint">
          <el-icon><InfoFilled /></el-icon>
          浏览器扩展节点用于 browser-extension 探针；grounded-api 不依赖节点。
        </p>
      </el-card>

      <el-card v-loading="loading" shadow="hover" class="mb-3">
        <el-empty v-if="!loading && nodes.length === 0" description="暂无探针节点">
          <ol class="empty-steps">
            <li>在 Chrome 安装「TourGEO Probe」扩展（仓库 <code>inbound-probe-extension/</code>）</li>
            <li>配置 API 地址与 Node Key（与租户 allowlist 一致，如 <code>demo-probe-1</code>）</li>
            <li>扩展启动后将自动注册并发送心跳</li>
          </ol>
          <div class="empty-actions">
            <el-button type="primary" @click="installDrawerVisible = true">查看安装说明</el-button>
            <el-button @click="copyConfigExample">复制配置示例</el-button>
          </div>
        </el-empty>

        <el-table v-else :data="sortedNodes" border stripe>
          <el-table-column label="节点" min-width="160">
            <template #default="{ row }">
              <div class="node-key">{{ row.nodeKey }}</div>
              <div class="node-id">#{{ row.id }}</div>
            </template>
          </el-table-column>
          <el-table-column label="地区" prop="region" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.region" size="small" type="info">{{ row.region }}</el-tag>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="支持平台" min-width="160" class-name="col-platforms">
            <template #default="{ row }">
              <template v-if="visiblePlatforms(row.platforms).length">
                <el-tag
                  v-for="p in visiblePlatforms(row.platforms)"
                  :key="p"
                  size="small"
                  class="mr-1 mb-1"
                >
                  {{ probePlatformLabel(p) }}
                </el-tag>
                <el-tag v-if="hiddenPlatformCount(row.platforms) > 0" size="small" type="info">
                  +{{ hiddenPlatformCount(row.platforms) }}
                </el-tag>
              </template>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column label="扩展版本" prop="extensionVersion" width="100" class-name="col-version">
            <template #default="{ row }">
              {{ row.extensionVersion || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="在线" width="100">
            <template #default="{ row }">
              <span class="online-indicator" :class="probeOnlineMeta(row).dotClass">
                <span class="dot" aria-hidden="true" />
                {{ probeOnlineMeta(row).label }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最后心跳" min-width="170">
            <template #default="{ row }">
              {{ formatHeartbeat(row.lastHeartbeatAt) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="probeNodeStatusMeta(row.status).type">
                {{ probeNodeStatusMeta(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="copyNodeKey(row.nodeKey)">复制 Key</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-alert
        type="info"
        show-icon
        :closable="false"
        title="扩展仅处理系统下发的诊断任务问题，不上传您浏览器中的其他对话内容。安装前请阅读授权说明。"
      />
    </template>

    <el-drawer v-model="installDrawerVisible" title="探针扩展安装说明" size="480px">
      <div class="install-drawer">
        <p><strong>仓库路径</strong>：<code>inbound-probe-extension/</code></p>
        <p><strong>环境变量</strong>：</p>
        <ul>
          <li><code>PLASMO_PUBLIC_API_BASE</code> — Java API 地址</li>
          <li><code>PLASMO_PUBLIC_NODE_KEY</code> — 与运维 allowlist 一致</li>
        </ul>
        <p><strong>开发加载</strong>：</p>
        <pre class="install-code">cd inbound-probe-extension
pnpm install
pnpm dev
# Chrome → 扩展 → 加载已解压 → build/chrome-mv3-dev</pre>
        <p class="install-note">扩展启动后自动调用 <code>POST /api/v1/probe/nodes/register</code> 并每 30s poll 任务。</p>
        <div class="drawer-actions">
          <el-button @click="copyConfigExample">复制配置示例</el-button>
          <el-button type="primary" @click="installDrawerVisible = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';
import { listProbeNodes } from '@/api/tourgeo/probe';
import type { ProbeNodeVo } from '@/api/tourgeo/types';
import {
  PROBE_CONFIG_EXAMPLE,
  probeNodeStatusMeta,
  probeOnlineMeta,
  probePlatformLabel
} from '@/constants/probe';

const MAX_VISIBLE_PLATFORMS = 3;

const loading = ref(false);
const forbidden = ref(false);
const nodes = ref<ProbeNodeVo[]>([]);
const installDrawerVisible = ref(false);

const summary = computed(() => {
  const total = nodes.value.length;
  const online = nodes.value.filter((n) => n.online && n.status === 'ACTIVE').length;
  return { total, online, offline: total - online };
});

const showNoOnlineWarning = computed(
  () => !loading.value && nodes.value.length > 0 && summary.value.online === 0
);

const sortedNodes = computed(() =>
  [...nodes.value].sort((a, b) => {
    const ta = a.lastHeartbeatAt ? Date.parse(a.lastHeartbeatAt) : 0;
    const tb = b.lastHeartbeatAt ? Date.parse(b.lastHeartbeatAt) : 0;
    return tb - ta;
  })
);

function visiblePlatforms(platforms: string[]): string[] {
  return (platforms ?? []).slice(0, MAX_VISIBLE_PLATFORMS);
}

function hiddenPlatformCount(platforms: string[]): number {
  const len = platforms?.length ?? 0;
  return len > MAX_VISIBLE_PLATFORMS ? len - MAX_VISIBLE_PLATFORMS : 0;
}

function formatHeartbeat(value?: string): string {
  if (!value) return '从未';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('zh-CN', { hour12: false });
}

async function copyText(text: string, label: string) {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage.success(`${label}已复制`);
  } catch {
    ElMessage.error('复制失败');
  }
}

function copyNodeKey(nodeKey: string) {
  void copyText(nodeKey, 'Node Key');
}

function copyConfigExample() {
  void copyText(JSON.stringify(PROBE_CONFIG_EXAMPLE, null, 2), '配置示例');
}

async function loadNodes() {
  loading.value = true;
  forbidden.value = false;
  try {
    nodes.value = await listProbeNodes();
  } catch (e: unknown) {
    nodes.value = [];
    const msg = e instanceof Error ? e.message : String(e);
    if (msg.includes('403') || msg.includes('无权') || msg.includes('Forbidden')) {
      forbidden.value = true;
    } else if (msg !== 'error') {
      ElMessage.error(msg || '加载探针节点失败');
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadNodes();
});
</script>

<style scoped lang="scss">
.tg-probe-nodes {
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

    .online-count {
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
    gap: var(--tg-space-2, 8px);
  }

  .empty-steps {
    margin: 0 0 16px;
    padding-left: 1.2em;
    text-align: left;
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: var(--tg-font-size-sm, 13px);
    line-height: 1.8;

    code {
      font-size: 12px;
    }
  }

  .empty-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
  }

  .node-key {
    font-family: ui-monospace, monospace;
    font-weight: 500;
  }

  .node-id {
    margin-top: 2px;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .online-indicator {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--tg-color-text-muted, #9ca3af);
    }

    &.is-online .dot {
      background: var(--el-color-success);
    }

    &.is-offline .dot {
      background: var(--tg-color-text-muted, #9ca3af);
    }

    &.is-never .dot {
      background: var(--el-color-info-light-5, #c8c9cc);
    }
  }

  .install-drawer {
    font-size: 14px;
    line-height: 1.7;

    code {
      font-size: 12px;
    }
  }

  .install-code {
    padding: 12px;
    border-radius: 6px;
    background: var(--el-fill-color-light, #f5f7fa);
    font-size: 12px;
    line-height: 1.6;
    overflow-x: auto;
  }

  .install-note {
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: 13px;
  }

  .drawer-actions {
    display: flex;
    gap: 8px;
    margin-top: 16px;
  }
}

@media (max-width: 1199px) {
  .tg-probe-nodes :deep(.col-version) {
    display: none;
  }
}

@media (max-width: 767px) {
  .tg-probe-nodes :deep(.col-platforms) {
    display: none;
  }
}
</style>
