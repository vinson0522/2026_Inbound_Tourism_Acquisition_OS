import { defineStore } from 'pinia';
import { listProjects } from '@/api/tourgeo/project';
import type { CustomerProject } from '@/api/tourgeo/types';

export const useProjectStore = defineStore('tourgeoProject', () => {
  const projects = ref<CustomerProject[]>([]);
  const currentProjectId = ref<number | null>(null);
  const loading = ref(false);
  const initialized = ref(false);

  const currentProject = computed(() => projects.value.find((p) => p.id === currentProjectId.value) ?? null);

  const hasProject = computed(() => projects.value.length > 0 && currentProjectId.value != null);

  async function fetchProjects() {
    loading.value = true;
    try {
      projects.value = await listProjects();
      if (projects.value.length && currentProjectId.value == null) {
        currentProjectId.value = projects.value[0].id;
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
