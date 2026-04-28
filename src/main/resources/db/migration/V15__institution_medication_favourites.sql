-- Create table for institution-specific medication favourites
CREATE TABLE medication_favourite (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_id BIGINT NOT NULL,
    medication_id BIGINT NOT NULL,
    display_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATE,
    valid_until DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uc_medication_favourite UNIQUE (institution_id, medication_id),
    CONSTRAINT fk_medication_favourite_institution FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE,
    CONSTRAINT fk_medication_favourite_medication FOREIGN KEY (medication_id) REFERENCES medication(id) ON DELETE CASCADE
);

CREATE INDEX idx_medication_favourite_institution ON medication_favourite(institution_id);
CREATE INDEX idx_medication_favourite_medication ON medication_favourite(medication_id);

-- Add new reference from treatment to medication favourite
ALTER TABLE treatment
    ADD COLUMN medication_favourite_id BIGINT;

ALTER TABLE treatment
    ADD CONSTRAINT fk_treatment_medication_favourite
    FOREIGN KEY (medication_favourite_id) REFERENCES medication_favourite(id) ON DELETE SET NULL;

-- Seed favourites for all institutions based on existing global favourites
INSERT INTO medication_favourite (institution_id, medication_id, display_name, active, valid_from, created_at, updated_at)
SELECT inst.id,
       med.id,
       med.arzneimittelbezeichnung,
       TRUE,
       COALESCE(med.valid_from, CURRENT_DATE),
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM institution inst
JOIN medication med ON med.is_favourite = TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM medication_favourite existing
    WHERE existing.institution_id = inst.id
      AND existing.medication_id = med.id
);

-- Seed favourites for treatments that reference medications (ensures historical coverage)
INSERT INTO medication_favourite (institution_id, medication_id, display_name, active, valid_from, created_at, updated_at)
SELECT DISTINCT tp.institution_id,
       t.medication_id,
       med.arzneimittelbezeichnung,
       TRUE,
       COALESCE(med.valid_from, CURRENT_DATE),
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM treatment t
JOIN treatment_plan tp ON tp.id = t.treatment_plan_id
JOIN medication med ON med.id = t.medication_id
LEFT JOIN medication_favourite mf
       ON mf.institution_id = tp.institution_id
      AND mf.medication_id = t.medication_id
WHERE t.medication_id IS NOT NULL
  AND mf.id IS NULL;

-- Point treatments to their corresponding medication favourites
UPDATE treatment t
SET medication_favourite_id = mf.id
FROM treatment_plan tp
JOIN medication_favourite mf
  ON mf.institution_id = tp.institution_id
 AND mf.medication_id = t.medication_id
WHERE t.treatment_plan_id = tp.id
  AND t.medication_id IS NOT NULL;

-- Drop legacy foreign key and column from treatment
ALTER TABLE treatment
    DROP CONSTRAINT IF EXISTS fk_treatment_medication_id;
ALTER TABLE treatment
    DROP CONSTRAINT IF EXISTS treatment_medication_id_fkey;
DROP INDEX IF EXISTS idx_treatment_medication;
ALTER TABLE treatment
    DROP COLUMN IF EXISTS medication_id;

-- Drop global favourite flag on medication
ALTER TABLE medication
    DROP COLUMN IF EXISTS is_favourite;
