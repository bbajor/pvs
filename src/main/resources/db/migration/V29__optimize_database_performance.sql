-- Performance optimization migration for cloud deployment
-- Adds additional indexes for frequently queried columns and composite indexes

-- Patient search optimization (name, birth date)
CREATE INDEX IF NOT EXISTS idx_patient_last_name_first_name ON patient(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_patient_birth_date ON patient(birth_date) WHERE birth_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_patient_insurance_number ON patient(insurance_number) WHERE insurance_number IS NOT NULL;

-- Appointment query optimization (date range queries)
CREATE INDEX IF NOT EXISTS idx_appointment_start_time_date ON appointment(DATE(start_time));
CREATE INDEX IF NOT EXISTS idx_appointment_scheduler_start_time ON appointment(scheduler_id, start_time);

-- Treatment plan optimization
CREATE INDEX IF NOT EXISTS idx_treatment_plan_patient_institution ON treatment_plan(patient_id, institution_id);
CREATE INDEX IF NOT EXISTS idx_treatment_plan_status ON treatment_plan(status) WHERE status IS NOT NULL;

-- Task optimization (due date queries)
CREATE INDEX IF NOT EXISTS idx_task_due_date_completed ON task(due_date, completed) WHERE due_date IS NOT NULL;

-- User account optimization (login queries)
CREATE INDEX IF NOT EXISTS idx_user_account_username_enabled ON user_account(username, enabled) WHERE enabled = TRUE;

-- AI usage log optimization (monthly quota queries)
CREATE INDEX IF NOT EXISTS idx_ai_usage_log_provider_timestamp ON ai_usage_log(provider, timestamp);

-- Enable slow query log (PostgreSQL configuration - requires superuser)
-- This should be configured at database level, not in migration
-- ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries > 1 second
-- SELECT pg_reload_conf();

-- Analyze tables for query planner optimization
ANALYZE;


