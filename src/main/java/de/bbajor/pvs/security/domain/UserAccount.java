package de.bbajor.pvs.security.domain;

import java.util.HashSet;
import java.util.Set;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
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
import org.hibernate.annotations.Filter;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
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

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "password_change_required")
    private boolean passwordChangeRequired = false;

    @Column(name = "initial_password_set")
    private boolean initialPasswordSet = false;

    @Column(name = "recovery_email")
    private String recoveryEmail;

    @Column(name = "recovery_email_verified")
    private boolean recoveryEmailVerified = false;

    @Column(name = "mfa_reset_token")
    private String mfaResetToken;

    @Column(name = "mfa_reset_token_expiry")
    private java.time.LocalDateTime mfaResetTokenExpiry;

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
