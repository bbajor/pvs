package de.bbajor.pvs.institution.service;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.AppUserInfo;
import de.bbajor.pvs.security.AppUserPrincipal;
import de.bbajor.pvs.security.InstitutionAwarePrincipal;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;

/**
 * Resolves the active institution for the current thread from Spring Security (single-tenant deployment).
 * <p>
 * For background jobs and serverless wrappers that need an explicit tenant, use
 * {@link #runWithInstitutionId(Long, Runnable)}.
 * </p>
 */
@Service
public class CurrentInstitutionService {

    private static final Logger log = LoggerFactory.getLogger(CurrentInstitutionService.class);

    private final ThreadLocal<Long> executionOverride = new ThreadLocal<>();

    private final UserAccountRepository userAccountRepository;

    public CurrentInstitutionService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * Optional institution id: empty if not logged in or user has no institution.
     */
    public Optional<Long> getCurrentInstitutionId() {
        Long override = executionOverride.get();
        if (override != null) {
            return Optional.of(override);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                return Optional.of(institutionAuth.getInstitutionId());
            }
            log.debug("InstitutionAuthenticationToken has no institutionId");
            return Optional.empty();
        }
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserAccountUserDetailsAdapter adapter) {
                return resolveUserAccountInstitution(adapter.getUsername());
            }
            if (principal instanceof AppUserPrincipal appUserPrincipal) {
                return resolveUserAccountInstitution(appUserPrincipal.getAppUser());
            }
            if (principal instanceof InstitutionAwarePrincipal institutionAware) {
                return institutionAware.getInstitutionId();
            }
        }
        return Optional.empty();
    }

    private Optional<Long> resolveUserAccountInstitution(AppUserInfo appUser) {
        Optional<Long> institutionId = resolveUserAccountInstitution(appUser.getPreferredUsername());
        if (institutionId.isPresent()) {
            return institutionId;
        }

        String email = appUser.getEmail();
        if (email != null && !email.isBlank() && !email.equals(appUser.getPreferredUsername())) {
            return resolveUserAccountInstitution(email);
        }
        return Optional.empty();
    }

    private Optional<Long> resolveUserAccountInstitution(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        try {
            return userAccountRepository.findByUsernameOrEmail(identifier)
                    .filter(UserAccount::isEnabled)
                    .map(UserAccount::getInstitution)
                    .map(institution -> institution.getId());
        } catch (Exception e) {
            log.warn("Could not resolve institution from UserAccount");
            return Optional.empty();
        }
    }

    public Long getRequiredInstitutionId() {
        return getCurrentInstitutionId()
                .orElseThrow(() -> new IllegalStateException("No institution in current security context"));
    }

    public boolean hasInstitution() {
        return getCurrentInstitutionId().isPresent();
    }

    /**
     * Runs an action with a fixed institution id (scheduled jobs, Spring Cloud Functions, nested task creation).
     */
    public void runWithInstitutionId(@Nullable Long institutionId, Runnable action) {
        callWithInstitutionId(institutionId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Executes a supplier with a fixed institution id (e.g. Spring Cloud Functions).
     */
    public <T> T callWithInstitutionId(@Nullable Long institutionId, Supplier<T> supplier) {
        if (institutionId == null) {
            return supplier.get();
        }
        Long previous = executionOverride.get();
        try {
            executionOverride.set(institutionId);
            return supplier.get();
        } finally {
            if (previous != null) {
                executionOverride.set(previous);
            } else {
                executionOverride.remove();
            }
        }
    }

    /**
     * Clears any execution override (rare; prefer {@link #runWithInstitutionId}).
     */
    public void clearExecutionOverride() {
        executionOverride.remove();
    }

    /**
     * Sets the institution id for the current thread (legacy dialogs, login success path).
     * Pair with {@link #clearExecutionOverride()} when the scope ends.
     */
    public void setThreadLocalInstitutionId(Long institutionId) {
        if (institutionId == null) {
            executionOverride.remove();
        } else {
            executionOverride.set(institutionId);
        }
    }

    public boolean institutionMatches(Long expectedInstitutionId) {
        return Objects.equals(getCurrentInstitutionId().orElse(null), expectedInstitutionId);
    }
}
