-- Add KBV metadata columns to diagnosis table for validation and synchronization
ALTER TABLE diagnosis
    ADD COLUMN IF NOT EXISTS kbv_quarter VARCHAR(20),
    ADD COLUMN IF NOT EXISTS kbv_valid_from DATE,
    ADD COLUMN IF NOT EXISTS kbv_valid_to DATE,
    ADD COLUMN IF NOT EXISTS validated_against_kbv BOOLEAN DEFAULT FALSE;

-- Create index for KBV quarter lookups
CREATE INDEX IF NOT EXISTS idx_diagnosis_kbv_quarter ON diagnosis(kbv_quarter);

-- Create index for validated diagnoses
CREATE INDEX IF NOT EXISTS idx_diagnosis_validated_kbv ON diagnosis(validated_against_kbv) WHERE validated_against_kbv = TRUE;
