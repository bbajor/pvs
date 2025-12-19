-- Add findings and visual acuity fields to TreatmentPlan
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS subretinal_fluid BOOLEAN;
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS intraretinal_fluid_increase BOOLEAN;
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS serous_rpe_detachment_increase BOOLEAN;
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS new_retinal_hemorrhage BOOLEAN;
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS visual_acuity_initial_left VARCHAR(255);
ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS visual_acuity_initial_right VARCHAR(255);

-- Add findings and visual acuity fields to Treatment
ALTER TABLE treatment ADD COLUMN IF NOT EXISTS subretinal_fluid BOOLEAN;
ALTER TABLE treatment ADD COLUMN IF NOT EXISTS intraretinal_fluid_increase BOOLEAN;
ALTER TABLE treatment ADD COLUMN IF NOT EXISTS serous_rpe_detachment_increase BOOLEAN;
ALTER TABLE treatment ADD COLUMN IF NOT EXISTS new_retinal_hemorrhage BOOLEAN;
ALTER TABLE treatment ADD COLUMN IF NOT EXISTS visual_acuity VARCHAR(255);
