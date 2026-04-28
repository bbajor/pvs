-- Make treatment_id nullable in treatment_audit_log to allow audit logs for deleted treatments
-- Change foreign key constraint from ON DELETE CASCADE to ON DELETE SET NULL
-- This allows audit logs to persist even after the treatment is deleted

-- First, find and drop the existing foreign key constraint
-- PostgreSQL: Use DO block to find constraint name dynamically
-- H2: Will fail on DO block, but that's OK - we'll handle it separately
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- PostgreSQL approach: Find constraint name dynamically
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'treatment_audit_log'::regclass
      AND contype = 'f'
      AND confrelid = 'treatment'::regclass
    LIMIT 1;
    
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE treatment_audit_log DROP CONSTRAINT %I', constraint_name);
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- If DO block fails (e.g., H2), try to drop common constraint names
    -- This will be handled by the ALTER TABLE statements below
    NULL;
END $$;

-- Make treatment_id nullable (works for both PostgreSQL and H2)
ALTER TABLE treatment_audit_log ALTER COLUMN treatment_id DROP NOT NULL;

-- For H2: Try to drop constraint by common name (if DO block didn't work)
-- This will fail silently if constraint doesn't exist or has different name
ALTER TABLE treatment_audit_log DROP CONSTRAINT IF EXISTS FKIS8HC8LK7W9FY3PFMVFKFDYED;

-- Recreate the foreign key constraint with ON DELETE SET NULL
ALTER TABLE treatment_audit_log 
    ADD CONSTRAINT treatment_audit_log_treatment_id_fkey 
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE SET NULL;

