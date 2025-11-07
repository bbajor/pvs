-- Migration V10: Add security_method to smtp_config table
-- This migration adds a security_method column to support different SMTP encryption methods
-- (NONE, STARTTLS, SSL_TLS) instead of just a boolean use_tls flag

-- Add security_method column
ALTER TABLE smtp_config 
ADD COLUMN IF NOT EXISTS security_method VARCHAR(20);

-- Migrate existing data: convert use_tls to security_method
-- If use_tls is true, set to STARTTLS (default for port 587)
-- If use_tls is false, set to NONE
UPDATE smtp_config 
SET security_method = CASE 
    WHEN use_tls = TRUE THEN 'STARTTLS'
    ELSE 'NONE'
END
WHERE security_method IS NULL;

-- Set default for new records
ALTER TABLE smtp_config 
ALTER COLUMN security_method SET DEFAULT 'STARTTLS';

