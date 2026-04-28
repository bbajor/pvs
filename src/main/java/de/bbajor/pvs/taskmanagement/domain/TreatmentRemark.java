package de.bbajor.pvs.taskmanagement.domain;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Bemerkung für eine Behandlung.
 * Kann entweder eine Standardbemerkung oder eine benutzerdefinierte Bemerkung sein.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "treatment_remark")
public class TreatmentRemark extends BasicEntity<Long> {

    /**
     * Behandlung, zu der diese Bemerkung gehört.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    /**
     * Standardbemerkung (optional, falls es eine Standardbemerkung ist).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "standard_remark_id")
    private StandardRemark standardRemark;

    /**
     * Text der Bemerkung (für benutzerdefinierte Bemerkungen oder als Fallback).
     */
    @NotBlank
    @Size(max = 500)
    private String text;

    /**
     * Sortierreihenfolge (für alphanumerische Sortierung).
     */
    private Integer sortOrder;
}

