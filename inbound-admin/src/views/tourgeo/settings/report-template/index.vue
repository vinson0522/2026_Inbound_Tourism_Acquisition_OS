<template>
  <div class="p-2 tg-report-template">
    <div class="tg-page-header">
      <div>
        <h1 class="tg-page-title">报告模板</h1>
        <p class="tg-page-sub">系统设置 · 租户报告白标（FR-704）</p>
      </div>
    </div>

    <el-skeleton v-if="loading && !loaded" :rows="12" animated />

    <template v-else>
      <el-row :gutter="16">
        <el-col :xs="24" :lg="14">
          <el-card shadow="hover" class="mb-3">
            <template #header>
              <span class="card-title">报告白标模板</span>
            </template>
            <el-alert type="info" :closable="false" show-icon class="mb-3">
              配置将应用于本租户所有项目的报告导出（周报 / 月报 / 诊断）。
            </el-alert>

            <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" @submit.prevent>
              <el-form-item label="Logo URL" prop="logoUrl">
                <div class="logo-row">
                  <el-input v-model="form.logoUrl" placeholder="https://cdn.example.com/logo.png" clearable />
                  <el-button :disabled="!form.logoUrl?.trim()" @click="previewLogo">预览</el-button>
                </div>
                <p v-if="logoLoadError" class="field-hint is-error">Logo 加载失败，请检查 URL</p>
                <div v-else-if="logoPreviewVisible && form.logoUrl?.trim()" class="logo-preview">
                  <img :src="form.logoUrl.trim()" alt="logo preview" @error="onLogoError" @load="onLogoLoad" />
                </div>
              </el-form-item>
              <el-form-item label="封面标题" prop="coverTitle">
                <el-input v-model="form.coverTitle" maxlength="80" show-word-limit />
              </el-form-item>
              <el-form-item label="公司名称" prop="companyName">
                <el-input v-model="form.companyName" maxlength="80" show-word-limit />
              </el-form-item>
              <el-form-item label="主题主色" prop="primaryColor">
                <div class="color-row">
                  <el-color-picker v-model="form.primaryColor" />
                  <el-input v-model="form.primaryColor" maxlength="16" style="width: 120px" />
                </div>
              </el-form-item>
              <el-form-item label="页脚文案" prop="footerText">
                <el-input v-model="form.footerText" type="textarea" :rows="2" maxlength="200" show-word-limit />
              </el-form-item>
              <el-form-item label="导出章节" prop="sections">
                <el-checkbox-group v-model="form.sections">
                  <el-checkbox v-for="opt in REPORT_SECTION_OPTIONS" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item>
                <el-button @click="confirmResetDefaults">恢复默认</el-button>
                <el-button @click="handleCancel">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存模板</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="10">
          <el-card shadow="hover" class="mb-3 preview-card">
            <template #header>
              <span class="card-title">封面预览（示意）</span>
            </template>
            <div class="cover-mock" :style="{ borderTopColor: safeColor }">
              <div v-if="form.logoUrl?.trim()" class="cover-mock__logo">
                <img :src="form.logoUrl.trim()" alt="logo" @error="() => {}" />
              </div>
              <div v-else class="cover-mock__logo cover-mock__logo--placeholder">Logo</div>
              <h2 class="cover-mock__title" :style="{ color: safeColor }">{{ form.coverTitle || '—' }}</h2>
              <p class="cover-mock__company">{{ form.companyName || '—' }}</p>
              <p class="cover-mock__period">{{ previewPeriodLabel }} · 增长月报</p>
              <ul v-if="form.sections.length" class="cover-mock__sections">
                <li v-for="sec in visibleSections" :key="sec.value">{{ sec.label }}</li>
              </ul>
              <p class="cover-mock__footer">{{ form.footerText || '—' }}</p>
            </div>
            <p class="preview-hint">实际 DOCX/PDF 版式由服务端渲染；此处仅前端 mock。</p>
          </el-card>
        </el-col>
      </el-row>

      <el-alert type="info" :closable="false" show-icon title="Logo 请使用 HTTPS 可访问图片 URL；关闭章节后导出文档不渲染该章。" />
    </template>
  </div>
</template>

<script setup lang="ts" name="ReportTemplateSettings">
import type { FormInstance, FormRules } from 'element-plus';
import { getReportTemplate, saveReportTemplate } from '@/api/tourgeo/report';
import type { ReportSectionKey, ReportTemplateSaveForm } from '@/api/tourgeo/types';
import {
  DEFAULT_REPORT_SECTIONS,
  DEFAULT_REPORT_TEMPLATE,
  REPORT_SECTION_OPTIONS,
  defaultPreviousMonthStr,
  formatMonthLabel
} from '@/constants/report';
import { onBeforeRouteLeave, useRouter } from 'vue-router';

const router = useRouter();
const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const loaded = ref(false);
const logoLoadError = ref(false);
const logoPreviewVisible = ref(false);
const savedSnapshot = ref('');
const templateId = ref<number | null>(null);

const form = reactive<ReportTemplateSaveForm>({
  logoUrl: '',
  coverTitle: DEFAULT_REPORT_TEMPLATE.coverTitle,
  companyName: DEFAULT_REPORT_TEMPLATE.companyName,
  primaryColor: DEFAULT_REPORT_TEMPLATE.primaryColor,
  footerText: DEFAULT_REPORT_TEMPLATE.footerText,
  sections: [...DEFAULT_REPORT_SECTIONS]
});

const rules: FormRules = {
  coverTitle: [{ required: true, message: '请输入封面标题', trigger: 'blur' }],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  sections: [
    {
      type: 'array',
      required: true,
      min: 1,
      message: '至少选择 1 个导出章节',
      trigger: 'change'
    }
  ]
};

const previewPeriodLabel = computed(() => formatMonthLabel(defaultPreviousMonthStr()));

const safeColor = computed(() => {
  const c = form.primaryColor?.trim() || DEFAULT_REPORT_TEMPLATE.primaryColor;
  return /^#?[0-9A-Fa-f]{3,8}$/.test(c) ? (c.startsWith('#') ? c : `#${c}`) : DEFAULT_REPORT_TEMPLATE.primaryColor;
});

const visibleSections = computed(() =>
  REPORT_SECTION_OPTIONS.filter((opt) => form.sections.includes(opt.value))
);

const isDirty = computed(() => snapshotForm() !== savedSnapshot.value);

function snapshotForm(): string {
  return JSON.stringify({
    logoUrl: form.logoUrl?.trim() ?? '',
    coverTitle: form.coverTitle,
    companyName: form.companyName,
    primaryColor: form.primaryColor,
    footerText: form.footerText,
    sections: [...form.sections].sort()
  });
}

function applyVo(data: Awaited<ReturnType<typeof getReportTemplate>>) {
  templateId.value = data.templateId ?? null;
  form.logoUrl = data.logoUrl ?? '';
  form.coverTitle = data.coverTitle ?? DEFAULT_REPORT_TEMPLATE.coverTitle;
  form.companyName = data.companyName ?? DEFAULT_REPORT_TEMPLATE.companyName;
  form.primaryColor = data.primaryColor ?? DEFAULT_REPORT_TEMPLATE.primaryColor;
  form.footerText = data.footerText ?? DEFAULT_REPORT_TEMPLATE.footerText;
  form.sections = (data.sections?.length ? data.sections : DEFAULT_REPORT_SECTIONS) as ReportSectionKey[];
  savedSnapshot.value = snapshotForm();
}

function applyDefaults() {
  form.logoUrl = DEFAULT_REPORT_TEMPLATE.logoUrl;
  form.coverTitle = DEFAULT_REPORT_TEMPLATE.coverTitle;
  form.companyName = DEFAULT_REPORT_TEMPLATE.companyName;
  form.primaryColor = DEFAULT_REPORT_TEMPLATE.primaryColor;
  form.footerText = DEFAULT_REPORT_TEMPLATE.footerText;
  form.sections = [...DEFAULT_REPORT_SECTIONS];
}

async function loadTemplate() {
  loading.value = true;
  try {
    const data = await getReportTemplate();
    applyVo(data);
    loaded.value = true;
  } catch (e) {
    const msg = e instanceof Error ? e.message : '加载模板失败';
    ElMessage.error(msg);
  } finally {
    loading.value = false;
  }
}

function previewLogo() {
  logoLoadError.value = false;
  logoPreviewVisible.value = true;
}

function onLogoError() {
  logoLoadError.value = true;
}

function onLogoLoad() {
  logoLoadError.value = false;
}

async function confirmResetDefaults() {
  try {
    await ElMessageBox.confirm('将表单恢复为系统默认白标配置，需点击「保存模板」后生效。', '恢复默认', {
      type: 'warning'
    });
    applyDefaults();
  } catch {
    /* cancelled */
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const saved = await saveReportTemplate({
      logoUrl: form.logoUrl?.trim() ?? '',
      coverTitle: form.coverTitle.trim(),
      companyName: form.companyName.trim(),
      primaryColor: form.primaryColor?.trim() || DEFAULT_REPORT_TEMPLATE.primaryColor,
      footerText: form.footerText?.trim() ?? '',
      sections: form.sections
    });
    applyVo(saved);
    ElMessage.success('报告模板已保存');
  } catch (e) {
    const msg = e instanceof Error ? e.message : '保存失败';
    if (msg !== 'error') ElMessage.error(msg);
  } finally {
    saving.value = false;
  }
}

async function handleCancel() {
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' });
    } catch {
      return;
    }
  }
  router.push('/reports/index');
}

onBeforeRouteLeave((_to, _from, next) => {
  if (!isDirty.value) {
    next();
    return;
  }
  ElMessageBox.confirm('有未保存的修改，确定离开？', '提示', { type: 'warning' })
    .then(() => next())
    .catch(() => next(false));
});

onMounted(() => {
  loadTemplate();
});
</script>

<style scoped lang="scss">
.tg-report-template {
  .card-title {
    font-weight: 600;
  }

  .logo-row {
    display: flex;
    gap: 8px;
    width: 100%;
  }

  .field-hint {
    margin: 4px 0 0;
    font-size: 12px;

    &.is-error {
      color: var(--el-color-danger);
    }
  }

  .logo-preview img {
    max-height: 48px;
    margin-top: 8px;
  }

  .color-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .cover-mock {
    border: 1px dashed var(--el-border-color);
    border-top: 4px solid var(--el-color-primary);
    border-radius: 8px;
    padding: 20px 16px;
    min-height: 280px;
    text-align: center;
    background: #fafafa;
  }

  .cover-mock__logo {
    margin-bottom: 12px;

    img {
      max-height: 40px;
      max-width: 120px;
    }

    &--placeholder {
      display: inline-block;
      padding: 8px 16px;
      background: #e5e7eb;
      color: #6b7280;
      font-size: 12px;
      border-radius: 4px;
    }
  }

  .cover-mock__title {
    margin: 0 0 8px;
    font-size: 18px;
  }

  .cover-mock__company {
    margin: 0 0 4px;
    color: var(--tg-color-text-secondary, #6b7280);
    font-size: 14px;
  }

  .cover-mock__period {
    margin: 0 0 16px;
    font-size: 13px;
    color: var(--tg-color-text-regular, #4b5563);
  }

  .cover-mock__sections {
    list-style: none;
    padding: 0;
    margin: 0 0 16px;
    text-align: left;
    font-size: 13px;
    color: var(--tg-color-text-regular, #4b5563);

    li {
      padding: 4px 0;
      border-bottom: 1px dotted var(--el-border-color-lighter);
    }
  }

  .cover-mock__footer {
    margin: 16px 0 0;
    padding-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
    font-size: 12px;
    color: #9ca3af;
  }

  .preview-hint {
    margin: 12px 0 0;
    font-size: 12px;
    color: var(--tg-color-text-secondary, #6b7280);
  }
}
</style>
