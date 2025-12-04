-- Migration: Add treatment remarks system
-- Adds support for standard remarks (institution-wide) and treatment-specific remarks

-- Standard remarks table (institution-wide configurable remarks)
CREATE TABLE standard_remark (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_id BIGINT NOT NULL,
    text VARCHAR(500) NOT NULL,
    sort_order INTEGER,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE
);

CREATE INDEX idx_standard_remark_institution ON standard_remark(institution_id);
CREATE INDEX idx_standard_remark_sort_order ON standard_remark(institution_id, sort_order);

-- Treatment remarks table (remarks assigned to treatments)
CREATE TABLE treatment_remark (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    treatment_id BIGINT NOT NULL,
    standard_remark_id BIGINT,
    text VARCHAR(500) NOT NULL,
    sort_order INTEGER,
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (standard_remark_id) REFERENCES standard_remark(id) ON DELETE SET NULL
);

CREATE INDEX idx_treatment_remark_treatment ON treatment_remark(treatment_id);
CREATE INDEX idx_treatment_remark_sort_order ON treatment_remark(treatment_id, sort_order);

