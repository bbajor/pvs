package de.bbajor.pvs.security.api;

import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import de.bbajor.pvs.security.AppUserInfo;
import de.bbajor.pvs.security.domain.UserId;

public record JwtAppUserInfo(
        UserId userId,
        String preferredUsername,
        String fullName,
        @Nullable String email,
        Locale locale,
        @Nullable Long institutionId) implements AppUserInfo {

    public JwtAppUserInfo {
        Objects.requireNonNull(userId);
        preferredUsername = Objects.requireNonNull(preferredUsername);
        fullName = fullName == null || fullName.isBlank() ? preferredUsername : fullName;
        locale = locale == null ? Locale.ROOT : locale;
    }

    @Override
    public UserId getUserId() {
        return userId;
    }

    @Override
    public String getPreferredUsername() {
        return preferredUsername;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public @Nullable String getEmail() {
        return email;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
}

