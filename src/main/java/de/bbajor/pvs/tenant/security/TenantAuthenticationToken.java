package de.bbajor.pvs.tenant.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token that includes tenant information.
 */
public class TenantAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final String tenantCode;
    private final Long tenantId;

    public TenantAuthenticationToken(String tenantCode, Object principal, Object credentials) {
        super(principal, credentials);
        this.tenantCode = tenantCode;
        this.tenantId = null;
    }

    public TenantAuthenticationToken(String tenantCode, Long tenantId, Object principal, Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.tenantCode = tenantCode;
        this.tenantId = tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
