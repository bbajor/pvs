-- Migration V13: Add OpenPGP public key to SMTP config
-- This migration adds a field for storing the sender's public key separately
-- The public key is used for Autocrypt headers in signed emails
-- If not provided, it will be extracted from the private key at runtime

-- Add public key field to smtp_config
ALTER TABLE smtp_config 
ADD COLUMN IF NOT EXISTS openpgp_public_key TEXT; -- Public key for Autocrypt header (not encrypted)

