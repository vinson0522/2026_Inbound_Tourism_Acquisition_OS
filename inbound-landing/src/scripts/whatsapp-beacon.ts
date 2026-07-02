const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e';

function parseUtm(): Record<string, string> {
  const params = new URLSearchParams(window.location.search);
  const utm: Record<string, string> = {};
  for (const key of ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content']) {
    const v = params.get(key);
    if (v) utm[key] = v;
  }
  return utm;
}

export function trackWhatsAppClick(opts: {
  apiBase: string;
  projectId: number;
  landingPageId: number;
}): void {
  const apiBase = opts.apiBase.replace(/\/+$/, '');
  if (!apiBase || !opts.projectId || !opts.landingPageId) return;

  const body = JSON.stringify({
    eventType: 'whatsapp_click',
    projectId: opts.projectId,
    landingPageId: opts.landingPageId,
    utm: parseUtm(),
    device: navigator.userAgent.slice(0, 200),
  });
  const url = `${apiBase}/api/v1/public/lead-events`;

  try {
    if (navigator.sendBeacon) {
      navigator.sendBeacon(url, new Blob([body], { type: 'application/json' }));
      return;
    }
    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        clientid: CLIENT_ID,
      },
      body,
      keepalive: true,
    }).catch(() => {});
  } catch {
    // Fire-and-forget — never block wa.me navigation
  }
}

let initialized = false;

export function initWhatsAppBeacon(): void {
  if (initialized) return;
  initialized = true;

  document.addEventListener(
    'click',
    (event) => {
      const link = (event.target as Element | null)?.closest<HTMLAnchorElement>('a[data-wa-beacon]');
      if (!link) return;

      const projectId = Number(link.dataset.projectId);
      const landingPageId = Number(link.dataset.landingPageId);
      const apiBase = link.dataset.apiBase ?? '';
      trackWhatsAppClick({ apiBase, projectId, landingPageId });
    },
    true
  );
}
