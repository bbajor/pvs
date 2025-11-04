package de.bbajor.pvs.security.domain;

import java.util.HashSet;
import java.util.Set;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.location.model.Location;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "user_account", uniqueConstraints = { 
    @UniqueConstraint(columnNames = { "institution_id", "username" })
})
public class UserAccount extends BasicEntity<Long> {

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    /**
     * The institution this user belongs to.
     * <p>
     * Data isolation: All filtering is done via institution.
     * Users belong to exactly one institution (null for super-admins).
     * </p>
     * <p>
     * This is the primary field for institution assignment.
     * The tenant field (below) is kept for backward compatibility during migration.
     * </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    /**
     * The preferred location (Standort) for this user.
     * <p>
     * Users can be assigned to a preferred location within their institution.
     * This is useful for:
     * - Appointment planning (filter appointments by preferred location)
     * - Treatment planning (assign treatments to user's preferred location)
     * - Default location selection in UI
     * </p>
     * <p>
     * This is optional - if not set, the user can work at any location
     * within their institution.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_location_id")
    private Location preferredLocation;
}
