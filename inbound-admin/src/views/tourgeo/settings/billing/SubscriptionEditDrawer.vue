<template>
  <el-drawer
    :model-value="visible"
    title="编辑套餐与额度"
    size="520px"
    destroy-on-close
    :before-close="handleBeforeClose"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-alert
      type="info"
      show-icon
      :closable="false"
      class="drawer-alert"
      title="用于演示/运营调整，非客户自助购买。保存后立即生效。"
    />

    <el-alert
      v-if="inactiveSubscription"
      type="warning"
      show-icon
      :closable="false"
      class="drawer-alert"
      title="当前订阅已失效，保存后仍写入 subscription 表，部分功能可能不可用。"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="132px" class="edit-form">
      <el-form-item label="套餐模板" prop="planCode">
        <el-select v-model="form.planCode" placeholder="选择套餐" style="width: 100%">
          <el-option v-for="opt in PLAN_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value">
            <span>{{ opt.label }}</span>
            <span class="plan-option-code">{{ opt.value }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <div class="section-title">额度上限 (quota_json)</div>

      <el-form-item
        v-for="field in QUOTA_FORM_FIELDS"
        :key="field.key"
        :label="field.label"
        :prop="`quotaJson.${field.key}`"
      >
        <el-input-number v-model="form.quotaJson[field.key]" :min="0" :step="1" controls-position="right" />
      </el-form-item>

      <div class="section-title">计费周期</div>

      <el-form-item label="周期开始" prop="periodStart">
        <el-date-picker
          v-model="form.periodStart"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="周期结束" prop="periodEnd">
        <el-date-picker
          v-model="form.periodEnd"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 100%"
        />
      </el-form-item>

      <el-alert
        type="warning"
        show-icon
        :closable="false"
        class="period-hint"
        title="修改周期不会自动清零 used_json；到期由系统 Job 重置。"
      />
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="applyPreset">套用模板默认额度</el-button>
        <div class="drawer-footer__actions">
          <el-button @click="requestClose">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { updateSubscription } from '@/api/tourgeo/billing';
import type { SubscriptionVo } from '@/api/tourgeo/types';
import {
  PLAN_OPTIONS,
  PLAN_QUOTA_PRESETS,
  QUOTA_FORM_FIELDS,
  type QuotaJson,
  isInactiveSubscription,
  quotasToJson
} from '@/constants/billing';

const props = defineProps<{
  visible: boolean;
  subscription: SubscriptionVo | null;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  saved: [data: SubscriptionVo];
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);
const snapshot = ref('');

const form = reactive({
  planCode: 'growth_service',
  quotaJson: { ...PLAN_QUOTA_PRESETS.growth_service } as QuotaJson,
  periodStart: '',
  periodEnd: ''
});

const inactiveSubscription = computed(() => isInactiveSubscription(props.subscription?.status));

const rules: FormRules = {
  planCode: [{ required: true, message: '请选择套餐模板', trigger: 'change' }],
  periodStart: [{ required: true, message: '请选择周期开始', trigger: 'change' }],
  periodEnd: [
    { required: true, message: '请选择周期结束', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !form.periodStart) {
          callback();
          return;
        }
        if (value <= form.periodStart) {
          callback(new Error('周期结束须晚于周期开始'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ],
  ...Object.fromEntries(
    QUOTA_FORM_FIELDS.map((field) => [
      `quotaJson.${field.key}`,
      [{ required: true, type: 'number', min: 0, message: `${field.label}须 ≥ 0`, trigger: 'blur' }]
    ])
  )
};

function serializeForm() {
  return JSON.stringify(form);
}

function isDirty() {
  return snapshot.value !== '' && serializeForm() !== snapshot.value;
}

function initForm(sub: SubscriptionVo | null) {
  if (!sub) return;
  form.planCode = sub.planCode;
  form.quotaJson = quotasToJson(sub.quotas ?? []);
  form.periodStart = sub.periodStart ?? '';
  form.periodEnd = sub.periodEnd ?? '';
  snapshot.value = serializeForm();
}

watch(
  () => [props.visible, props.subscription] as const,
  ([open, sub]) => {
    if (open && sub) {
      initForm(sub);
    }
  },
  { immediate: true }
);

function applyPreset() {
  const preset = PLAN_QUOTA_PRESETS[form.planCode];
  if (!preset) {
    ElMessage.warning('该套餐暂无预设额度');
    return;
  }
  form.quotaJson = { ...preset };
  ElMessage.success('已套用模板默认额度');
}

async function submit() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      const data = await updateSubscription({
        planCode: form.planCode,
        quotaJson: { ...form.quotaJson },
        periodStart: form.periodStart,
        periodEnd: form.periodEnd
      });
      ElMessage.success('套餐已更新');
      snapshot.value = serializeForm();
      emit('saved', data);
      emit('update:visible', false);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg && msg !== 'error') {
        ElMessage.error(msg);
      }
    } finally {
      saving.value = false;
    }
  });
}

async function handleBeforeClose(done: () => void) {
  if (!isDirty()) {
    done();
    return;
  }
  try {
    await ElMessageBox.confirm('有未保存的修改，确定关闭？', '提示', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    });
    done();
  } catch {
    /* cancelled */
  }
}

function requestClose() {
  handleBeforeClose(() => emit('update:visible', false));
}

defineExpose({ isDirty });
</script>

<style scoped lang="scss">
.drawer-alert {
  margin-bottom: 16px;
}

.edit-form {
  padding-right: 8px;
}

.section-title {
  margin: 8px 0 12px;
  padding-bottom: 8px;
  font-weight: 600;
  font-size: 13px;
  color: var(--tg-color-text-secondary, #6b7280);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.period-hint {
  margin-top: 4px;
}

.plan-option-code {
  float: right;
  font-size: 12px;
  color: var(--tg-color-text-secondary, #6b7280);
}

.drawer-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;

  &__actions {
    display: flex;
    gap: 8px;
    margin-left: auto;
  }
}
</style>
