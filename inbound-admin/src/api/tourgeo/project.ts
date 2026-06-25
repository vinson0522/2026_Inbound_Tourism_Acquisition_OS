import type { CustomerProject } from './types';
import { MOCK_PROJECTS } from './mock-data';

const USE_MOCK = import.meta.env.VITE_TOURGEO_MOCK !== 'false';

export async function listProjects(): Promise<CustomerProject[]> {
  if (USE_MOCK) {
    await delay(200);
    return [...MOCK_PROJECTS];
  }
  // TODO: GET /api/v1/projects
  return [];
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
