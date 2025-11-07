-- Add MFA fields to user_account table
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_change_required BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS initial_password_set BOOLEAN DEFAULT FALSE;
