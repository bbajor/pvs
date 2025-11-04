-- Initial schema migration for PVS application
-- This migration creates the baseline database structure
-- 
-- Data isolation: All data is filtered by institution.
-- Patient → Location → Institution (primary path)
-- Institution is the primary filtering unit for data isolation.

-- Flyway schema history table will be created automatically
-- Create extension for UUID if needed (PostgreSQL specific)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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

-- Tenant table (legacy - for migration support only)
-- TODO: Remove after complete migration to Institution
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000)
);

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
    institution_id BIGINT NOT NULL
);

CREATE INDEX idx_location_institution ON location(institution_id);

-- Practice table (legacy - for migration support only)
-- TODO: Remove after complete migration to Location
CREATE TABLE practice (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    practice_name VARCHAR(255),
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
    tenant_id BIGINT
);

CREATE INDEX idx_practice_tenant ON practice(tenant_id);

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
    -- Primary: institution assignment (data isolation via institution)
    institution_id BIGINT,
    -- Legacy: tenant assignment (for migration only)
    tenant_id BIGINT,
    -- Preferred location within institution (optional)
    preferred_location_id BIGINT,
    -- Constraints
    CONSTRAINT uk_user_account_institution_username UNIQUE (institution_id, username),
    CONSTRAINT uk_user_account_tenant_username UNIQUE (tenant_id, username),
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    FOREIGN KEY (preferred_location_id) REFERENCES location(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_account_institution ON user_account(institution_id);
CREATE INDEX idx_user_account_tenant ON user_account(tenant_id);

CREATE TABLE user_account_roles (
    user_account_id BIGINT NOT NULL,
    roles VARCHAR(255),
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE
);

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
-- Legacy: Patient → Practice → Tenant (for migration only).
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
    patient_history_id INTEGER UNIQUE,
    -- Primary: location assignment (data isolation via location → institution)
    location_id BIGINT,
    -- Institution assignment (set automatically from location.institution_id)
    -- Used for unique constraints at institution level (not location level)
    institution_id BIGINT NOT NULL,
    -- Legacy: practice assignment (for migration only)
    practice_id BIGINT,
    -- Constraints
    -- Unique constraints at institution level (not location level)
    -- A patient is unique per institution (first_name, last_name, birth), not per location
    CONSTRAINT uk_patient_institution_name_birth UNIQUE (institution_id, first_name, last_name, birth),
    CONSTRAINT uk_patient_institution_insurance_number UNIQUE (institution_id, insurance_number),
    CONSTRAINT uk_patient_practice_name_birth UNIQUE (practice_id, first_name, last_name, birth),
    FOREIGN KEY (health_insurance_id) REFERENCES health_insurance(id),
    FOREIGN KEY (patient_history_id) REFERENCES patient_history(id),
    FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE RESTRICT,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    FOREIGN KEY (practice_id) REFERENCES practice(id) ON DELETE RESTRICT
);

-- Indexes for patient lookup
CREATE INDEX idx_patient_location ON patient(location_id);
CREATE INDEX idx_patient_institution ON patient(institution_id);
CREATE INDEX idx_patient_practice ON patient(practice_id);
CREATE INDEX idx_patient_insurance_number ON patient(institution_id, insurance_number);

-- Additional Patient-related tables (basic structure - will be expanded based on actual entities)
CREATE TABLE anamnesis (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE patient_record (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE disease (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reason_for_visit (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

-- Task Management
-- Tasks are filtered by institution (data isolation).
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(255),
    description TEXT,
    due_date TIMESTAMP,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    -- Data isolation: institution assignment
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_task_institution ON task(institution_id);

-- Medication/ICD tables
-- Medications are filtered by institution (data isolation).
CREATE TABLE medication (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    -- Data isolation: institution assignment
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_medication_institution ON medication(institution_id);

CREATE TABLE icd_version (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE icd_entry (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE icd_primary_key (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE icd_additional_key (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE icd_star_key (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

-- Treatment related tables
-- All treatment-related data is filtered by institution (data isolation).
CREATE TABLE treatment (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    -- Data isolation: institution assignment
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_treatment_institution ON treatment(institution_id);

CREATE TABLE treatment_plan (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    -- Data isolation: institution assignment (via patient → location → institution)
    -- Explicit institution_id for performance and data isolation compliance
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_treatment_plan_institution ON treatment_plan(institution_id);

CREATE TABLE treatment_audit_log (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE clinical_trial (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE diagnosis (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    -- Data isolation: institution assignment
    institution_id BIGINT NOT NULL,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT
);

CREATE INDEX idx_diagnosis_institution ON diagnosis(institution_id);

-- Surgical Center
CREATE TABLE surgical_center (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE surgical_center_time_slot (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

-- AI Usage Log
CREATE TABLE ai_usage_log (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);
