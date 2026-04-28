package de.bbajor.pvs.security.api;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;

import de.bbajor.pvs.security.AppUserInfo;
import de.bbajor.pvs.security.AppUserPrincipal;
import de.bbajor.pvs.security.InstitutionAwarePrincipal;

public final class JwtAppUserPrincipal implements AppUserPrincipal, InstitutionAwarePrincipal {

    private final AppUserInfo appUser;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtAppUserPrincipal(AppUserInfo appUser, Collection<? extends GrantedAuthority> authorities) {
        this.appUser = Objects.requireNonNull(appUser);
        this.authorities = List.copyOf(Objects.requireNonNull(authorities));
    }

    @Override
    public AppUserInfo getAppUser() {
        return appUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public java.util.Optional<Long> getInstitutionId() {
        if (appUser instanceof JwtAppUserInfo jwtUserInfo) {
            return java.util.Optional.ofNullable(jwtUserInfo.institutionId());
        }
        return java.util.Optional.empty();
    }
}

