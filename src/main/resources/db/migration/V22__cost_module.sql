-- Cost Module: Preismodell-Verwaltung und Kostenberechnung
-- Ermöglicht die Berechnung von OP-Saal-Kosten basierend auf verschiedenen Preismodellen
-- (Miete vs. eigene Kosten) sowie Kostenhistorie am Patienten

-- Cost Calculation (Preismodell pro OP-Saal)
CREATE TABLE cost_calculation (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    surgical_center_id INTEGER NOT NULL,
    institution_id BIGINT NOT NULL,
    pricing_model VARCHAR(50) NOT NULL,
    price_per_slot DECIMAL(19,2),
    price_per_hour DECIMAL(19,2),
    monthly_fixed_costs DECIMAL(19,2),
    variable_cost_per_treatment DECIMAL(19,2),
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (surgical_center_id) REFERENCES surgical_center(id) ON DELETE CASCADE,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cost_calculation_model CHECK (
        (pricing_model = 'RENTAL' AND (price_per_slot IS NOT NULL OR price_per_hour IS NOT NULL))
        OR (pricing_model = 'OWNED' AND monthly_fixed_costs IS NOT NULL)
    )
);

CREATE INDEX idx_cost_calculation_surgical_center ON cost_calculation(surgical_center_id);
CREATE INDEX idx_cost_calculation_institution ON cost_calculation(institution_id);
CREATE INDEX idx_cost_calculation_valid_from ON cost_calculation(valid_from);
CREATE INDEX idx_cost_calculation_active ON cost_calculation(active);

-- Treatment Cost (Kosten pro Behandlung)
CREATE TABLE treatment_cost (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    treatment_id BIGINT NOT NULL UNIQUE,
    total_cost DECIMAL(19,2) NOT NULL,
    cost_per_patient DECIMAL(19,2) NOT NULL,
    patient_count_at_calculation INTEGER,
    pricing_model_used VARCHAR(50),
    calculated_at TIMESTAMP NOT NULL,
    calculated_by_user_id VARCHAR(255),
    notes VARCHAR(1000),
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (calculated_by_user_id) REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE INDEX idx_treatment_cost_treatment ON treatment_cost(treatment_id);
CREATE INDEX idx_treatment_cost_calculated_at ON treatment_cost(calculated_at);

-- Patient Cost History (Kostenhistorie am Patienten)
CREATE TABLE patient_cost_history (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    patient_id INTEGER NOT NULL,
    treatment_id BIGINT NOT NULL,
    treatment_cost_id BIGINT NOT NULL,
    cost_amount DECIMAL(19,2) NOT NULL,
    treatment_date DATE NOT NULL,
    surgical_center_id INTEGER,
    FOREIGN KEY (patient_id) REFERENCES patient(id) ON DELETE CASCADE,
    FOREIGN KEY (treatment_id) REFERENCES treatment(id) ON DELETE CASCADE,
    FOREIGN KEY (treatment_cost_id) REFERENCES treatment_cost(id) ON DELETE CASCADE,
    FOREIGN KEY (surgical_center_id) REFERENCES surgical_center(id) ON DELETE SET NULL
);

CREATE INDEX idx_patient_cost_history_patient ON patient_cost_history(patient_id);
CREATE INDEX idx_patient_cost_history_treatment ON patient_cost_history(treatment_id);
CREATE INDEX idx_patient_cost_history_treatment_date ON patient_cost_history(treatment_date);
CREATE INDEX idx_patient_cost_history_surgical_center ON patient_cost_history(surgical_center_id);

