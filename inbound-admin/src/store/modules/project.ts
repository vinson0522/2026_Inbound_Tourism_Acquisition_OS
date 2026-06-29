import { defineStore } from 'pinia';
import { listProjectOptions } from '@/api/tourgeo/project';
import type { CustomerProjectVo } from '@/api/tourgeo/types';

export const useProjectStore = defineStore('tourgeoProject', () => {
  const projects = ref<CustomerProjectVo[]>([]);
  const currentProjectId = ref<number | null>(null);
  const loading = ref(false);
  const initialized = ref(false);

  const currentProject = computed(() => projects.value.find((p) => p.id === currentProjectId.value) ?? null);

  const hasProject = computed(() => projects.value.length > 0 && currentProjectId.value != null);

  async function fetchProjects() {
    loading.value = true;
    try {
      const res = await listProjectOptions();
      projects.value = res.data ?? [];
      if (projects.value.length && currentProjectId.value == null) {
        currentProjectId.value = projects.value[0].id;
      } else if (currentProjectId.value != null && !projects.value.some((p) => p.id === currentProjectId.value)) {
        currentProjectId.value = projects.value[0]?.id ?? null;
      }
      initialized.value = true;
    } finally {
      loading.value = false;
    }
  }

  function setCurrentProject(id: number | null) {
    currentProjectId.value = id;
  }

  return {
    projects,
    currentProjectId,
    currentProject,
    hasProject,
    loading,
    initialized,
    fetchProjects,
    setCurrentProject
  };
});
