package de.bbajor.pvs.security;

/**
 * Constants for application role names used throughout the security system.
 * <p>
 * This class centralizes the definition of all role names used in the application, ensuring consistency across security
 * configurations, annotations, and tests. Using these constants instead of string literals helps prevent typos and
 * makes role management more maintainable.
 * </p>
 * <p>
 * These role names are used in various contexts:
 * <ul>
 * <li>Security method annotations: {@code @PreAuthorize("hasRole('" + AppRoles.ADMIN + "')")}</li>
 * <li>Security configuration and access control rules</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Customization:</strong> Modify or add role constants in this class to match your application's security
 * requirements. Consider the principle of least privilege when defining roles and their associated permissions.
 * </p>
 * <p>
 * Example usage: <!-- spotless:off -->
 * <pre>
 * {@code
 * // In security annotations
 * @PreAuthorize("hasRole('" + AppRoles.ADMIN + "')")
 * public void adminOnlyMethod() { ... }
 *
 * @Route
 * @RolesAllowed(AppRoles.ADMIN)
 * public class AdminView extends Main { ... }
 * }
 * </pre>
 * <!-- spotless:on -->
 * </p>
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
public final class AppRoles {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private AppRoles() {
    }

    /**
     * Role for administrative users with elevated privileges.
     * <p>
     * Users with this role typically have access to administrative functions such as user management, system
     * configuration, and sensitive operations. Use this role sparingly and only for users who require administrative
     * access.
     * </p>
     */
    public static final String ADMIN = "ADMIN";

    /**
     * Role for standard application users.
     * <p>
     * This is the default role for regular users of the application. Users with this role have access to standard
     * application features but not administrative functions.
     * </p>
     */
    public static final String USER = "USER";

    /**
     * Role for technical users with elevated privileges.
     * <p>
     * This is the default role for technical users of the application. Users with this role have access to standard
     * application features as well as migration/update jobs for external data but not full administrative functions.
     * </p>
     */
    public static final String TECH_USER = "TECH_USER";

    /**
     * Role for medical doctors with permission to approve and modify treatments.
     */
    public static final String DOCTOR = "DOCTOR";

    /**
     * Role for medical staff (non-doctor) personnel.
     */
    public static final String MEDICAL_STAFF = "MEDICAL_STAFF";

    /**
     * Role for the practice owner.
     */
    public static final String OWNER = "OWNER";

    /**
     * Role for institution administrators who can create and manage institutions.
     * <p>
     * Users with this role can create institutions and initial admins for those institutions,
     * but cannot access the data of the institutions themselves (data isolation).
     * This role is typically used by system administrators who manage the multi-tenant setup.
     * MFA is optional but recommended for users with this role.
     * </p>
     */
    public static final String INSTITUTION_ADMIN = "INSTITUTION_ADMIN";

    /**
     * Role for super administrators with full system access.
     * <p>
     * Super administrators have the highest level of access and can perform all administrative operations.
     * MFA is required for users with this role.
     * Users with this role have access to all system functions and can manage everything,
     * including institutions and their data. Use this role sparingly.
     * </p>
     */
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
}
