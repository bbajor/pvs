-- Add main_location flag to location table
ALTER TABLE location ADD COLUMN IF NOT EXISTS main_location BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure that each institution has at most one main location.
-- (Application code in LocationService enforces exactly one.)
CREATE INDEX IF NOT EXISTS idx_location_main_location ON location(institution_id, main_location);