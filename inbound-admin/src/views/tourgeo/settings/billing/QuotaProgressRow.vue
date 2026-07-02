<template>
  <div class="quota-row">
    <div class="quota-row__head">
      <div class="quota-row__label">
        <span>{{ item.label }}</span>
        <el-tag v-if="item.status === 'warning'" type="warning" size="small" class="quota-row__tag">即将用尽</el-tag>
        <el-tag v-else-if="item.status === 'overage'" type="danger" size="small" class="quota-row__tag">已超额</el-tag>
      </div>
      <span class="quota-row__value">
        {{ item.used }} / {{ displayLimit }} {{ item.unit }}
        <span v-if="item.period === 'monthly'" class="quota-row__period">/ 月</span>
      </span>
    </div>
    <el-progress
      :percentage="percentage"
      :status="progressStatus"
      :stroke-width="12"
      :aria-valuenow="item.used"
      :aria-valuemin="0"
      :aria-valuemax="displayLimit"
    />
    <p v-if="subText" class="quota-row__sub">{{ subText }}</p>
    <el-alert
      v-if="item.status === 'overage' && showInlineAlert"
      type="error"
      :closable="false"
      show-icon
      class="quota-row__alert"
      :title="`${item.label}已达上限（${item.used}/${displayLimit}）`"
      description="请等待下周期重置或联系升级套餐。"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { QuotaItemVo } from '@/api/tourgeo/types';
import { quotaProgressStatus } from '@/constants/billing';

const props = withDefaults(
  defineProps<{
    item: QuotaItemVo;
    showInlineAlert?: boolean;
  }>(),
  {
    showInlineAlert: false
  }
);

const displayLimit = computed(() => (props.item.limit > 0 ? props.item.limit : 0));

const percentage = computed(() => {
  if (props.item.percentage != null && props.item.percentage >= 0) {
    return Math.min(100, props.item.percentage);
  }
  const limit = props.item.limit;
  if (limit <= 0) return 0;
  return Math.min(100, Math.round((props.item.used / limit) * 100));
});

const progressStatus = computed(() => quotaProgressStatus(props.item.status));

const subText = computed(() => {
  const { used, limit, status } = props.item;
  if (limit <= 0) return '未配置额度上限';
  if (status === 'overage') {
    const over = Math.max(used - limit, 0);
    return over > 0 ? `已超额 ${over} ${props.item.unit}` : '已达上限';
  }
  if (status === 'warning') {
    const remain = Math.max(limit - used, 0);
    return `距超额还剩 ${remain} ${props.item.unit}`;
  }
  return '';
});
</script>

<style scoped lang="scss">
.quota-row {
  margin-bottom: 20px;

  &__head {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__label {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 140px;
    font-weight: 500;
    color: var(--tg-color-text-primary, #1f2937);
  }

  &__tag {
    flex-shrink: 0;
  }

  &__value {
    font-variant-numeric: tabular-nums;
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: var(--tg-font-size-sm, 13px);
  }

  &__period {
    margin-left: 2px;
  }

  &__sub {
    margin: 6px 0 0;
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
  }

  &__alert {
    margin-top: 10px;
  }
}

@media (max-width: 991px) {
  .quota-row__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
