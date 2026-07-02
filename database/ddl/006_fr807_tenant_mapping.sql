-- FR-807: business tenant ↔ RuoYi sys_tenant mapping + tenant B smoke data
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS ruoyi_tenant_id VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_ruoyi_tenant_id
    ON tenant(ruoyi_tenant_id) WHERE deleted_at IS NULL AND ruoyi_tenant_id IS NOT NULL;

UPDATE tenant SET ruoyi_tenant_id = '000000' WHERE id = 1 AND (ruoyi_tenant_id IS NULL OR ruoyi_tenant_id = '');

INSERT INTO tenant (id, name, plan_code, ruoyi_tenant_id, status, white_label_config)
VALUES (2, 'Beta Travel Co', 'trial', '000001', 'ACTIVE', '{}')
ON CONFLICT (id) DO UPDATE SET ruoyi_tenant_id = EXCLUDED.ruoyi_tenant_id, name = EXCLUDED.name;

INSERT INTO subscription (tenant_id, plan_code, quota_json, used_json, period_start, period_end, status)
SELECT 2, 'trial',
    '{"projects":5,"diagnostics_per_month":4,"keywords_per_month":500,"content_per_month":100,"landing_pages_per_month":20,"reports_per_month":8}',
    '{}', CURRENT_DATE, CURRENT_DATE + INTERVAL '1 month', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM subscription WHERE tenant_id = 2 AND status = 'ACTIVE');

INSERT INTO customer_project (
    tenant_id, name, brand_name, website, target_markets_json, languages_json, created_by
)
SELECT 2, 'Tenant B Sample Project', 'Beta Journeys', 'https://beta.example.com', '["US"]', '["en"]', 1
WHERE NOT EXISTS (SELECT 1 FROM customer_project WHERE tenant_id = 2 AND deleted_at IS NULL);

-- RuoYi sys_tenant + user for tenant B (password same as admin: admin123)
INSERT INTO sys_tenant (id, tenant_id, contact_user_name, contact_phone, company_name, license_number, address, intro, domain, remark, package_id, expire_time, account_count, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
SELECT 2, '000001', 'Beta Admin', '15888888889', 'Beta Travel Co', NULL, NULL, 'FR-807 tenant B', NULL, NULL, NULL, NULL, -1, '0', '0', 103, 1, NOW(), NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_id = '000001');

INSERT INTO sys_user (user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark)
SELECT 100, '000001', 103, 'tenantb', 'Tenant B Admin', 'sys_user', 'tenantb@demo-travel.com', '15888888889', '0', NULL,
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', NOW(), 103, 1, NOW(), NULL, NULL, 'FR-807 tenant B'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'tenantb' AND tenant_id = '000001');

INSERT INTO sys_user_role (user_id, role_id)
SELECT 100, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 100 AND role_id = 1);
