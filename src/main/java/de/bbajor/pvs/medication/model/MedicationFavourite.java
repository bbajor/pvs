package de.bbajor.pvs.medication.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
@Table(name = "medication_favourite", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "institution_id", "medication_id" })
})
public class MedicationFavourite extends BasicEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (validFrom == null) {
            validFrom = now.toLocalDate();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getEffectiveDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return medication != null ? medication.getArzneimittelbezeichnung() : "";
    }
}

