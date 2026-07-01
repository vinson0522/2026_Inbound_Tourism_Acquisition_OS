import node from '@astrojs/node';
import { defineConfig } from 'astro/config';

/** @type {import('astro').AstroUserConfig} */
export default defineConfig({
  output: 'hybrid',
  adapter: node({ mode: 'standalone' }),
  server: {
    port: 4321,
    host: true,
  },
});
