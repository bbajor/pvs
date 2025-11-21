package de.bbajor.pvs.institution.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity representing a feature flag for an institution.
 * Allows SuperAdmin to enable/disable features per institution.
 * Features are disabled by default.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "institution_feature", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "institution_id", "feature_key" })
})
public class InstitutionFeature extends BasicEntity<Long> {

    /**
     * The institution this feature flag belongs to.
     */
    @ManyToOne
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * Feature key (e.g., "EGK_READER", "VOICE_INPUT").
     * Must be unique per institution.
     */
    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    /**
     * Feature name for display (e.g., "Gesundheitskarte einlesen").
     */
    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    /**
     * Feature description.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Whether this feature is enabled for this institution.
     * Default: false (disabled).
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /**
     * Whether this is a beta feature (for testing purposes).
     */
    @Column(name = "beta", nullable = false)
    private boolean beta = false;

    @Override
    public String toString() {
        return String.format("%s (%s): %s", featureName, featureKey, enabled ? "Aktiviert" : "Deaktiviert");
    }
}

