-- Add soft delete support for health_insurance
-- Existing data stays active by default

ALTER TABLE health_insurance
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_health_insurance_active
    ON health_insurance(active);
