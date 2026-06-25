<template>
  <el-tag :type="meta.type" effect="light">{{ meta.label }}</el-tag>
</template>

<script setup lang="ts">
import { DIAGNOSTIC_STATUS_META, type DiagnosticRunStatus } from '@/constants/diagnostic';

const props = defineProps<{ status: DiagnosticRunStatus }>();

const meta = computed(() => DIAGNOSTIC_STATUS_META[props.status]);
</script>
