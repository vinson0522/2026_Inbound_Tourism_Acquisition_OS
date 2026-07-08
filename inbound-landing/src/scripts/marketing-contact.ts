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
  const w = window as Window & { turnstile?: { getResponse?: () => string } };
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

export function initMarketingContactForm(): void {
  const form = document.getElementById('marketing-contact-form') as HTMLFormElement | null;
  if (!form) return;

  const alertEl = document.getElementById('mc-alert') as HTMLElement | null;
  const submitBtn = document.getElementById('mc-submit') as HTMLButtonElement | null;
  const apiBase = (form.dataset.apiBase ?? 'http://localhost:8080').replace(/\/+$/, '');
  const siteKey = form.dataset.siteKey ?? '';
  const msg = {
    required: form.dataset.msgRequired ?? 'Please tell us your name.',
    contact: form.dataset.msgContact ?? 'Please provide at least an email or phone number.',
    captcha: form.dataset.msgCaptcha ?? 'Please complete the verification challenge.',
    network: form.dataset.msgNetwork ?? 'Network error. Please try again later.',
    generic: form.dataset.msgGeneric ?? 'Something went wrong. Please try again.',
    success: form.dataset.msgSuccess ?? 'Thank you! We will reply within 24 hours.',
    submit: form.dataset.msgSubmit ?? 'Send message',
    submitting: form.dataset.msgSubmitting ?? 'Sending…',
  };

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!alertEl || !submitBtn) return;

    alertEl.hidden = true;

    const fd = new FormData(form);
    const name = String(fd.get('name') ?? '').trim();
    const email = String(fd.get('email') ?? '').trim();
    const phone = String(fd.get('phone') ?? '').trim();
    const company = String(fd.get('company') ?? '').trim();
    const message = String(fd.get('message') ?? '').trim();

    if (!name) {
      showAlert(alertEl, 'error', msg.required);
      return;
    }
    if (!email && !phone) {
      showAlert(alertEl, 'error', msg.contact);
      return;
    }

    const token = getTurnstileToken(siteKey);
    if (siteKey && !token) {
      showAlert(alertEl, 'error', msg.captcha);
      return;
    }

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      clientid: CLIENT_ID,
    };
    if (token) headers['X-Turnstile-Token'] = token;

    const body = {
      name,
      email,
      phone,
      company,
      message,
      source: 'marketing',
      utm: parseUtm(),
      device: navigator.userAgent,
    };

    submitBtn.disabled = true;
    submitBtn.textContent = msg.submitting;

    try {
      const res = await fetch(`${apiBase}/api/v1/public/marketing-contact`, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      });
      const json = await res.json().catch(() => ({}) as Record<string, unknown>);

      if (res.ok && json.code === 200) {
        showAlert(alertEl, 'success', msg.success);
        form.reset();
        resetTurnstile(siteKey);
      } else {
        const serverMsg = (json.msg ?? json.message) as string | undefined;
        showAlert(alertEl, 'error', serverMsg ?? msg.generic);
        resetTurnstile(siteKey);
      }
    } catch {
      showAlert(alertEl, 'error', msg.network);
      resetTurnstile(siteKey);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = msg.submit;
    }
  });
}
