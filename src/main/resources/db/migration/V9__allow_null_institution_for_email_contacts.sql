-- Allow institution_id to be NULL for system-wide email contacts (e.g., Super-Admin recovery email)
ALTER TABLE institution_email_contact
    ALTER COLUMN institution_id DROP NOT NULL;

