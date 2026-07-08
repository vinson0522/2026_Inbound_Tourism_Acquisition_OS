-- B-27: repeatable smoke quota reset (demo tenant 1)
--
-- smoke test_projects_api.py / test_diagnostic_e2e.py POST create resources and
-- consume subscription.used_json via QuotaServiceImpl.checkAndConsume. After a few
-- runs used >= limit -> HTTP 402 code=40201 and smoke drops to 8/10.
--
-- This script ONLY resets the demo tenant's consumption counters + rolls the billing
-- period to the current month. It does NOT change billing semantics (QuotaServiceImpl).
-- Idempotent: safe to run before every smoke regression pass.

UPDATE subscription
SET used_json    = '{}'::jsonb,
    period_start = CURRENT_DATE,
    period_end   = CURRENT_DATE + INTERVAL '1 month',
    updated_at   = now()
WHERE tenant_id = 1
  AND status = 'ACTIVE'::subscription_status;
