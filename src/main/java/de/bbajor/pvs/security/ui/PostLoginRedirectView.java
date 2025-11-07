package de.bbajor.pvs.security.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;

/**
 * View that handles post-login redirects based on user state.
 * 
 * <p>
 * This view checks if:
 * <ul>
 * <li>MFA verification is required</li>
 * <li>Password change is required</li>
 * </ul>
 * and redirects accordingly.
 * </p>
 */
@Route("post-login")
@PermitAll
public class PostLoginRedirectView implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final AuthenticationContext authenticationContext;
    private final HttpSession httpSession;

    public PostLoginRedirectView(
            CurrentUser currentUser,
            UserAccountRepository userAccountRepository,
            AuthenticationContext authenticationContext,
            HttpSession httpSession) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.authenticationContext = authenticationContext;
        this.httpSession = httpSession;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!authenticationContext.isAuthenticated()) {
            event.forwardTo("/");
            return;
        }

        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount == null) {
                event.forwardTo("/");
                return;
            }

            // Check if MFA is required
            if (userAccount.isMfaEnabled() && userAccount.getMfaSecret() != null) {
                if (MfaAuthenticationFilter.isMfaRequired(httpSession)) {
                    event.forwardTo("/mfa-verify");
                    return;
                }
            }

            // Check if password change is required
            if (userAccount.isPasswordChangeRequired()) {
                event.forwardTo("/password-change");
                return;
            }

            // All checks passed, redirect to main view
            event.forwardTo("/");
        });
    }
}
