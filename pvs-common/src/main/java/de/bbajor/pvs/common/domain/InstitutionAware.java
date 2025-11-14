package de.bbajor.pvs.common.domain;

import org.jspecify.annotations.Nullable;

/**
 * Marker interface for entities and DTOs that are aware of their institution/tenant context.
 * 
 * Used in serverless functions to ensure multi-tenant isolation.
 * All function requests should include institutionId.
 */
public interface InstitutionAware {
    
    /**
     * Get the institution ID for this entity/DTO.
     * 
     * @return the institution ID, or null if not set
     */
    @Nullable
    Long getInstitutionId();
    
    /**
     * Set the institution ID for this entity/DTO.
     * 
     * @param institutionId the institution ID
     */
    void setInstitutionId(@Nullable Long institutionId);
}


