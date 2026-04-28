-- Add treatment_status field to treatment table
-- This field indicates the status of the treatment after review.
-- Replaces the simple patient_appeared boolean with more detailed status information.

ALTER TABLE treatment ADD COLUMN treatment_status VARCHAR(50);

-- Migrate existing data: Set treatment_status based on patient_appeared
-- If patient_appeared is true, set to PATIENT_APPEARED_SUCCESSFUL
-- If patient_appeared is false, set to PATIENT_NO_SHOW
-- If patient_appeared is null, set to PATIENT_APPEARED_SUCCESSFUL (default)
UPDATE treatment 
SET treatment_status = CASE 
    WHEN patient_appeared = TRUE THEN 'PATIENT_APPEARED_SUCCESSFUL'
    WHEN patient_appeared = FALSE THEN 'PATIENT_NO_SHOW'
    ELSE 'PATIENT_APPEARED_SUCCESSFUL'
END
WHERE treatment_status IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN treatment.treatment_status IS 'Status der Behandlung nach der Überprüfung. Definiert verschiedene Zustände, die nach einer Behandlung auftreten können.';

