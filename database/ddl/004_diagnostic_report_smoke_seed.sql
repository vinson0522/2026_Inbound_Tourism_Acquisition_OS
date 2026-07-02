-- FR-106 smoke: ensure diagnostic_run id=2 is exportable (SUCCESS + geo_score + results)
-- Idempotent — safe to re-run on dev DBs where E2E left run#2 stuck in RUNNING.
BEGIN;

UPDATE diagnostic_run
SET status = 'SUCCESS'::diagnostic_run_status,
    geo_score = COALESCE(geo_score, 85.00),
    started_at = COALESCE(started_at, NOW() - INTERVAL '1 day'),
    finished_at = COALESCE(finished_at, NOW() - INTERVAL '1 day'),
    updated_at = NOW()
WHERE id = 2
  AND deleted_at IS NULL;

UPDATE probe_task
SET status = 'SUCCESS'::probe_task_status,
    finished_at = COALESCE(finished_at, NOW() - INTERVAL '1 day'),
    updated_at = NOW()
WHERE run_id = 2
  AND deleted_at IS NULL
  AND status NOT IN ('SUCCESS'::probe_task_status, 'FAILED'::probe_task_status);

INSERT INTO diagnostic_result (
    tenant_id, run_id, question_id, platform, probe_mode, model,
    answer_text, mentioned_brands_json, competitors_json, citations_json,
    rank, sampled_at, created_by
)
SELECT
    1, 2, 1, 'gemini', 'grounded-api', 'gemini',
    'Dragon Journey Travel offers private first-time China tours for English-speaking travelers, '
    || 'covering Beijing, Xi''an, Zhangjiajie and Shanghai with licensed guides.',
    '["Dragon Journey Travel"]'::jsonb,
    '["China Highlights", "Trip.com"]'::jsonb,
    '[{"url":"https://demo-dragonjourney.com","title":"Dragon Journey Travel","domain":"demo-dragonjourney.com","rank":1}]'::jsonb,
    2,
    NOW() - INTERVAL '1 day',
    1
WHERE EXISTS (SELECT 1 FROM diagnostic_run WHERE id = 2 AND deleted_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM diagnostic_result WHERE run_id = 2 AND deleted_at IS NULL);

SELECT setval(
    pg_get_serial_sequence('diagnostic_run', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM diagnostic_run), 2)
);

COMMIT;
