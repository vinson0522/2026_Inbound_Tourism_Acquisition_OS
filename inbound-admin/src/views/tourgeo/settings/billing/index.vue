<template>
  <div class="p-2 tg-billing">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">套餐与额度</h1>
        <p class="tg-page-sub">系统设置 · 当前租户套餐用量（FR-804）</p>
      </div>
    </div>

    <el-skeleton v-if="loading && !subscription" :rows="10" animated />

    <el-empty v-else-if="notFound" description="暂无有效套餐">
      <p class="empty-hint">开发环境可能未配置 subscription 记录；请联系管理员。</p>
      <el-button type="primary" @click="loadSubscription">重试</el-button>
    </el-empty>

    <el-result v-else-if="forbidden" icon="warning" title="无权访问" sub-title="需要租户管理员权限查看套餐与额度。" />

    <template v-else-if="subscription">
      <el-alert
        v-if="inactiveSubscription"
        type="error"
        show-icon
        :closable="false"
        class="mb-3"
        title="订阅已失效，部分功能可能不可用"
      />

      <el-alert
        v-if="overageAlertText"
        type="error"
        show-icon
        :closable="false"
        class="mb-3"
        :title="overageAlertText"
      />

      <el-alert
        v-else-if="subscription.hasWarning"
        type="warning"
        show-icon
        :closable="false"
        class="mb-3"
        title="部分额度即将用尽，请合理安排本月操作。"
      />

      <el-card shadow="hover" class="mb-3">
        <template #header>
          <span class="card-title">套餐概览</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="当前套餐">
            <el-tag type="success" class="mr-2">{{ subscription.planName || planLabel(subscription.planCode) }}</el-tag>
            <code class="plan-code">{{ subscription.planCode }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="订阅状态">
            <el-tag :type="statusMeta.type">{{ statusMeta.label }}</el-tag>
            <span class="status-code">{{ subscription.status }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="计费周期">
            <span>{{ periodText }}</span>
            <span v-if="daysRemainingText" :class="['days-remaining', { 'is-warning': lowDaysRemaining }]">
              （{{ daysRemainingText }}）
            </span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="toolbar-actions">
          <el-tooltip content="在线升级 M2 · 请联系客户成功经理" placement="top">
            <el-button disabled>升级套餐</el-button>
          </el-tooltip>
          <el-tooltip content="增购额度 M2" placement="top">
            <el-button disabled>购买加量</el-button>
          </el-tooltip>
          <el-tooltip content="发票 M2" placement="top">
            <el-button disabled>下载发票</el-button>
          </el-tooltip>
        </div>
      </el-card>

      <el-card v-loading="loading" shadow="hover" class="mb-3">
        <template #header>
          <div class="quota-header">
            <span class="card-title">本周期额度使用</span>
            <el-button icon="Refresh" :loading="loading" @click="loadSubscription">刷新用量</el-button>
          </div>
        </template>
        <p class="quota-hint">
          <el-icon><InfoFilled /></el-icon>
          月度额度在周期结束日重置；客户项目数为租户总量上限。
        </p>
        <quota-progress-row v-for="item in subscription.quotas" :key="item.key" :item="item" />
      </el-card>

      <el-card shadow="never" class="info-card">
        <ul class="info-list">
          <li>超额时创建诊断、AI 生成等操作将返回「额度不足」并拦截。</li>
          <li>升级套餐或增购额度请联系您的客户成功经理。（M1 无在线支付）</li>
        </ul>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';
import { getCurrentSubscription } from '@/api/tourgeo/billing';
import type { SubscriptionVo } from '@/api/tourgeo/types';
import { isInactiveSubscription, planLabel, subscriptionStatusMeta } from '@/constants/billing';
import QuotaProgressRow from './QuotaProgressRow.vue';

const loading = ref(false);
const subscription = ref<SubscriptionVo | null>(null);
const notFound = ref(false);
const forbidden = ref(false);

const statusMeta = computed(() => subscriptionStatusMeta(subscription.value?.status));

const inactiveSubscription = computed(() => isInactiveSubscription(subscription.value?.status));

const periodText = computed(() => {
  const sub = subscription.value;
  if (!sub?.periodStart && !sub?.periodEnd) return '—';
  const start = sub.periodStart ?? '—';
  const end = sub.periodEnd ?? '—';
  return `${start} 至 ${end}`;
});

const lowDaysRemaining = computed(() => {
  const days = subscription.value?.daysRemaining;
  return days != null && days <= 7;
});

const daysRemainingText = computed(() => {
  const days = subscription.value?.daysRemaining;
  if (days == null) return '';
  return `剩余 ${days} 天`;
});

const overageItems = computed(() => {
  const quotas = subscription.value?.quotas ?? [];
  return quotas.filter((q) => q.status === 'overage' && q.limit > 0);
});

const overageAlertText = computed(() => {
  const items = overageItems.value;
  if (!items.length) return '';
  if (items.length === 1) {
    const q = items[0];
    return `套餐额度已用尽：${q.label}已达 ${q.used}/${q.limit}。相关操作已被拦截，请联系升级。`;
  }
  const labels = items.map((q) => q.label).join('、');
  return `多项套餐额度已用尽（${labels}）。请联系升级。`;
});

async function loadSubscription() {
  loading.value = true;
  notFound.value = false;
  forbidden.value = false;
  try {
    subscription.value = await getCurrentSubscription();
  } catch (e: unknown) {
    subscription.value = null;
    const msg = e instanceof Error ? e.message : String(e);
    if (msg.includes('403') || msg.includes('无权') || msg.includes('Forbidden')) {
      forbidden.value = true;
    } else if (msg.includes('404') || msg.includes('暂无有效套餐') || msg.includes('not found')) {
      notFound.value = true;
    } else if (msg !== 'error') {
      ElMessage.error(msg || '加载套餐信息失败');
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadSubscription();
});
</script>

<style scoped lang="scss">
.tg-billing {
  .card-title {
    font-weight: 600;
  }

  .empty-hint {
    margin: 0 0 12px;
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: var(--tg-font-size-sm, 13px);
  }

  .plan-code {
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .status-code {
    margin-left: 8px;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .days-remaining {
    margin-left: 4px;
    color: var(--tg-color-text-secondary, #6b7280);

    &.is-warning {
      color: var(--el-color-warning);
      font-weight: 500;
    }
  }

  .toolbar-actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tg-space-2, 8px);
    margin-top: var(--tg-space-4, 16px);
  }

  .quota-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .quota-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    margin: 0 0 16px;
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  .info-card {
    background: var(--el-color-info-light-9, #f4f4f5);
    border: none;
  }

  .info-list {
    margin: 0;
    padding-left: 1.2em;
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: var(--tg-font-size-sm, 13px);
    line-height: 1.8;
  }
}

@media (max-width: 991px) {
  .tg-billing .toolbar-actions {
    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>
