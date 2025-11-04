-- Migration: Tenant to Institution, Practice to Location
-- This migration refactors the data model to support multi-database architecture per institution
-- WARNING: This is a breaking change migration - ensure full backup before running!

-- Step 1: Create Institution table (if tenant doesn't exist, or we'll migrate tenant data)
-- Note: In production, tenant table might already exist via Hibernate ddl-auto
CREATE TABLE IF NOT EXISTS tenant (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000)
);

-- Create Institution table with multi-database support fields
CREATE TABLE IF NOT EXISTS institution (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_code VARCHAR(50) NOT NULL UNIQUE,
    institution_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    database_name VARCHAR(100) UNIQUE,
    container_name VARCHAR(100) UNIQUE,
    database_port INTEGER,
    database_password VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_institution_code ON institution(institution_code);
CREATE INDEX IF NOT EXISTS idx_institution_active ON institution(active);
CREATE INDEX IF NOT EXISTS idx_institution_database_name ON institution(database_name);

-- Step 2: Migrate Tenant data to Institution
-- For each existing tenant, create corresponding institution
INSERT INTO institution (institution_code, institution_name, active, description, version, database_name, container_name)
SELECT 
    tenant_code,
    tenant_name,
    active,
    description,
    version,
    'pvs_inst_' || LOWER(REPLACE(tenant_code, '-', '_')),
    'postgres-inst-' || LOWER(REPLACE(tenant_code, '-', '_'))
FROM tenant
WHERE NOT EXISTS (
    SELECT 1 FROM institution WHERE institution_code = tenant.tenant_code
);

-- Step 3: Create Location table (rename/migrate from Practice)
CREATE TABLE IF NOT EXISTS location (
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
    institution_id BIGINT NOT NULL
);

-- Step 4: Migrate Practice data to Location
-- Map practice.tenant_id to location.institution_id via institution.institution_code
INSERT INTO location (
    id, version, location_name, street, house_number, postal_code, city, country,
    owner_name, owner_title, lanr, bsnr, phone, fax, email, additional_info, institution_id
)
SELECT 
    p.id,
    p.version,
    p.practice_name,
    p.street,
    p.house_number,
    p.postal_code,
    p.city,
    p.country,
    p.owner_name,
    p.owner_title,
    p.lanr,
    p.bsnr,
    p.phone,
    p.fax,
    p.email,
    p.additional_info,
    i.id  -- Map tenant_id to institution_id
FROM practice p
JOIN tenant t ON p.tenant_id = t.id
JOIN institution i ON i.institution_code = t.tenant_code
WHERE NOT EXISTS (
    SELECT 1 FROM location WHERE location.id = p.id
);

-- Step 5: Add preferred_location_id to user_account
ALTER TABLE user_account 
ADD COLUMN IF NOT EXISTS preferred_location_id BIGINT;

-- Step 6: Update all tenant_id references to institution_id
-- This requires careful migration for each table

-- 6.1: Update user_account to reference institution instead of tenant
-- First, add institution_id column
ALTER TABLE user_account 
ADD COLUMN IF NOT EXISTS institution_id BIGINT;

-- Migrate data: map tenant_id to institution_id
UPDATE user_account ua
SET institution_id = i.id
FROM tenant t
JOIN institution i ON i.institution_code = t.tenant_code
WHERE ua.tenant_id = t.id AND ua.institution_id IS NULL;

-- 6.2: Update patient table (if exists and has tenant_id)
DO $$
BEGIN
    -- Check if patient table has tenant_id column
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'patient' AND column_name = 'tenant_id'
    ) THEN
        -- Add institution_id column if not exists
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'patient' AND column_name = 'institution_id'
        ) THEN
            ALTER TABLE patient ADD COLUMN institution_id BIGINT;
        END IF;
        
        -- Migrate data
        UPDATE patient p
        SET institution_id = i.id
        FROM tenant t
        JOIN institution i ON i.institution_code = t.tenant_code
        WHERE p.tenant_id = t.id AND p.institution_id IS NULL;
    END IF;
END $$;

-- 6.3: Update appointment_scheduler table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'appointment_scheduler' AND column_name = 'tenant_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'appointment_scheduler' AND column_name = 'institution_id'
        ) THEN
            ALTER TABLE appointment_scheduler ADD COLUMN institution_id BIGINT;
        END IF;
        
        UPDATE appointment_scheduler a
        SET institution_id = i.id
        FROM tenant t
        JOIN institution i ON i.institution_code = t.tenant_code
        WHERE a.tenant_id = t.id AND a.institution_id IS NULL;
    END IF;
END $$;

-- 6.4: Update appointment table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'appointment' AND column_name = 'tenant_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'appointment' AND column_name = 'institution_id'
        ) THEN
            ALTER TABLE appointment ADD COLUMN institution_id BIGINT;
        END IF;
        
        UPDATE appointment a
        SET institution_id = i.id
        FROM tenant t
        JOIN institution i ON i.institution_code = t.tenant_code
        WHERE a.tenant_id = t.id AND a.institution_id IS NULL;
    END IF;
END $$;

-- 6.5: Update other tables with tenant_id (treatment, treatment_plan, task, etc.)
DO $$
DECLARE
    table_name_var TEXT;
BEGIN
    FOR table_name_var IN 
        SELECT table_name FROM information_schema.columns 
        WHERE column_name = 'tenant_id' 
        AND table_name NOT IN ('user_account', 'patient', 'appointment_scheduler', 'appointment', 'practice', 'location', 'tenant', 'institution')
    LOOP
        -- Add institution_id if not exists
        EXECUTE format('
            ALTER TABLE %I 
            ADD COLUMN IF NOT EXISTS institution_id BIGINT',
            table_name_var
        );
        
        -- Migrate data
        EXECUTE format('
            UPDATE %I t
            SET institution_id = i.id
            FROM tenant tn
            JOIN institution i ON i.institution_code = tn.tenant_code
            WHERE t.tenant_id = tn.id AND t.institution_id IS NULL',
            table_name_var
        );
    END LOOP;
END $$;

-- Step 7: Update practice_id references to location_id
-- 7.1: Update appointment_scheduler
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'appointment_scheduler' AND column_name = 'practice_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_name = 'appointment_scheduler' AND column_name = 'location_id'
        ) THEN
            ALTER TABLE appointment_scheduler ADD COLUMN location_id BIGINT;
        END IF;
        
        -- Migrate: practice.id maps to location.id (same ID)
        UPDATE appointment_scheduler
        SET location_id = practice_id
        WHERE location_id IS NULL AND practice_id IS NOT NULL;
    END IF;
END $$;

-- Step 8: Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_user_account_institution ON user_account(institution_id);
CREATE INDEX IF NOT EXISTS idx_user_account_preferred_location ON user_account(preferred_location_id);
CREATE INDEX IF NOT EXISTS idx_location_institution ON location(institution_id);
CREATE INDEX IF NOT EXISTS idx_appointment_scheduler_location ON appointment_scheduler(location_id) WHERE location_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_appointment_scheduler_institution ON appointment_scheduler(institution_id) WHERE institution_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_appointment_institution ON appointment(institution_id) WHERE institution_id IS NOT NULL;

-- Step 9: Comments for documentation
COMMENT ON TABLE institution IS 'Institution registry (stored in central registry database). Each institution will have its own database.';
COMMENT ON TABLE location IS 'Location/Standort of an institution. Multiple locations per institution are possible. Stored in institution database.';
COMMENT ON COLUMN user_account.institution_id IS 'Reference to institution (replaces tenant_id)';
COMMENT ON COLUMN user_account.preferred_location_id IS 'Preferred location for user (optional, for appointment/treatment filtering)';
COMMENT ON COLUMN location.institution_id IS 'Reference to institution. Note: In multi-DB architecture, this is stored as ID only (no FK constraint)';

-- Note: Foreign key constraints will be added later when we switch to multi-DB architecture
-- For now, we keep both tenant_id and institution_id to allow gradual migration

