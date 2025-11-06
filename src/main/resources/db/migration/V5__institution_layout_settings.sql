-- Migration: Add layout settings fields to institution table
-- Allows institutions to customize their UI appearance based on their website colors

ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_primary_color VARCHAR(7);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_secondary_color VARCHAR(7);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_background_color VARCHAR(7);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_text_color VARCHAR(7);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_accent_color VARCHAR(7);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_border_radius VARCHAR(10);
ALTER TABLE institution ADD COLUMN IF NOT EXISTS layout_font_family VARCHAR(100);

COMMENT ON COLUMN institution.layout_primary_color IS 'Primary brand color (hex format, e.g., #1976d2)';
COMMENT ON COLUMN institution.layout_secondary_color IS 'Secondary brand color (hex format)';
COMMENT ON COLUMN institution.layout_background_color IS 'Background color for main content areas';
COMMENT ON COLUMN institution.layout_text_color IS 'Primary text color';
COMMENT ON COLUMN institution.layout_accent_color IS 'Accent color for highlights and call-to-action elements';
COMMENT ON COLUMN institution.layout_border_radius IS 'Border radius for UI elements (e.g., "8px", "0.5rem")';
COMMENT ON COLUMN institution.layout_font_family IS 'Font family for UI text (e.g., "Arial, sans-serif")';

