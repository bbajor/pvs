-- Migration for Appointment Scheduler module
-- Adds tables for appointment scheduling, office hours, and scheduler assignments

-- Appointment Scheduler table
CREATE TABLE appointment_scheduler (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    practice_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    CONSTRAINT fk_scheduler_practice FOREIGN KEY (practice_id) REFERENCES practice(id) ON DELETE CASCADE,
    CONSTRAINT fk_scheduler_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE INDEX idx_scheduler_practice ON appointment_scheduler(practice_id);
CREATE INDEX idx_scheduler_tenant ON appointment_scheduler(tenant_id);
CREATE INDEX idx_scheduler_active ON appointment_scheduler(active);

-- Office Hours table
CREATE TABLE office_hours (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    scheduler_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    CONSTRAINT fk_office_hours_scheduler FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
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
    CONSTRAINT fk_assignment_scheduler FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_user FOREIGN KEY (user_account_id) REFERENCES user_account(id) ON DELETE CASCADE,
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
    tenant_id BIGINT NOT NULL,
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
    CONSTRAINT fk_appointment_scheduler FOREIGN KEY (scheduler_id) REFERENCES appointment_scheduler(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE SET NULL,
    CONSTRAINT chk_appointment_time CHECK (start_time < end_time)
);

CREATE INDEX idx_appointment_scheduler ON appointment(scheduler_id);
CREATE INDEX idx_appointment_tenant ON appointment(tenant_id);
CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_treatment ON appointment(treatment_id);
CREATE INDEX idx_appointment_start_time ON appointment(start_time);
CREATE INDEX idx_appointment_status ON appointment(status);

-- Treatment Doctor Assignment table (many-to-many relationship)
CREATE TABLE treatment_doctor (
    treatment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    PRIMARY KEY (treatment_id, doctor_id),
    CONSTRAINT fk_treatment_doctor_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    CONSTRAINT fk_treatment_doctor_user FOREIGN KEY (doctor_id) REFERENCES user_account(id) ON DELETE CASCADE
);

CREATE INDEX idx_treatment_doctor_treatment ON treatment_doctor(treatment_id);
CREATE INDEX idx_treatment_doctor_doctor ON treatment_doctor(doctor_id);

-- Comments for documentation
COMMENT ON TABLE appointment_scheduler IS 'Appointment schedulers for practices, doctors, and staff';
COMMENT ON TABLE office_hours IS 'Office hours configuration for schedulers';
COMMENT ON TABLE scheduler_assignment IS 'User/role assignments to schedulers';
COMMENT ON TABLE appointment IS 'Patient appointments in schedulers';
COMMENT ON TABLE treatment_doctor IS 'Many-to-many relationship between treatments and treating doctors';
