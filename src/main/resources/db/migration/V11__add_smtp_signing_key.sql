-- Migration V11: Add OpenPGP signing key to SMTP config
-- This migration adds fields for storing the sender's private key for email signing
-- The private key is encrypted before storage

-- Add private key and passphrase fields to smtp_config
ALTER TABLE smtp_config 
ADD COLUMN IF NOT EXISTS openpgp_private_key TEXT; -- Encrypted private key for signing

ALTER TABLE smtp_config 
ADD COLUMN IF NOT EXISTS openpgp_private_key_passphrase VARCHAR(500); -- Encrypted passphrase for private key

