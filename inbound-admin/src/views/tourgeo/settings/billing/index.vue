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
          <el-button v-hasPermi="['tourgeo:billing:edit']" type="primary" plain @click="editDrawerVisible = true">
            编辑套餐
          </el-button>
          <el-tooltip content="在线购买 M3+ · 请用编辑套餐调整 quota" placement="top">
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
            <div class="quota-header__actions">
              <el-button icon="Refresh" :loading="loading" @click="loadSubscription">刷新用量</el-button>
              <el-button
                v-hasPermi="['tourgeo:billing:reset']"
                type="danger"
                link
                :loading="resetting"
                @click="openResetDialog"
              >
                重置本周期用量
              </el-button>
            </div>
          </div>
        </template>
        <p class="quota-hint">
          <el-icon><InfoFilled /></el-icon>
          月度额度在周期结束日重置；客户项目数为租户总量上限。
        </p>
        <quota-progress-row v-for="item in subscription.quotas" :key="item.key" :item="item" />
      </el-card>

      <el-card shadow="never" class="info-card mb-3">
        <template #header>
          <span class="card-title">说明</span>
        </template>
        <ul class="info-list">
          <li>超额时创建诊断、AI 生成等操作将返回「额度不足」并拦截。</li>
          <li>升级套餐或增购额度请联系您的客户成功经理。（M1 无在线支付）</li>
        </ul>
      </el-card>

      <el-card shadow="never" class="info-card">
        <template #header>
          <span class="card-title">计费周期与重置</span>
        </template>
        <ul class="info-list">
          <li>系统每日 02:00 检查：若 period_end 早于今天，则推进计费周期并将 5 项月度 used_json 归零；projects 累计保留。</li>
          <li>客户项目数为租户生命周期累计上限，不随账单周期清零。</li>
          <li>手动「重置本周期用量」等效触发一次周期结算，仅用于演示或运维排障（M2 写应用 log，无审计 UI）。</li>
          <li>重置后，若 quota 上限未变，402 超额拦截将解除（projects 超额除外）。</li>
          <li>无 Stripe；套餐变更通过「编辑套餐」写入 subscription 表。</li>
        </ul>
      </el-card>
    </template>

    <subscription-edit-drawer
      ref="editDrawerRef"
      v-model:visible="editDrawerVisible"
      :subscription="subscription"
      @saved="onSubscriptionSaved"
    />

    <el-dialog v-model="resetDialogVisible" title="确认重置本周期用量" width="440px" destroy-on-close>
      <el-alert type="warning" show-icon :closable="false" class="mb-3" title="此操作不可撤销，仅用于演示或运维排障。" />
      <p class="reset-copy">将把以下 <strong>月度</strong> 已用额度归零：</p>
      <ul class="reset-list">
        <li>GEO 诊断 · 关键词 · 内容 · 落地页 · 报告生成</li>
        <li><strong>客户项目数 (projects) 不重置</strong>（租户总量上限）</li>
        <li>若今日 ≥ period_end，将推进下一计费周期（与自动 Job 一致）</li>
      </ul>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="resetting" @click="confirmReset">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { InfoFilled } from '@element-plus/icons-vue';
import { getCurrentSubscription, resetBillingPeriod } from '@/api/tourgeo/billing';
import type { SubscriptionVo } from '@/api/tourgeo/types';
import { isInactiveSubscription, planLabel, subscriptionStatusMeta } from '@/constants/billing';
import QuotaProgressRow from './QuotaProgressRow.vue';
import SubscriptionEditDrawer from './SubscriptionEditDrawer.vue';

const loading = ref(false);
const resetting = ref(false);
const subscription = ref<SubscriptionVo | null>(null);
const notFound = ref(false);
const forbidden = ref(false);
const editDrawerVisible = ref(false);
const resetDialogVisible = ref(false);
const editDrawerRef = ref<InstanceType<typeof SubscriptionEditDrawer>>();

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

function onSubscriptionSaved(data: SubscriptionVo) {
  subscription.value = data;
}

function openResetDialog() {
  resetDialogVisible.value = true;
}

async function confirmReset() {
  resetting.value = true;
  try {
    subscription.value = await resetBillingPeriod();
    ElMessage.success('本周期用量已重置');
    resetDialogVisible.value = false;
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    if (msg && msg !== 'error') {
      ElMessage.error(msg);
    }
  } finally {
    resetting.value = false;
  }
}

onBeforeRouteLeave(async (_to, _from, next) => {
  if (editDrawerVisible.value && editDrawerRef.value?.isDirty?.()) {
    try {
      await ElMessageBox.confirm('有未保存的套餐修改，确定离开？', '提示', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑'
      });
      next();
    } catch {
      next(false);
    }
    return;
  }
  next();
});

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

    &__actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }
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

  .reset-copy {
    margin: 0 0 8px;
    color: var(--tg-color-text-primary, #1f2937);
  }

  .reset-list {
    margin: 0;
    padding-left: 1.2em;
    color: var(--tg-color-text-secondary, #6b7280);
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
