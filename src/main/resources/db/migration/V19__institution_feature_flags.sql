-- Feature Flags für Institutionen
-- Ermöglicht es SuperAdmin, Features pro Institution zu aktivieren/deaktivieren
-- Features sind standardmäßig deaktiviert

CREATE TABLE institution_feature (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    institution_id BIGINT NOT NULL,
    feature_key VARCHAR(100) NOT NULL,
    feature_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    beta BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (institution_id) REFERENCES institution(id) ON DELETE CASCADE,
    UNIQUE (institution_id, feature_key)
);

CREATE INDEX idx_institution_feature_institution ON institution_feature(institution_id);
CREATE INDEX idx_institution_feature_key ON institution_feature(feature_key);
CREATE INDEX idx_institution_feature_enabled ON institution_feature(enabled);



