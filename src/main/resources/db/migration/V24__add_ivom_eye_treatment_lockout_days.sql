-- Add IVOM-Planer: Zeitsperre zwischen der Behandlung beider Augen
-- Verhindert, dass beide Augen innerhalb eines konfigurierbaren Zeitraums behandelt werden
ALTER TABLE institution
    ADD COLUMN IF NOT EXISTS ivom_eye_treatment_lockout_days INTEGER DEFAULT 0;

COMMENT ON COLUMN institution.ivom_eye_treatment_lockout_days IS 'Zeitsperre zwischen der Behandlung beider Augen (in Tagen). Verhindert, dass beide Augen innerhalb dieses Zeitraums behandelt werden. Standard: 0 (keine Sperre).';

