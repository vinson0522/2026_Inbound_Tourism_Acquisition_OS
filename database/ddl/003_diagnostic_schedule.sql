-- EPIC-2 M3 FR-109: diagnostic_schedule incremental migration
BEGIN;

DO $$ BEGIN
    CREATE TYPE diagnostic_schedule_frequency AS ENUM ('WEEKLY', 'MONTHLY');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS diagnostic_schedule (
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

CREATE UNIQUE INDEX IF NOT EXISTS uq_diagnostic_schedule_project
    ON diagnostic_schedule(project_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_diagnostic_schedule_due
    ON diagnostic_schedule(next_run_at) WHERE deleted_at IS NULL AND enabled = true;

DO $$ BEGIN
    CREATE TRIGGER trg_diagnostic_schedule_updated_at
        BEFORE UPDATE ON diagnostic_schedule FOR EACH ROW EXECUTE FUNCTION set_updated_at();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

COMMIT;
