package de.bbajor.pvs.institution.security;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom authentication token that includes institution information.
 * <p>
 * This token extends UsernamePasswordAuthenticationToken (which is Serializable)
 * and can be stored in HTTP sessions. However, after deserialization, Spring Security
 * may convert it to a different Authentication type. Therefore, VaadinTenantContextInitializer
 * handles institution restoration from UserAccount if needed.
 * </p>
 */
public class InstitutionAuthenticationToken extends UsernamePasswordAuthenticationToken {   

    private final String institutionCode;
    private final Long institutionId;

    public InstitutionAuthenticationToken(String institutionCode, Object principal, Object credentials) {
        super(principal, credentials);
        this.institutionCode = institutionCode;
        this.institutionId = null;
    }

    public InstitutionAuthenticationToken(String institutionCode, Long institutionId, Object principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.institutionCode = institutionCode;
        this.institutionId = institutionId;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public Long getInstitutionId() {
        return institutionId;
    }
}

