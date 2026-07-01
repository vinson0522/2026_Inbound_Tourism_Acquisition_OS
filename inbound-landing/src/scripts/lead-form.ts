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

function getTurnstileToken(siteKey: string): string {
  if (!siteKey) return '';
  const w = window as Window & {
    turnstile?: { getResponse?: () => string; reset?: () => void };
  };
  return w.turnstile?.getResponse?.() ?? '';
}

function resetTurnstile(siteKey: string): void {
  if (!siteKey) return;
  const w = window as Window & { turnstile?: { reset?: () => void } };
  w.turnstile?.reset?.();
}

function showAlert(el: HTMLElement, type: 'success' | 'error', message: string): void {
  el.hidden = false;
  el.className = `lp-alert lp-alert--${type}`;
  el.textContent = message;
}

function mapFieldToBody(name: string, value: string): Record<string, unknown> {
  switch (name) {
    case 'travel_date':
      return value ? { travelDate: value } : {};
    case 'party_size':
    case 'pax':
      return value ? { partySize: Number(value) } : {};
    case 'notes':
      return value ? { message: value } : {};
    default:
      return value ? { [name]: value } : {};
  }
}

export function initLeadForm(): void {
  const form = document.getElementById('inbound-lead-form') as HTMLFormElement | null;
  if (!form) return;

  const alertEl = document.getElementById('lead-form-alert') as HTMLElement | null;
  const submitBtn = document.getElementById('lead-submit-btn') as HTMLButtonElement | null;
  const landingPageId = Number(form.dataset.landingId);
  const apiBase = (form.dataset.apiBase ?? 'http://localhost:8080').replace(/\/+$/, '');
  const siteKey = form.dataset.siteKey ?? '';

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!alertEl || !submitBtn) return;

    alertEl.hidden = true;
    submitBtn.disabled = true;

    const fd = new FormData(form);
    let body: Record<string, unknown> = {
      landingPageId,
      source: 'form',
      utm: parseUtm(),
      device: navigator.userAgent,
    };

    for (const [name, value] of fd.entries()) {
      if (typeof value !== 'string') continue;
      body = { ...body, ...mapFieldToBody(name, value.trim()) };
    }

    const email = body.email as string | undefined;
    const phone = body.phone as string | undefined;
    if (!email && !phone) {
      showAlert(alertEl, 'error', 'Please provide at least an email or phone number.');
      submitBtn.disabled = false;
      return;
    }

    const token = getTurnstileToken(siteKey);
    if (siteKey && !token) {
      showAlert(alertEl, 'error', 'Please complete the verification challenge.');
      submitBtn.disabled = false;
      return;
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      clientid: CLIENT_ID,
    };
    if (token) headers['X-Turnstile-Token'] = token;

    try {
      const res = await fetch(`${apiBase}/api/v1/public/leads`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      });
      const json = await res.json().catch(() => ({}));

      if (res.ok && json.code === 200) {
        showAlert(
          alertEl,
          'success',
          "Thank you! We'll reply within 24 hours."
        );
        form.reset();
        resetTurnstile(siteKey);
      } else {
        const msg =
          json.msg ?? json.message ?? 'Something went wrong. Please try again.';
        showAlert(alertEl, 'error', String(msg));
        resetTurnstile(siteKey);
      }
    } catch {
      showAlert(alertEl, 'error', 'Network error. Please try again later.');
      resetTurnstile(siteKey);
    } finally {
      submitBtn.disabled = false;
    }
  });
}
