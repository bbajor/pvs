-- Performance indexes (PostgreSQL + H2 kompatibel; keine partiellen Indizes / Ausdrücke in Indizes)

CREATE INDEX IF NOT EXISTS idx_patient_last_name_first_name ON patient(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_patient_birth_date ON patient(birth_date);
CREATE INDEX IF NOT EXISTS idx_patient_insurance_number ON patient(insurance_number);

CREATE INDEX IF NOT EXISTS idx_appointment_start_time ON appointment(start_time);
CREATE INDEX IF NOT EXISTS idx_appointment_scheduler_start_time ON appointment(scheduler_id, start_time);

CREATE INDEX IF NOT EXISTS idx_treatment_plan_patient_institution ON treatment_plan(patient_id, institution_id);
CREATE INDEX IF NOT EXISTS idx_treatment_plan_status ON treatment_plan(status);

CREATE INDEX IF NOT EXISTS idx_task_due_date_completed ON task(due_date, completed);

CREATE INDEX IF NOT EXISTS idx_user_account_username_enabled ON user_account(username, enabled);
