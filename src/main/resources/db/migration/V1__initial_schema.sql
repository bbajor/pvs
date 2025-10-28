-- Initial schema migration for PVS application
-- This migration creates the baseline database structure

-- Flyway schema history table will be created automatically
-- Create extension for UUID if needed (PostgreSQL specific)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- BasicEntity base columns: id, version are handled per-table below

-- User Account table
CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    user_id VARCHAR(255),
    full_name VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE user_account_roles (
    user_account_id BIGINT NOT NULL,
    roles VARCHAR(255),
    FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- Practice table
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
    additional_info TEXT
);

-- Health Insurance table
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
    cost_carrier_name VARCHAR(255)
);

-- Patient History table (created before Patient due to foreign key)
CREATE TABLE patient_history (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

-- Patient table
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
    insurance_number VARCHAR(255) UNIQUE,
    description TEXT,
    health_insurance_id INTEGER,
    patient_history_id INTEGER UNIQUE,
    CONSTRAINT uk_patient_name_birth UNIQUE (first_name, last_name, birth),
    FOREIGN KEY (health_insurance_id) REFERENCES health_insurance(id),
    FOREIGN KEY (patient_history_id) REFERENCES patient_history(id)
);

-- Index for insurance_number lookup
CREATE INDEX idx_patient_insurance_number ON patient(insurance_number);

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
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(255),
    description TEXT,
    due_date TIMESTAMP,
    completed BOOLEAN NOT NULL DEFAULT FALSE
);

-- Medication/ICD tables
CREATE TABLE medication (
    id SERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

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
CREATE TABLE treatment (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE treatment_plan (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0
);

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
    version BIGINT NOT NULL DEFAULT 0
);

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

