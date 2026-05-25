package de.bbajor.pvs.security.api;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.security.AppUserPrincipal;

public final class JwtAppAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final AppUserPrincipal principal;

    public JwtAppAuthenticationToken(Jwt jwt, AppUserPrincipal principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    public Jwt getJwt() {
        return jwt;
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}

