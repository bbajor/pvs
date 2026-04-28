package de.bbajor.pvs.taskmanagement.domain;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
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
 * Standardbemerkung für Behandlungen.
 * Institutionweit verfügbar und in den Einstellungen pflegbar.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "standard_remark")
public class StandardRemark extends BasicEntity<Long> {

    /**
     * Institution, zu der diese Standardbemerkung gehört.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /**
     * Text der Standardbemerkung.
     */
    @NotBlank
    @Size(max = 500)
    private String text;

    /**
     * Sortierreihenfolge (für alphanumerische Sortierung).
     */
    private Integer sortOrder;
}

