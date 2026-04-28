-- Add PIN fields to user_account table for passwordless login
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pin_reset_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pin_reset_token_expiry TIMESTAMP;

