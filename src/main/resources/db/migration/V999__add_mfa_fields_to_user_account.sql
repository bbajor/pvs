-- Agent 3: Add MFA fields to user_account table

ALTER TABLE user_account ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS mfa_secret VARCHAR(255);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS mfa_backup_codes VARCHAR(1000);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS mfa_setup_completed BOOLEAN DEFAULT FALSE;

-- Index for MFA-enabled users (performance)
CREATE INDEX IF NOT EXISTS idx_user_account_mfa_enabled ON user_account(mfa_enabled) WHERE mfa_enabled = TRUE;
