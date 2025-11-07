-- Add MFA recovery email and reset token fields to user_account table
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS recovery_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recovery_email_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mfa_reset_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mfa_reset_token_expiry TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_account_mfa_reset_token ON user_account(mfa_reset_token) WHERE mfa_reset_token IS NOT NULL;

