-- Migration V14: Add default encryption method to SMTP config
-- This migration adds a field for the default email encryption method
-- This is used when no specific encryption method is configured for a recipient

-- Add default encryption method field to smtp_config
ALTER TABLE smtp_config 
ADD COLUMN IF NOT EXISTS default_encryption_method VARCHAR(20) DEFAULT 'NONE';

-- Set default to NONE for existing records
UPDATE smtp_config 
SET default_encryption_method = 'NONE' 
WHERE default_encryption_method IS NULL;

