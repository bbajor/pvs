package de.bbajor.pvs.security.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;

final class JwtUserAccountAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;

    JwtUserAccountAuthenticationConverter(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserAccount account = resolveAccount(jwt);
        if (!account.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }

        UserAccountUserDetailsAdapter principal = new UserAccountUserDetailsAdapter(account);
        return new JwtAppAuthenticationToken(jwt, principal, principal.getAuthorities());
    }

    private UserAccount resolveAccount(Jwt jwt) {
        return findUnique(userAccountRepository.findAllByUserId(jwt.getSubject()))
                .or(() -> findByUniqueHumanIdentifier(jwt))
                .orElseThrow(() -> new BadCredentialsException("User account not found"));
    }

    private Optional<UserAccount> findByUniqueHumanIdentifier(Jwt jwt) {
        for (String identifier : candidateIdentifiers(jwt)) {
            Optional<UserAccount> account = findUnique(
                    userAccountRepository.findAllByUsernameOrEmailOrderByInstitutionFirst(identifier));
            if (account.isPresent()) {
                return account;
            }
        }
        return Optional.empty();
    }

    private List<String> candidateIdentifiers(Jwt jwt) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        addIfPresent(identifiers, jwt.getClaimAsString("preferred_username"));
        addIfPresent(identifiers, jwt.getClaimAsString("username"));
        addIfPresent(identifiers, jwt.getClaimAsString("email"));
        addIfPresent(identifiers, jwt.getSubject());
        return List.copyOf(identifiers);
    }

    private static void addIfPresent(LinkedHashSet<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private static Optional<UserAccount> findUnique(List<UserAccount> accounts) {
        if (accounts.size() > 1) {
            throw new BadCredentialsException("Ambiguous user account");
        }
        return accounts.isEmpty() ? Optional.empty() : Optional.of(accounts.get(0));
    }
}
