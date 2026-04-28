-- Initial schema migration for PVS application
-- This migration creates the baseline database structure
-- 
-- Data isolation: All data is filtered by institution.
-- Patient → Location → Institution (primary path)
-- Institution is the primary filtering unit for data isolation.

-- Flyway schema history table will be created automatically
-- Create extension for UUID if needed (PostgreSQL specific)
-- Note: This is PostgreSQL-specific and will be skipped by H2
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'uuid-ossp') THEN
        CREATE EXTENSION "uuid-ossp";
    END IF;
END $$;

-- BasicEntity base columns: id, version are handled per-table below

-- Institution table (central registry database)
-- Each institution represents a customer organization (e.g., medical center, clinic).
-- In multi-database architecture, each institution has its own database.
-- An institution can have multiple locations where patients are treated.
CREATE TABLE institution (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_code VARCHAR(50) NOT NULL UNIQUE,
    institution_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    -- Institution address and contact information
    street VARCHAR(255),
    house_number VARCHAR(50),
    postal_code VARCHAR(20),
    city VARCHAR(255),
    country VARCHAR(255),
    phone VARCHAR(50),
    fax VARCHAR(50),
    email VARCHAR(255),
    -- Company information
    company_name VARCHAR(255),
    tax_id VARCHAR(100),
    -- Multi-database architecture fields
    database_name VARCHAR(100) NOT NULL UNIQUE,
    container_name VARCHAR(100) NOT NULL UNIQUE,
    database_port INTEGER,
    database_password VARCHAR(255),
    -- Remote LLM configuration (per institution)
    remote_llm_enabled BOOLEAN DEFAULT FALSE,
    remote_llm_api_url VARCHAR(500),
    remote_llm_api_key VARCHAR(500),
    remote_llm_monthly_quota INTEGER
);

CREATE INDEX idx_institution_code ON institution(institution_code);
CREATE INDEX idx_institution_active ON institution(active);
CREATE INDEX idx_institution_database_name ON institution(database_name);

-- Location table (replaces Practice)
-- Locations are stored in the institution's own database (not registry database).
-- An institution can have multiple locations where patients are treated.
-- Note: institution_id is a reference only (no FK constraint in multi-DB architecture).
CREATE TABLE location (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    location_name VARCHAR(255),
    street VARCHAR(255),
    house_number VARCHAR(255),
    postal_code VARCHAR(255),
    city VARCHAR(255),
    country VARCHAR(255),
    owner_name VARCHAR(255),
    owner_title VARCHAR(255),
    lanr VARCHAR(255),
    bsnr VARCHAR(255),
    phone VARCHAR(255),
    fax VARCHAR(255),
    email VARCHAR(255),
    additional_info TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_location_institution ON location(institution_id);

-- User Account table
-- Users belong to an institution (null for super-admins).
-- Users can have a preferred location within their institution.
CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    user_id VARCHAR(255),
    full_name VARCHAR(255),
    email VARCHAR(255),
    -- MFA fields
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    password_change_required BOOLEAN DEFAULT FALSE,
    initial_password_set BOOLEAN DEFAULT FALSE,
    -- Primary: institution assignment (data isolation via institution)
    institution_id BIGINT,
    -- Preferred location within institution (optional)
    preferred_location_id BIGINT,
    -- Constraints
    CONSTRAINT uk_user_account_institution_username UNIQUE (institution_id, username),
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    FOREIGN KEY (preferred_location_id) REFERENCES location(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_account_institution ON user_account(institution_id);
CREATE INDEX idx_user_account_preferred_location ON user_account(preferred_location_id);

CREATE TABLE user_account_roles (
    user_account_id BIGINT NOT NULL,
    roles VARCHAR(255),
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_account_roles_user ON user_account_roles(user_account_id);

-- Health Insurance table
-- Health insurances are filtered by institution (data isolation).
CREATE TABLE health_insurance (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    insurance_start DATE,
    insurance_type VARCHAR(255),
    status VARCHAR(255),
    wop VARCHAR(255),
    billing_carrier_country_code VARCHAR(255),
    billing_carrier_id VARCHAR(255),
    billing_carrier_name VARCHAR(255),
    cost_carrier_country_code VARCHAR(255),
    cost_carrier_id VARCHAR(255),
    cost_carrier_name VARCHAR(255),
    -- Data isolation: institution assignment
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_health_insurance_institution ON health_insurance(institution_id);

-- Patient History table (created before Patient due to foreign key)
CREATE TABLE patient_history (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

-- Patient table
-- Patients are assigned to a location.
-- Data isolation: Patient → Location → Institution (primary path).
CREATE TABLE patient (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    salutation VARCHAR(255),
    title VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    birth DATE NOT NULL,
    patient_street VARCHAR(255),
    patient_house_no VARCHAR(255),
    patient_postal_code INTEGER,
    patient_city VARCHAR(255),
    patient_country VARCHAR(255),
    gender VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    insurance_number VARCHAR(255),
    description TEXT,
    health_insurance_id INTEGER,
    patient_history_id INTEGER,
    location_id BIGINT,
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (health_insurance_id) REFERENCES health_insurance(id) ON DELETE SET NULL,
    FOREIGN KEY (patient_history_id) REFERENCES patient_history(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE SET NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    CONSTRAINT uk_patient_institution_name_birth UNIQUE (institution_id, first_name, last_name, birth),
    CONSTRAINT uk_patient_institution_insurance UNIQUE (institution_id, insurance_number)
);

CREATE INDEX idx_patient_location ON patient(location_id);
CREATE INDEX idx_patient_institution ON patient(institution_id);
CREATE INDEX idx_patient_health_insurance ON patient(health_insurance_id);

-- Medication table
CREATE TABLE medication (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    eingangsnummer VARCHAR(50),
    arzneimittelbezeichnung VARCHAR(500),
    darreichungsform VARCHAR(500),
    zielgruppe VARCHAR(100),
    anwendungsart VARCHAR(200),
    anwendungsgebiete TEXT,
    indikation_atc TEXT,
    bescheiddatum_zulassung VARCHAR(50),
    zulassungsstatus VARCHAR(100),
    zulassungs_nr VARCHAR(100),
    verkehrsfaehigkeit VARCHAR(50),
    zulassungs_reg_nr_oder_kennziffer VARCHAR(500),
    parallelimportinformationen TEXT,
    eu_verfahrensnummer VARCHAR(100),
    zulassungsinhaber VARCHAR(500),
    hersteller_endfreigabe VARCHAR(500),
    vertreiber VARCHAR(500),
    oertlicher_vertreter VARCHAR(500),
    wirkstoffe TEXT,
    packungsgroessen_gruppe TEXT,
    am_klassifikationen TEXT,
    description VARCHAR(1000),
    is_favourite BOOLEAN DEFAULT FALSE,
    valid_from DATE,
    valid_until DATE,
    additional_notes VARCHAR(1000)
);

-- Diagnosis table
CREATE TABLE diagnosis (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255),
    icd_code VARCHAR(50),
    description VARCHAR(1000)
);

-- Surgical Center table
CREATE TABLE surgical_center (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255),
    description VARCHAR(1000),
    phone VARCHAR(255),
    email VARCHAR(255),
    contact VARCHAR(255),
    phone_contact VARCHAR(255),
    surgical_center_street VARCHAR(255),
    surgical_center_house_no VARCHAR(255),
    surgical_center_postal_code INTEGER,
    surgical_center_city VARCHAR(255),
    surgical_center_country VARCHAR(255),
    institution_id BIGINT,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_surgical_center_institution ON surgical_center(institution_id);

-- Surgical Center Time Slot table
CREATE TABLE surgical_center_time_slot (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    description VARCHAR(1000),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    is_approved BOOLEAN DEFAULT FALSE,
    surgical_center_id INTEGER NOT NULL,
    FOREIGN KEY (surgical_center_id) REFERENCES surgical_center(id) ON DELETE CASCADE,
    CONSTRAINT uk_time_slot_center_date_time UNIQUE (surgical_center_id, date, start_time, end_time)
);

CREATE INDEX idx_time_slot_surgical_center ON surgical_center_time_slot(surgical_center_id);
CREATE INDEX idx_time_slot_date ON surgical_center_time_slot(date);

-- Treatment Plan table
CREATE TABLE treatment_plan (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    creation_date DATE,
    description VARCHAR(1000),
    additional_information VARCHAR(1000),
    institution_id BIGINT NOT NULL,
    patient_id INTEGER NOT NULL,
    diagnosis_id BIGINT,
    clinical_trial_id BIGINT,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (diagnosis_id) REFERENCES diagnosis(id) ON DELETE SET NULL
);

CREATE INDEX idx_treatment_plan_institution ON treatment_plan(institution_id);
CREATE INDEX idx_treatment_plan_patient ON treatment_plan(patient_id);
CREATE INDEX idx_treatment_plan_diagnosis ON treatment_plan(diagnosis_id);

-- Treatment table
CREATE TABLE treatment (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    approval_date DATE,
    approval_date_time TIMESTAMP,
    approved_by_user_id VARCHAR(255),
    approved_by_user_name VARCHAR(255),
    second_approval_date_time TIMESTAMP,
    second_approved_by_user_id VARCHAR(255),
    second_approved_by_user_name VARCHAR(255),
    frequency VARCHAR(255),
    dosage VARCHAR(255),
    bill_id VARCHAR(255),
    additional_info VARCHAR(1000),
    side_of_eye VARCHAR(50),
    treatment_plan_id BIGINT NOT NULL,
    surgical_center_time_slot_id BIGINT,
    medication_id BIGINT,
    FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (surgical_center_time_slot_id) REFERENCES surgical_center_time_slot(id) ON DELETE SET NULL,
    FOREIGN KEY (medication_id) REFERENCES medication(id) ON DELETE SET NULL
);

CREATE INDEX idx_treatment_plan ON treatment(treatment_plan_id);
CREATE INDEX idx_treatment_time_slot ON treatment(surgical_center_time_slot_id);
CREATE INDEX idx_treatment_medication ON treatment(medication_id);

-- Treatment Doctor Assignment table (many-to-many relationship)
CREATE TABLE treatment_doctor (
    treatment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    PRIMARY KEY (treatment_id, doctor_id),
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_treatment_doctor_treatment ON treatment_doctor(treatment_id);
CREATE INDEX idx_treatment_doctor_doctor ON treatment_doctor(doctor_id);

-- Appointment Scheduler table
CREATE TABLE appointment_scheduler (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    location_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    type VARCHAR(50) NOT NULL DEFAULT 'DOCTOR',
    FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE CASCADE
);

CREATE INDEX idx_scheduler_location ON appointment_scheduler(location_id);
CREATE INDEX idx_scheduler_active ON appointment_scheduler(active);

-- Office Hours table
CREATE TABLE office_hours (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    scheduler_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
    CONSTRAINT chk_time_range CHECK (start_time < end_time),
    CONSTRAINT chk_slot_duration CHECK (slot_duration_minutes >= 5 AND slot_duration_minutes <= 120)
);

CREATE INDEX idx_office_hours_scheduler ON office_hours(scheduler_id);
CREATE INDEX idx_office_hours_day ON office_hours(day_of_week);
CREATE INDEX idx_office_hours_active ON office_hours(active);

-- Scheduler Assignment table (links schedulers to users or roles)
CREATE TABLE scheduler_assignment (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    scheduler_id BIGINT NOT NULL,
    user_account_id BIGINT,
    role VARCHAR(50),
    FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT chk_assignment CHECK (user_account_id IS NOT NULL OR role IS NOT NULL)
);

CREATE INDEX idx_assignment_scheduler ON scheduler_assignment(scheduler_id);
CREATE INDEX idx_assignment_user ON scheduler_assignment(user_account_id);
CREATE INDEX idx_assignment_role ON scheduler_assignment(role);

-- Appointment table
CREATE TABLE appointment (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    scheduler_id BIGINT NOT NULL,
    patient_id INTEGER NOT NULL,
    treatment_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    reason VARCHAR(200) NOT NULL,
    notes VARCHAR(1000),
    additional_info VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(255),
    FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE SET NULL,
    CONSTRAINT chk_appointment_time CHECK (start_time < end_time)
);

CREATE INDEX idx_appointment_scheduler ON appointment(scheduler_id);
CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_treatment ON appointment(treatment_id);
CREATE INDEX idx_appointment_start_time ON appointment(start_time);
CREATE INDEX idx_appointment_status ON appointment(status);

-- Task table
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    description VARCHAR(255),
    creation_date TIMESTAMP,
    due_date DATE,
    completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    completed_by_user_id VARCHAR(255),
    completed_by_user_name VARCHAR(255),
    time_slot_id BIGINT,
    FOREIGN KEY (time_slot_id) REFERENCES surgical_center_time_slot(id) ON DELETE CASCADE
);

CREATE INDEX idx_task_time_slot ON task(time_slot_id);
CREATE INDEX idx_task_completed ON task(completed);
CREATE INDEX idx_task_due_date ON task(due_date);

-- AI Usage Log table
CREATE TABLE ai_usage_log (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    timestamp TIMESTAMP NOT NULL,
    provider VARCHAR(255) NOT NULL,
    request_type VARCHAR(255) NOT NULL,
    token_count BIGINT,
    status VARCHAR(50),
    error_message VARCHAR(1000)
);

CREATE INDEX idx_ai_usage_log_timestamp ON ai_usage_log(timestamp);
CREATE INDEX idx_ai_usage_log_provider ON ai_usage_log(provider);

-- ICD Version table (created before ICD Entry)
CREATE TABLE icd_version (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    description VARCHAR(1000),
    valid_from DATE,
    valid_to DATE
);

-- ICD Primary Key table (created before ICD Entry)
CREATE TABLE icd_primary_key (
    icd_primary_key_id BIGSERIAL PRIMARY KEY,
    key_number VARCHAR(20) NOT NULL UNIQUE
);

CREATE INDEX idx_icd_primary_key_number ON icd_primary_key(key_number);

-- ICD Star Key table (created before ICD Entry)
CREATE TABLE icd_star_key (
    icd_star_key_id BIGSERIAL PRIMARY KEY,
    key_number VARCHAR(20) NOT NULL UNIQUE
);

CREATE INDEX idx_icd_star_key_number ON icd_star_key(key_number);

-- ICD Additional Key table (created before ICD Entry)
CREATE TABLE icd_additional_key (
    icd_additional_key_id BIGSERIAL PRIMARY KEY,
    key_number VARCHAR(20) NOT NULL UNIQUE
);

CREATE INDEX idx_icd_additional_key_number ON icd_additional_key(key_number);

-- ICD Entry table (created before Disease)
CREATE TABLE icd_entry (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    coding_type INTEGER NOT NULL,
    print_indicator INTEGER NOT NULL,
    text_content TEXT NOT NULL
);

-- ICD Entry to Primary Key 1 (many-to-many)
CREATE TABLE icd_primary_key1 (
    icd_entry_id INTEGER NOT NULL,
    icd_primary_key_id BIGINT NOT NULL,
    PRIMARY KEY (icd_entry_id, icd_primary_key_id),
    FOREIGN KEY (icd_entry_id) REFERENCES icd_entry(id) ON DELETE CASCADE,
    FOREIGN KEY (icd_primary_key_id) REFERENCES icd_primary_key(icd_primary_key_id) ON DELETE CASCADE
);

CREATE INDEX idx_icd_primary_key1_entry ON icd_primary_key1(icd_entry_id);
CREATE INDEX idx_icd_primary_key1_key ON icd_primary_key1(icd_primary_key_id);

-- ICD Entry to Primary Key 2 (many-to-many)
CREATE TABLE icd_primary_key2 (
    icd_entry_id INTEGER NOT NULL,
    icd_primary_key_id BIGINT NOT NULL,
    PRIMARY KEY (icd_entry_id, icd_primary_key_id),
    FOREIGN KEY (icd_entry_id) REFERENCES icd_entry(id) ON DELETE CASCADE,
    FOREIGN KEY (icd_primary_key_id) REFERENCES icd_primary_key(icd_primary_key_id) ON DELETE CASCADE
);

CREATE INDEX idx_icd_primary_key2_entry ON icd_primary_key2(icd_entry_id);
CREATE INDEX idx_icd_primary_key2_key ON icd_primary_key2(icd_primary_key_id);

-- ICD Entry to Star Key (many-to-many join table)
CREATE TABLE icd_star_key_join (
    icd_entry_id INTEGER NOT NULL,
    icd_star_key_id BIGINT NOT NULL,
    PRIMARY KEY (icd_entry_id, icd_star_key_id),
    FOREIGN KEY (icd_entry_id) REFERENCES icd_entry(id) ON DELETE CASCADE,
    FOREIGN KEY (icd_star_key_id) REFERENCES icd_star_key(icd_star_key_id) ON DELETE CASCADE
);

CREATE INDEX idx_icd_star_key_join_entry ON icd_star_key_join(icd_entry_id);
CREATE INDEX idx_icd_star_key_join_key ON icd_star_key_join(icd_star_key_id);

-- ICD Entry to Additional Key (many-to-many join table)
CREATE TABLE icd_additional_key_join (
    icd_entry_id INTEGER NOT NULL,
    icd_additional_key_id BIGINT NOT NULL,
    PRIMARY KEY (icd_entry_id, icd_additional_key_id),
    FOREIGN KEY (icd_entry_id) REFERENCES icd_entry(id) ON DELETE CASCADE,
    FOREIGN KEY (icd_additional_key_id) REFERENCES icd_additional_key(icd_additional_key_id) ON DELETE CASCADE
);

CREATE INDEX idx_icd_additional_key_join_entry ON icd_additional_key_join(icd_entry_id);
CREATE INDEX idx_icd_additional_key_join_key ON icd_additional_key_join(icd_additional_key_id);

-- Reason For Visit table
CREATE TABLE reason_for_visit (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    date_of_visit DATE,
    reason VARCHAR(255),
    description VARCHAR(1000),
    additional_information VARCHAR(1000)
);

-- Anamnesis table
CREATE TABLE anamnesis (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    additional_information VARCHAR(1000)
);

-- Disease table (references ICD Entry)
CREATE TABLE disease (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255),
    icd_entry_id INTEGER,
    FOREIGN KEY (icd_entry_id) REFERENCES icd_entry(id) ON DELETE SET NULL
);

CREATE INDEX idx_disease_icd_entry ON disease(icd_entry_id);

-- Anamnesis to Disease (one-to-many via join table, as @OneToMany without mappedBy creates join table)
CREATE TABLE anamnesis_known_diseases (
    anamnesis_id INTEGER NOT NULL,
    known_diseases_id INTEGER NOT NULL,
    PRIMARY KEY (anamnesis_id, known_diseases_id),
    FOREIGN KEY (anamnesis_id) REFERENCES anamnesis(id) ON DELETE CASCADE,
    FOREIGN KEY (known_diseases_id) REFERENCES disease(id) ON DELETE CASCADE,
    CONSTRAINT UKcbipfnqtcqoh5f6ja0v0npouv UNIQUE (known_diseases_id)
);

CREATE INDEX idx_anamnesis_known_diseases_anamnesis ON anamnesis_known_diseases(anamnesis_id);
CREATE INDEX idx_anamnesis_known_diseases_disease ON anamnesis_known_diseases(known_diseases_id);

-- Patient Record table
CREATE TABLE patient_record (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    date_of_record DATE,
    description VARCHAR(1000),
    is_active BOOLEAN DEFAULT TRUE,
    reason_for_visit_id INTEGER,
    patient_anamnesis_id INTEGER,
    FOREIGN KEY (reason_for_visit_id) REFERENCES reason_for_visit(id) ON DELETE SET NULL,
    FOREIGN KEY (patient_anamnesis_id) REFERENCES anamnesis(id) ON DELETE SET NULL
);

CREATE INDEX idx_patient_record_reason ON patient_record(reason_for_visit_id);
CREATE INDEX idx_patient_record_anamnesis ON patient_record(patient_anamnesis_id);

-- PatientHistory to PatientRecord (one-to-many via join table, as @OneToMany without mappedBy creates join table)
-- Note: Hibernate uses plural form for join table name (patientRecords -> patient_records)
CREATE TABLE patient_history_patient_records (
    patient_history_id INTEGER NOT NULL,
    patient_records_id INTEGER NOT NULL,
    PRIMARY KEY (patient_history_id, patient_records_id),
    FOREIGN KEY (patient_history_id) REFERENCES patient_history(id) ON DELETE CASCADE,
    FOREIGN KEY (patient_records_id) REFERENCES patient_record(id) ON DELETE CASCADE,
    CONSTRAINT UKa8gs8vs2g6iernpqpsi5s3oj UNIQUE (patient_records_id)
);

CREATE INDEX idx_patient_history_records_history ON patient_history_patient_records(patient_history_id);
CREATE INDEX idx_patient_history_records_record ON patient_history_patient_records(patient_records_id);

-- Clinical Trial table
CREATE TABLE clinical_trial (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255),
    description VARCHAR(1000),
    code VARCHAR(255),
    sponsor VARCHAR(255),
    contact_person VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    ivom_id BIGINT,
    FOREIGN KEY (ivom_id) REFERENCES treatment_plan(id) ON DELETE SET NULL
);

CREATE INDEX idx_clinical_trial_treatment_plan ON clinical_trial(ivom_id);

-- Update TreatmentPlan to link to ClinicalTrial
ALTER TABLE treatment_plan ADD FOREIGN KEY (clinical_trial_id) REFERENCES clinical_trial(id) ON DELETE SET NULL;

-- Treatment Audit Log table
CREATE TABLE treatment_audit_log (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    action_type VARCHAR(50),
    action_timestamp TIMESTAMP,
    actor_user_id VARCHAR(64),
    actor_user_name VARCHAR(128),
    details VARCHAR(512),
    treatment_id BIGINT NOT NULL,
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE
);

CREATE INDEX idx_treatment_audit_log_treatment ON treatment_audit_log(treatment_id);
CREATE INDEX idx_treatment_audit_log_timestamp ON treatment_audit_log(action_timestamp);

-- Comments for documentation
COMMENT ON TABLE institution IS 'Institutions (customers) - stored in registry database';
COMMENT ON TABLE location IS 'Locations (practices) - stored in institution database';
COMMENT ON TABLE user_account IS 'User accounts - stored in institution database';
COMMENT ON TABLE health_insurance IS 'Health insurance records - filtered by institution';
COMMENT ON TABLE patient IS 'Patient records - filtered by institution';
COMMENT ON TABLE appointment_scheduler IS 'Appointment schedulers for locations';
COMMENT ON TABLE office_hours IS 'Office hours configuration for schedulers';
COMMENT ON TABLE scheduler_assignment IS 'User/role assignments to schedulers';
COMMENT ON TABLE appointment IS 'Patient appointments in schedulers';
COMMENT ON TABLE treatment_plan IS 'Treatment plans for patients';
COMMENT ON TABLE treatment IS 'Individual treatments in treatment plans';
COMMENT ON TABLE treatment_doctor IS 'Many-to-many relationship between treatments and treating doctors';
COMMENT ON TABLE surgical_center IS 'Surgical centers for treatments';
COMMENT ON TABLE surgical_center_time_slot IS 'Time slots for surgical centers';
COMMENT ON TABLE task IS 'Tasks for treatment review';
COMMENT ON TABLE ai_usage_log IS 'AI usage logging for monitoring';
COMMENT ON TABLE patient_history IS 'Patient history records';
COMMENT ON TABLE patient_record IS 'Individual patient records';
COMMENT ON TABLE reason_for_visit IS 'Reasons for patient visits';
COMMENT ON TABLE anamnesis IS 'Patient anamnesis records';
COMMENT ON TABLE disease IS 'Disease records linked to ICD entries';
COMMENT ON TABLE icd_version IS 'ICD version information';
COMMENT ON TABLE icd_entry IS 'ICD coding entries';
COMMENT ON TABLE icd_primary_key IS 'ICD primary key numbers';
COMMENT ON TABLE icd_star_key IS 'ICD star key numbers';
COMMENT ON TABLE icd_additional_key IS 'ICD additional key numbers';
COMMENT ON TABLE icd_primary_key1 IS 'Many-to-many relationship between ICD entries and primary keys (field 4)';
COMMENT ON TABLE icd_primary_key2 IS 'Many-to-many relationship between ICD entries and primary keys (field 7)';
COMMENT ON TABLE icd_star_key_join IS 'Many-to-many relationship between ICD entries and star keys';
COMMENT ON TABLE icd_additional_key_join IS 'Many-to-many relationship between ICD entries and additional keys';
COMMENT ON TABLE clinical_trial IS 'Clinical trial information linked to treatment plans';
COMMENT ON TABLE treatment_audit_log IS 'Audit log for treatment actions';

