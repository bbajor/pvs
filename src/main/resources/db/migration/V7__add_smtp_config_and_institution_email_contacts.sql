-- Migration V7: Add SMTP configuration and institution email contacts with OpenPGP support
-- This migration adds:
-- 1. SMTP configuration table for storing mail server settings
-- 2. Institution email contacts table for storing email addresses with OpenPGP public keys

-- SMTP Configuration table
-- Only one configuration should exist (singleton pattern via application logic)
CREATE TABLE IF NOT EXISTS smtp_config (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL DEFAULT 587,
    username VARCHAR(255),
    password VARCHAR(500), -- Encrypted password
    from_address VARCHAR(255),
    use_tls BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE
);

-- Institution Email Contact table
-- Stores email contacts for institutions with their OpenPGP public keys
CREATE TABLE IF NOT EXISTS institution_email_contact (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    openpgp_public_key TEXT, -- ASCII-armored OpenPGP public key
    key_id VARCHAR(16), -- Key ID (16 hex characters)
    key_fingerprint VARCHAR(40), -- Key fingerprint (40 hex characters)
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(1000),
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_institution_email_contact_institution ON institution_email_contact(institution_id);
CREATE INDEX IF NOT EXISTS idx_institution_email_contact_email ON institution_email_contact(email);
CREATE INDEX IF NOT EXISTS idx_institution_email_contact_active ON institution_email_contact(active);

