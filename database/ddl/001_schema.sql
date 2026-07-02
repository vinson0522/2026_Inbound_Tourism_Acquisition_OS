-- =============================================================================
-- Inbound Tourism Acquisition OS — PostgreSQL + pgvector Full DDL
-- PRD V2.0 §11 数据模型 | 2026-06-23
-- =============================================================================
-- 运行前提: PostgreSQL 16+, pgvector 扩展
-- 用法: psql -U postgres -d inbound_growth -f 001_schema.sql
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- Extensions
-- ---------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- Enums
-- ---------------------------------------------------------------------------
CREATE TYPE entity_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'ARCHIVED');

CREATE TYPE user_role AS ENUM (
    'SUPER_ADMIN',
    'TENANT_ADMIN',
    'PROJECT_ADMIN',
    'CONTENT_OPERATOR',
    'SALES',
    'READONLY'
);

CREATE TYPE vector_index_status AS ENUM ('PENDING', 'INDEXING', 'READY', 'FAILED');

CREATE TYPE diagnostic_run_status AS ENUM (
    'PENDING',
    'RUNNING',
    'SUCCESS',
    'PARTIAL_FAILED',
    'FAILED',
    'CANCELLED'
);

CREATE TYPE diagnostic_schedule_frequency AS ENUM ('WEEKLY', 'MONTHLY');

CREATE TYPE probe_task_status AS ENUM ('PENDING', 'DISPATCHED', 'RUNNING', 'SUCCESS', 'FAILED', 'RETRY');

CREATE TYPE content_task_status AS ENUM ('DRAFT', 'GENERATING', 'GENERATED', 'ADOPTED', 'DISCARDED', 'FAILED');

CREATE TYPE landing_page_status AS ENUM ('DRAFT', 'EDITING', 'READY', 'PUBLISHED', 'ARCHIVED');

CREATE TYPE lead_status AS ENUM ('NEW', 'FOLLOWING', 'QUOTED', 'WON', 'LOST');

CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'EXPIRED', 'CANCELLED', 'TRIAL');

CREATE TYPE report_type AS ENUM ('DIAGNOSTIC', 'WEEKLY', 'MONTHLY', 'CUSTOM');

CREATE TYPE template_type AS ENUM ('QUESTION', 'CONTENT', 'REPORT', 'LANDING_PAGE');

CREATE TYPE knowledge_asset_type AS ENUM ('DOCUMENT', 'FAQ', 'ROUTE', 'POLICY', 'WEB_PAGE', 'OTHER');

CREATE TYPE material_asset_type AS ENUM ('IMAGE', 'VIDEO', 'AUDIO', 'OTHER');

-- ---------------------------------------------------------------------------
-- Utility: auto-update updated_at
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------------
-- Domain 1: Tenant & Auth
-- ---------------------------------------------------------------------------

CREATE TABLE tenant (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    plan_code           VARCHAR(64)  NOT NULL DEFAULT 'trial',
    ruoyi_tenant_id     VARCHAR(20),
    status              entity_status NOT NULL DEFAULT 'ACTIVE',
    white_label_config  JSONB NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE TRIGGER trg_tenant_updated_at
    BEFORE UPDATE ON tenant FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE UNIQUE INDEX uq_tenant_ruoyi_tenant_id ON tenant(ruoyi_tenant_id) WHERE deleted_at IS NULL AND ruoyi_tenant_id IS NOT NULL;

CREATE TABLE user_account (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    phone           VARCHAR(32),
    password_hash   VARCHAR(255),
    role            user_role NOT NULL DEFAULT 'READONLY',
    status          entity_status NOT NULL DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT,
    CONSTRAINT uq_user_account_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_user_account_tenant ON user_account(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_user_account_updated_at
    BEFORE UPDATE ON user_account FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE subscription (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    plan_code       VARCHAR(64) NOT NULL,
    quota_json      JSONB NOT NULL DEFAULT '{}',
    used_json       JSONB NOT NULL DEFAULT '{}',
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    status          subscription_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_subscription_tenant ON subscription(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_subscription_updated_at
    BEFORE UPDATE ON subscription FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE model_config (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    provider            VARCHAR(64) NOT NULL,
    api_key_encrypted   TEXT NOT NULL,
    rate_limit          INT NOT NULL DEFAULT 60,
    retry_policy_json   JSONB NOT NULL DEFAULT '{}',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_model_config_tenant ON model_config(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_model_config_updated_at
    BEFORE UPDATE ON model_config FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    type            template_type NOT NULL,
    name            VARCHAR(200) NOT NULL,
    config_json     JSONB NOT NULL DEFAULT '{}',
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_template_tenant_type ON template(tenant_id, type) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_template_updated_at
    BEFORE UPDATE ON template FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE scoring_rule (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    metric_weights_json JSONB NOT NULL DEFAULT '{}',
    version             INT NOT NULL DEFAULT 1,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_scoring_rule_tenant ON scoring_rule(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_scoring_rule_updated_at
    BEFORE UPDATE ON scoring_rule FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    user_id         BIGINT REFERENCES user_account(id),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     BIGINT,
    detail_json     JSONB NOT NULL DEFAULT '{}',
    ip              INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_tenant_created ON audit_log(tenant_id, created_at DESC);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);

-- ---------------------------------------------------------------------------
-- Domain 2: Customer Project & Knowledge
-- ---------------------------------------------------------------------------

CREATE TABLE customer_project (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    name            VARCHAR(200) NOT NULL,
    brand_name      VARCHAR(200) NOT NULL,
    website         VARCHAR(500),
    industry        VARCHAR(64) NOT NULL DEFAULT 'inbound_tourism',
    target_markets_json JSONB NOT NULL DEFAULT '[]',
    languages_json  JSONB NOT NULL DEFAULT '["en"]',
    status          entity_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT REFERENCES user_account(id)
);

CREATE INDEX idx_customer_project_tenant ON customer_project(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_customer_project_updated_at
    BEFORE UPDATE ON customer_project FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE travel_product (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    name            VARCHAR(300) NOT NULL,
    destinations_json JSONB NOT NULL DEFAULT '[]',
    days            INT,
    price_range     VARCHAR(100),
    suitable_for    TEXT,
    highlights      TEXT,
    inclusions      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_travel_product_project ON travel_product(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_travel_product_tenant ON travel_product(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_travel_product_updated_at
    BEFORE UPDATE ON travel_product FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE competitor (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    name            VARCHAR(200) NOT NULL,
    website         VARCHAR(500),
    social_links_json JSONB NOT NULL DEFAULT '{}',
    main_products   TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_competitor_project ON competitor(project_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_competitor_updated_at
    BEFORE UPDATE ON competitor FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE knowledge_asset (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    type            knowledge_asset_type NOT NULL DEFAULT 'DOCUMENT',
    title           VARCHAR(500) NOT NULL,
    content         TEXT,
    file_url        VARCHAR(1000),
    tags_json       JSONB NOT NULL DEFAULT '[]',
    vector_status   vector_index_status NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_knowledge_asset_project ON knowledge_asset(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_knowledge_asset_vector_status ON knowledge_asset(vector_status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_knowledge_asset_updated_at
    BEFORE UPDATE ON knowledge_asset FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE knowledge_chunk (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    asset_id        BIGINT NOT NULL REFERENCES knowledge_asset(id) ON DELETE CASCADE,
    chunk_index     INT NOT NULL DEFAULT 0,
    chunk_text      TEXT NOT NULL,
    token_count     INT,
    metadata_json   JSONB NOT NULL DEFAULT '{}',
    embedding       vector(1536),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT,
    CONSTRAINT uq_knowledge_chunk_asset_index UNIQUE (asset_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunk_project ON knowledge_chunk(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_knowledge_chunk_tenant ON knowledge_chunk(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_knowledge_chunk_embedding
    ON knowledge_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ---------------------------------------------------------------------------
-- Domain 3: GEO Diagnostic & Probe
-- ---------------------------------------------------------------------------

CREATE TABLE question_bank (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    market          VARCHAR(16) NOT NULL,
    language        VARCHAR(16) NOT NULL DEFAULT 'en',
    stage           VARCHAR(32) NOT NULL,
    question        TEXT NOT NULL,
    is_longtail     BOOLEAN NOT NULL DEFAULT FALSE,
    status          entity_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_question_bank_project ON question_bank(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_question_bank_stage ON question_bank(project_id, stage) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_question_bank_updated_at
    BEFORE UPDATE ON question_bank FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE probe_node (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    node_key            VARCHAR(128) NOT NULL,
    region              VARCHAR(64) NOT NULL,
    platforms_json      JSONB NOT NULL DEFAULT '[]',
    extension_version   VARCHAR(32),
    status              entity_status NOT NULL DEFAULT 'ACTIVE',
    rate_limit_json     JSONB NOT NULL DEFAULT '{}',
    last_heartbeat_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT,
    CONSTRAINT uq_probe_node_tenant_key UNIQUE (tenant_id, node_key)
);

CREATE INDEX idx_probe_node_tenant ON probe_node(tenant_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_probe_node_updated_at
    BEFORE UPDATE ON probe_node FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE platform_adapter (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT REFERENCES tenant(id),
    platform            VARCHAR(64) NOT NULL,
    version             VARCHAR(32) NOT NULL DEFAULT '1.0',
    dom_selectors_json  JSONB NOT NULL DEFAULT '{}',
    api_patterns_json   JSONB NOT NULL DEFAULT '{}',
    parse_rules_json    JSONB NOT NULL DEFAULT '{}',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT,
    CONSTRAINT uq_platform_adapter UNIQUE (tenant_id, platform, version)
);

CREATE TRIGGER trg_platform_adapter_updated_at
    BEFORE UPDATE ON platform_adapter FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE diagnostic_run (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    project_id          BIGINT NOT NULL REFERENCES customer_project(id),
    name                VARCHAR(200) NOT NULL,
    market              VARCHAR(16) NOT NULL,
    locale              VARCHAR(16) NOT NULL DEFAULT 'en-US',
    region              VARCHAR(64),
    probe_modes_json    JSONB NOT NULL DEFAULT '["grounded-api"]',
    calibration_ratio   NUMERIC(5, 4) DEFAULT 0,
    models_json         JSONB NOT NULL DEFAULT '[]',
    sample_count        INT NOT NULL DEFAULT 0,
    question_scope_json JSONB NOT NULL DEFAULT '{}',
    status              diagnostic_run_status NOT NULL DEFAULT 'PENDING',
    geo_score           NUMERIC(6, 2),
    started_at          TIMESTAMPTZ,
    finished_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_diagnostic_run_project ON diagnostic_run(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_diagnostic_run_status ON diagnostic_run(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_diagnostic_run_updated_at
    BEFORE UPDATE ON diagnostic_run FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE diagnostic_schedule (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    project_id          BIGINT NOT NULL REFERENCES customer_project(id),
    frequency           diagnostic_schedule_frequency NOT NULL DEFAULT 'WEEKLY',
    enabled             BOOLEAN NOT NULL DEFAULT false,
    market              VARCHAR(16) NOT NULL DEFAULT 'US',
    locale              VARCHAR(16) NOT NULL DEFAULT 'en-US',
    region              VARCHAR(64),
    probe_modes_json    JSONB NOT NULL DEFAULT '["grounded-api"]',
    models_json         JSONB NOT NULL DEFAULT '["gemini"]',
    sample_count        INT NOT NULL DEFAULT 3,
    question_scope_json JSONB NOT NULL DEFAULT '{"mode":"all"}',
    calibration_ratio   NUMERIC(5, 4) NOT NULL DEFAULT 0,
    next_run_at         TIMESTAMPTZ,
    last_run_id         BIGINT REFERENCES diagnostic_run(id),
    last_triggered_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_diagnostic_schedule_project ON diagnostic_schedule(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_diagnostic_schedule_due ON diagnostic_schedule(next_run_at)
    WHERE deleted_at IS NULL AND enabled = true;

CREATE TRIGGER trg_diagnostic_schedule_updated_at
    BEFORE UPDATE ON diagnostic_schedule FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE diagnostic_result (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    run_id              BIGINT NOT NULL REFERENCES diagnostic_run(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES question_bank(id),
    platform            VARCHAR(64) NOT NULL,
    probe_mode          VARCHAR(32) NOT NULL DEFAULT 'grounded-api',
    probe_node_id       BIGINT REFERENCES probe_node(id),
    model               VARCHAR(64),
    answer_text         TEXT,
    mentioned_brands_json JSONB NOT NULL DEFAULT '[]',
    competitors_json    JSONB NOT NULL DEFAULT '[]',
    links_json          JSONB NOT NULL DEFAULT '[]',
    citations_json      JSONB NOT NULL DEFAULT '[]',
    capture_method      VARCHAR(32),
    raw_response_json   JSONB,
    screenshot_url      VARCHAR(1000),
    rank                INT,
    score_json          JSONB NOT NULL DEFAULT '{}',
    human_corrected     BOOLEAN NOT NULL DEFAULT FALSE,
    sampled_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_diagnostic_result_run ON diagnostic_result(run_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_diagnostic_result_question ON diagnostic_result(question_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_diagnostic_result_updated_at
    BEFORE UPDATE ON diagnostic_result FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE probe_task (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    run_id          BIGINT NOT NULL REFERENCES diagnostic_run(id) ON DELETE CASCADE,
    question_id     BIGINT NOT NULL REFERENCES question_bank(id),
    platform        VARCHAR(64) NOT NULL,
    probe_mode      VARCHAR(32) NOT NULL DEFAULT 'grounded-api',
    probe_node_id   BIGINT REFERENCES probe_node(id),
    status          probe_task_status NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    error_message   TEXT,
    dispatched_at   TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_probe_task_run ON probe_task(run_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_probe_task_status ON probe_task(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_probe_task_updated_at
    BEFORE UPDATE ON probe_task FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Domain 4: Keywords → Content → Landing Page
-- ---------------------------------------------------------------------------

CREATE TABLE keyword_opportunity (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    keyword         VARCHAR(500) NOT NULL,
    keyword_en      VARCHAR(500),
    keyword_cn      VARCHAR(500),
    intent          VARCHAR(64),
    market          VARCHAR(16) NOT NULL,
    stage           VARCHAR(32),
    score           NUMERIC(6, 2),
    score_detail_json JSONB NOT NULL DEFAULT '{}',
    channel         VARCHAR(64),
    source_json     JSONB NOT NULL DEFAULT '{}',
    status          entity_status NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_keyword_opportunity_project ON keyword_opportunity(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_keyword_opportunity_score ON keyword_opportunity(project_id, score DESC) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_keyword_opportunity_updated_at
    BEFORE UPDATE ON keyword_opportunity FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE content_task (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    keyword_id      BIGINT REFERENCES keyword_opportunity(id),
    platform        VARCHAR(64) NOT NULL,
    format          VARCHAR(64) NOT NULL DEFAULT 'short_video',
    duration        INT,
    tone            VARCHAR(64),
    language        VARCHAR(16) NOT NULL DEFAULT 'en',
    target_market   VARCHAR(16),
    status          content_task_status NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_content_task_project ON content_task(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_content_task_status ON content_task(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_content_task_updated_at
    BEFORE UPDATE ON content_task FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE generated_content (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL REFERENCES tenant(id),
    task_id                 BIGINT NOT NULL REFERENCES content_task(id) ON DELETE CASCADE,
    title                   VARCHAR(500),
    hook                    TEXT,
    target_audience         TEXT,
    script                  TEXT,
    storyboard_json         JSONB NOT NULL DEFAULT '[]',
    voiceover               TEXT,
    on_screen_text          TEXT,
    hashtags                TEXT,
    cta                     TEXT,
    landing_page_suggestion TEXT,
    needs_human_review      BOOLEAN NOT NULL DEFAULT TRUE,
    version                 INT NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ,
    created_by              BIGINT
);

CREATE INDEX idx_generated_content_task ON generated_content(task_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_generated_content_updated_at
    BEFORE UPDATE ON generated_content FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE landing_page (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    keyword_id      BIGINT REFERENCES keyword_opportunity(id),
    template_type   VARCHAR(64) NOT NULL DEFAULT 'default',
    title           VARCHAR(500) NOT NULL,
    slug            VARCHAR(200) NOT NULL,
    content_json    JSONB NOT NULL DEFAULT '{}',
    seo_meta_json   JSONB NOT NULL DEFAULT '{}',
    form_config_json JSONB NOT NULL DEFAULT '{}',
    whatsapp_link   VARCHAR(500),
    status          landing_page_status NOT NULL DEFAULT 'DRAFT',
    published_url   VARCHAR(1000),
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT,
    CONSTRAINT uq_landing_page_project_slug UNIQUE (project_id, slug)
);

CREATE INDEX idx_landing_page_project ON landing_page(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_landing_page_status ON landing_page(status) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_landing_page_updated_at
    BEFORE UPDATE ON landing_page FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE content_plan (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    week            VARCHAR(16) NOT NULL,
    platform        VARCHAR(64) NOT NULL,
    content_id      BIGINT REFERENCES generated_content(id),
    keyword_id      BIGINT REFERENCES keyword_opportunity(id),
    landing_page_id BIGINT REFERENCES landing_page(id),
    status          entity_status NOT NULL DEFAULT 'ACTIVE',
    publish_date    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_content_plan_project_week ON content_plan(project_id, week) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_content_plan_updated_at
    BEFORE UPDATE ON content_plan FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE material_asset (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    project_id          BIGINT NOT NULL REFERENCES customer_project(id),
    type                material_asset_type NOT NULL DEFAULT 'IMAGE',
    url                 VARCHAR(1000) NOT NULL,
    thumbnail_url       VARCHAR(1000),
    tags_json           JSONB NOT NULL DEFAULT '[]',
    copyright_status    VARCHAR(32) NOT NULL DEFAULT 'unknown',
    source              VARCHAR(200),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_material_asset_project ON material_asset(project_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_material_asset_updated_at
    BEFORE UPDATE ON material_asset FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE video_breakdown (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenant(id),
    project_id          BIGINT NOT NULL REFERENCES customer_project(id),
    source_url          VARCHAR(1000) NOT NULL,
    frames_json         JSONB NOT NULL DEFAULT '[]',
    dimensions_json     JSONB NOT NULL DEFAULT '{}',
    reusable_structure  TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    created_by          BIGINT
);

CREATE INDEX idx_video_breakdown_project ON video_breakdown(project_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_video_breakdown_updated_at
    BEFORE UPDATE ON video_breakdown FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Domain 5: Leads & Conversion
-- ---------------------------------------------------------------------------

CREATE TABLE lead (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    landing_page_id BIGINT REFERENCES landing_page(id),
    keyword_id      BIGINT REFERENCES keyword_opportunity(id),
    name            VARCHAR(200),
    email           VARCHAR(255),
    phone           VARCHAR(64),
    travel_date     DATE,
    party_size      INT,
    budget          VARCHAR(100),
    message         TEXT,
    source          VARCHAR(64),
    utm_json        JSONB NOT NULL DEFAULT '{}',
    device          VARCHAR(200),
    status          lead_status NOT NULL DEFAULT 'NEW',
    assignee_id     BIGINT REFERENCES user_account(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_lead_project ON lead(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_lead_status ON lead(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_lead_assignee ON lead(assignee_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_lead_updated_at
    BEFORE UPDATE ON lead FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE lead_followup (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    lead_id         BIGINT NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    channel         VARCHAR(64),
    suggestion      TEXT,
    operator_id     BIGINT REFERENCES user_account(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_lead_followup_lead ON lead_followup(lead_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_lead_followup_updated_at
    BEFORE UPDATE ON lead_followup FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- EPIC-7 M3 FR-602: WhatsApp / channel click beacon (weak link via landing_page_id)
CREATE TABLE lead_channel_event (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    landing_page_id BIGINT REFERENCES landing_page(id),
    event_type      VARCHAR(64) NOT NULL,
    utm_json        JSONB NOT NULL DEFAULT '{}',
    device          VARCHAR(200),
    ip_hash         VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lead_channel_event_page ON lead_channel_event(project_id, landing_page_id, event_type);

-- ---------------------------------------------------------------------------
-- Domain 6: Reports
-- ---------------------------------------------------------------------------

CREATE TABLE report (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenant(id),
    project_id      BIGINT NOT NULL REFERENCES customer_project(id),
    type            report_type NOT NULL,
    period          VARCHAR(32),
    file_url        VARCHAR(1000),
    summary         TEXT,
    template_id     BIGINT REFERENCES template(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    created_by      BIGINT
);

CREATE INDEX idx_report_project ON report(project_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_report_type ON report(project_id, type) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_report_updated_at
    BEFORE UPDATE ON report FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;

-- ---------------------------------------------------------------------------
-- Post-init notes (run manually after bulk insert into knowledge_chunk):
--   ANALYZE knowledge_chunk;
--   REINDEX INDEX idx_knowledge_chunk_embedding;  -- optional after >10k rows
-- ---------------------------------------------------------------------------
