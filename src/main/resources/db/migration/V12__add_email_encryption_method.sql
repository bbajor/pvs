-- Add encryption method and S/MIME certificate fields to institution_email_contact table
ALTER TABLE institution_email_contact 
    ADD COLUMN encryption_method VARCHAR(20),
    ADD COLUMN smime_certificate TEXT;

-- Set default encryption method based on existing data:
-- If openpgp_public_key exists, set to OPENPGP, otherwise NONE
UPDATE institution_email_contact 
SET encryption_method = CASE 
    WHEN openpgp_public_key IS NOT NULL AND openpgp_public_key != '' THEN 'OPENPGP'
    ELSE 'NONE'
END;

