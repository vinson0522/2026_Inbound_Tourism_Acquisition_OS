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

COMMIT;
