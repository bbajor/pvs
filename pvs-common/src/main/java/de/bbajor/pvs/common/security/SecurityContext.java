package de.bbajor.pvs.common.security;

import org.jspecify.annotations.Nullable;

/**
 * Security context for serverless functions.
 * 
 * In serverless environments, we cannot use ThreadLocal or Session-based security.
 * Instead, security information (user ID, roles, institution ID) is passed as
 * part of the function request.
 */
public class SecurityContext {
    
    private final @Nullable Long userId;
    private final @Nullable String username;
    private final @Nullable String[] roles;
    private final @Nullable Long institutionId;
    
    public SecurityContext(@Nullable Long userId, @Nullable String username, 
                          @Nullable String[] roles, @Nullable Long institutionId) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.institutionId = institutionId;
    }
    
    public @Nullable Long getUserId() {
        return userId;
    }
    
    public @Nullable String getUsername() {
        return username;
    }
    
    public @Nullable String[] getRoles() {
        return roles;
    }
    
    public @Nullable Long getInstitutionId() {
        return institutionId;
    }
    
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        for (String r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasAnyRole(String... requiredRoles) {
        if (roles == null || requiredRoles.length == 0) {
            return false;
        }
        for (String requiredRole : requiredRoles) {
            if (hasRole(requiredRole)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create an empty security context (for system/internal functions).
     */
    public static SecurityContext empty() {
        return new SecurityContext(null, null, null, null);
    }
    
    /**
     * Create a system security context (for scheduled tasks, etc.).
     */
    public static SecurityContext system() {
        return new SecurityContext(null, "SYSTEM", new String[]{"SYSTEM"}, null);
    }
}


