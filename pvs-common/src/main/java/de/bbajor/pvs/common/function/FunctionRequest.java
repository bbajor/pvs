package de.bbajor.pvs.common.function;

import de.bbajor.pvs.common.domain.InstitutionAware;
import de.bbajor.pvs.common.security.SecurityContext;
import org.jspecify.annotations.Nullable;

/**
 * Base class for all function requests.
 * 
 * All function requests should extend this class to ensure:
 * - Institution/tenant isolation
 * - Security context
 * - Consistent request structure
 */
public abstract class FunctionRequest implements InstitutionAware {
    
    private @Nullable Long institutionId;
    private @Nullable SecurityContext securityContext;
    
    @Override
    public @Nullable Long getInstitutionId() {
        return institutionId;
    }
    
    @Override
    public void setInstitutionId(@Nullable Long institutionId) {
        this.institutionId = institutionId;
    }
    
    public @Nullable SecurityContext getSecurityContext() {
        return securityContext;
    }
    
    public void setSecurityContext(@Nullable SecurityContext securityContext) {
        this.securityContext = securityContext;
    }
}


