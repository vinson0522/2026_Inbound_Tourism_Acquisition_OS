# EPIC-6 M2 — Astro 4 SSG + preview on :4321
FROM node:20-alpine AS builder

WORKDIR /app

RUN corepack enable && corepack prepare pnpm@9.15.9 --activate

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile 2>/dev/null || pnpm install

COPY . .

ARG PUBLIC_API_BASE_URL=http://localhost:8080
ARG PUBLIC_TURNSTILE_SITE_KEY=
ARG PUBLIC_LANDING_BASE_URL=http://localhost:4321
ARG PUBLIC_SITE_URL=http://localhost:4321
ARG PUBLIC_ADMIN_URL=http://localhost:5173
ARG PUBLIC_DEMO_URL=mailto:hello@tourgeo.ai
ENV PUBLIC_API_BASE_URL=${PUBLIC_API_BASE_URL}
ENV PUBLIC_TURNSTILE_SITE_KEY=${PUBLIC_TURNSTILE_SITE_KEY}
ENV PUBLIC_LANDING_BASE_URL=${PUBLIC_LANDING_BASE_URL}
ENV PUBLIC_SITE_URL=${PUBLIC_SITE_URL}
ENV PUBLIC_ADMIN_URL=${PUBLIC_ADMIN_URL}
ENV PUBLIC_DEMO_URL=${PUBLIC_DEMO_URL}

RUN pnpm build

FROM node:20-alpine

WORKDIR /app

RUN corepack enable && corepack prepare pnpm@9.15.9 --activate

COPY --from=builder /app/package.json /app/pnpm-lock.yaml* ./
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/.astro ./.astro

ENV HOST=0.0.0.0
ENV PORT=4321
EXPOSE 4321

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=20s \
  CMD wget -qO- http://127.0.0.1:4321/ >/dev/null 2>&1 || exit 1

CMD ["node", "./dist/server/entry.mjs"]
