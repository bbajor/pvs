-- Create institution_settings table to prepare for database-per-tenant architecture
-- Each institution gets exactly one settings row with tenant-specific configuration.

CREATE TABLE institution_settings (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(255),
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Berlin',
    locale VARCHAR(20) NOT NULL DEFAULT 'de-DE',
    demo_mode BOOLEAN NOT NULL DEFAULT FALSE,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    smtp_config_json TEXT,
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_institution_settings_institution FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE
);

CREATE INDEX idx_institution_settings_demo_mode ON institution_settings(demo_mode);
CREATE INDEX idx_institution_settings_onboarding ON institution_settings(onboarding_completed);

-- Seed settings for existing institutions using current master data.
INSERT INTO institution_settings (institution_id, display_name, legal_name, contact_email, contact_phone)
SELECT id, institution_name, company_name, email, phone
FROM institution;

-- Ensure updated_at reflects initial creation time.
UPDATE institution_settings SET updated_at = created_at;
