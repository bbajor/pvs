package de.bbajor.pvs.security.api;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserId;

final class JwtUserAccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;

    JwtUserAccountAuthenticationConverter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserAccount userAccount = resolveUserAccount(jwt)
                .orElseThrow(() -> new BadCredentialsException("Unknown user account"));
        if (!userAccount.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        var authorities = createAuthorities(userAccount.getRoles());
        var principal = new JwtAppUserPrincipal(toAppUser(jwt, userAccount), authorities);
        return new JwtAppAuthenticationToken(jwt, principal, authorities);
    }

    private Optional<UserAccount> resolveUserAccount(Jwt jwt) {
        String subject = jwt.getSubject();
        if (hasText(subject)) {
            Optional<UserAccount> bySubject = requireUnique(
                    userAccountRepository.findAllByUserIdOrderByInstitutionFirst(subject));
            if (bySubject.isPresent()) {
                return bySubject;
            }
        }

        for (String identifier : List.of(
                claim(jwt, "preferred_username"),
                claim(jwt, "username"),
                claim(jwt, "email"),
                subject)) {
            if (hasText(identifier)) {
                Optional<UserAccount> byIdentifier = requireUnique(
                        userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst(identifier));
                if (byIdentifier.isPresent()) {
                    return byIdentifier;
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<UserAccount> requireUnique(List<UserAccount> userAccounts) {
        if (userAccounts.isEmpty()) {
            return Optional.empty();
        }
        if (userAccounts.size() > 1) {
            throw new BadCredentialsException("Ambiguous user account");
        }
        return Optional.of(userAccounts.get(0));
    }

    private static JwtAppUserInfo toAppUser(Jwt jwt, UserAccount userAccount) {
        String userId = userAccount.getUserId();
        if (!hasText(userId)) {
            userId = userAccount.getUsername();
        }

        String fullName = userAccount.getFullName();
        if (!hasText(fullName)) {
            fullName = userAccount.getUsername();
        }

        Long institutionId = userAccount.getInstitution() == null ? null : userAccount.getInstitution().getId();
        return new JwtAppUserInfo(
                UserId.of(userId),
                userAccount.getUsername(),
                fullName,
                userAccount.getEmail(),
                locale(jwt),
                institutionId);
    }

    private static Collection<? extends GrantedAuthority> createAuthorities(Set<String> roles) {
        Set<String> normalizedRoles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
        if (normalizedRoles.contains("INSTITUTION_ADMIN")) {
            normalizedRoles.add("ADMIN");
        }
        return normalizedRoles.stream()
                .filter(JwtUserAccountAuthenticationConverter::hasText)
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private static Locale locale(Jwt jwt) {
        String localeClaim = claim(jwt, "locale");
        if (!hasText(localeClaim)) {
            return Locale.ROOT;
        }
        return Locale.forLanguageTag(localeClaim);
    }

    private static String claim(Jwt jwt, String claimName) {
        return jwt.getClaimAsString(claimName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
