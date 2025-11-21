-- Add finished_date column to treatment_plan table
ALTER TABLE treatment_plan ADD COLUMN finished_date DATE;

-- Add index for filtering by finished status
CREATE INDEX idx_treatment_plan_finished_date ON treatment_plan(finished_date);

