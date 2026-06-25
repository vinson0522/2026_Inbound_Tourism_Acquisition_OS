<template>
  <div class="project-selector">
    <span v-if="showLabel" class="project-selector__label">当前项目</span>
    <el-select
      :model-value="projectStore.currentProjectId"
      placeholder="选择项目"
      filterable
      :loading="projectStore.loading"
      style="min-width: 220px"
      @change="onChange"
    >
      <el-option v-for="item in projectStore.projects" :key="item.id" :label="item.name" :value="item.id" />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { useProjectStore } from '@/store/modules/project';

withDefaults(
  defineProps<{
    showLabel?: boolean;
  }>(),
  { showLabel: true }
);

const emit = defineEmits<{ change: [projectId: number | null] }>();
const projectStore = useProjectStore();

onMounted(() => {
  if (!projectStore.initialized) {
    projectStore.fetchProjects();
  }
});

function onChange(id: number | null) {
  projectStore.setCurrentProject(id);
  emit('change', id);
}
</script>

<style scoped lang="scss">
.project-selector {
  display: inline-flex;
  align-items: center;
  gap: var(--tg-space-2, 8px);

  &__label {
    font-size: var(--tg-font-size-sm, 13px);
    color: var(--tg-color-text-secondary, #6b7280);
    white-space: nowrap;
  }
}
</style>
