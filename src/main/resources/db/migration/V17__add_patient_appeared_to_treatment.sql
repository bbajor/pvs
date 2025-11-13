-- Add patient_appeared field to treatment table
-- This field indicates whether the patient appeared for the treatment appointment.
-- Used to distinguish between planned and actual treatments in reports and statistics.

ALTER TABLE treatment ADD COLUMN patient_appeared BOOLEAN;

-- Default existing treatments to true (patient appeared) for backward compatibility
UPDATE treatment SET patient_appeared = TRUE WHERE patient_appeared IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN treatment.patient_appeared IS 'Indicates whether the patient appeared for this treatment appointment. If false, this treatment should not be counted in "actual treatments" statistics.';

