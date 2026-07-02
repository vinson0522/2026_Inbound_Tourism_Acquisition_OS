-- =============================================================================
-- Demo seed data for local development / first demo script (PRD §20.5)
-- Run after 001_schema.sql
-- =============================================================================

BEGIN;

INSERT INTO tenant (name, plan_code, status, white_label_config)
VALUES ('Demo Travel Agency', 'growth_service', 'ACTIVE', '{"logo_url":"","brand_color":"#1677ff"}');

INSERT INTO user_account (tenant_id, name, email, role, status)
VALUES (1, 'Demo Admin', 'admin@demo-travel.com', 'TENANT_ADMIN', 'ACTIVE');

INSERT INTO subscription (tenant_id, plan_code, quota_json, used_json, period_start, period_end, status)
VALUES (
    1,
    'growth_service',
    '{"projects":5,"diagnostics_per_month":4,"keywords_per_month":500,"content_per_month":100,"landing_pages_per_month":20,"reports_per_month":8}',
    '{}',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '1 month',
    'ACTIVE'
);

INSERT INTO customer_project (
    tenant_id, name, brand_name, website, target_markets_json, languages_json, created_by
) VALUES (
    1,
    'First-Time China Private Tour',
    'Dragon Journey Travel',
    'https://demo-dragonjourney.com',
    '["US","UK","AU"]',
    '["en"]',
    1
);

INSERT INTO travel_product (tenant_id, project_id, name, destinations_json, days, price_range, highlights, created_by)
VALUES (
    1, 1,
    '10-Day First-Time China Private Tour',
    '["Beijing","Xi''an","Zhangjiajie","Shanghai"]',
    10,
    'USD 2500-4500',
    'Great Wall, Terracotta Warriors, Avatar mountains, Bund skyline',
    1
);

INSERT INTO competitor (tenant_id, project_id, name, website, created_by)
VALUES
    (1, 1, 'China Highlights', 'https://www.chinahighlights.com', 1),
    (1, 1, 'Trip.com', 'https://www.trip.com', 1),
    (1, 1, 'Klook', 'https://www.klook.com', 1);

INSERT INTO question_bank (tenant_id, project_id, market, language, stage, question, is_longtail, created_by)
VALUES
    (1, 1, 'US', 'en', 'planning', 'Please create a 10-day China itinerary including Beijing, Xi''an, Zhangjiajie and Shanghai.', FALSE, 1),
    (1, 1, 'US', 'en', 'trust', 'Can you recommend reliable China travel agencies for foreign tourists?', FALSE, 1),
    (1, 1, 'US', 'en', 'decision', 'How much does a private 10-day China tour usually cost?', FALSE, 1);

INSERT INTO keyword_opportunity (tenant_id, project_id, keyword, keyword_en, market, stage, score, channel, created_by)
VALUES (
    1, 1,
    'Chongqing cyberpunk city tour',
    'Chongqing cyberpunk city tour',
    'US', 'inspiration', 88.5, 'tiktok', 1
);

INSERT INTO knowledge_asset (tenant_id, project_id, type, title, content, vector_status, created_by)
VALUES (
    1, 1, 'DOCUMENT',
    'Demo Brand Overview',
    'Dragon Journey Travel specializes in private first-time China tours for English-speaking travelers. '
    || 'Our 10-day itineraries cover Beijing, Xi''an, Zhangjiajie and Shanghai with licensed guides, '
    || '4-star hotels, and transparent pricing in USD. We assist with visa guidance and 24/7 support.',
    'PENDING', 1
);

INSERT INTO platform_adapter (tenant_id, platform, version, dom_selectors_json, api_patterns_json, parse_rules_json, enabled, created_by)
VALUES (
    1,
    'perplexity',
    '1.0',
    '{"input": "textarea, [contenteditable=''true''], #ask-input", "submit": "button[type=submit], button[aria-label=''Submit'']"}',
    '{"chatApi": "/rest/sse/perplexity_ask", "sseApi": "/rest/sse/perplexity_ask"}',
    '{"citationsPath": "citations", "answerPath": "answer"}',
    TRUE,
    1
)
ON CONFLICT (tenant_id, platform, version) DO NOTHING;

INSERT INTO platform_adapter (tenant_id, platform, version, dom_selectors_json, api_patterns_json, parse_rules_json, enabled, created_by)
VALUES (
    1,
    'chatgpt',
    '1.0',
    '{"input": "#prompt-textarea", "submit": "button[data-testid=send-button]"}',
    '{"chatApi": "/backend-api/conversation", "sseApi": "/backend-api/conversation"}',
    '{"citationsPath": "message.metadata.citations", "answerPath": "message.content.parts"}',
    TRUE,
    1
)
ON CONFLICT (tenant_id, platform, version) DO NOTHING;

-- FR-106 report export smoke — runId=2 SUCCESS (test_diagnostic_report_export.py default)
INSERT INTO diagnostic_run (
    id, tenant_id, project_id, name, market, locale, region,
    probe_modes_json, models_json, sample_count, question_scope_json,
    status, geo_score, started_at, finished_at, created_by
) VALUES (
    2, 1, 1,
    'Demo GEO Report Export Smoke',
    'US', 'en-US', 'us-east',
    '["grounded-api"]'::jsonb,
    '["gemini"]'::jsonb,
    1,
    '{"mode":"all"}'::jsonb,
    'SUCCESS'::diagnostic_run_status,
    85.00,
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days',
    1
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    geo_score = EXCLUDED.geo_score,
    started_at = EXCLUDED.started_at,
    finished_at = EXCLUDED.finished_at,
    updated_at = NOW();

INSERT INTO diagnostic_result (
    tenant_id, run_id, question_id, platform, probe_mode, model,
    answer_text, mentioned_brands_json, competitors_json, citations_json,
    rank, sampled_at, created_by
)
SELECT
    1, 2, 1, 'gemini', 'grounded-api', 'gemini',
    'Dragon Journey Travel specializes in private first-time China tours for English-speaking travelers.',
    '["Dragon Journey Travel"]'::jsonb,
    '["China Highlights"]'::jsonb,
    '[{"url":"https://demo-dragonjourney.com","title":"Dragon Journey Travel","domain":"demo-dragonjourney.com","rank":1}]'::jsonb,
    2,
    NOW() - INTERVAL '2 days',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM diagnostic_result WHERE run_id = 2 AND deleted_at IS NULL
);

SELECT setval(
    pg_get_serial_sequence('diagnostic_run', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM diagnostic_run), 2)
);

COMMIT;
