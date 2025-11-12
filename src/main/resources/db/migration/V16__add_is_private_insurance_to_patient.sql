-- Add is_private_insurance column to patient table
ALTER TABLE patient ADD COLUMN IF NOT EXISTS is_private_insurance BOOLEAN NOT NULL DEFAULT FALSE;

