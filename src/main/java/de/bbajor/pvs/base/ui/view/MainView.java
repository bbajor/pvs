package de.bbajor.pvs.base.ui.view;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;

/**
 * This view shows up when a user navigates to the root ('/') of the application.
 */
@Route
@PermitAll // When security is enabled, allow all authenticated users
public final class MainView extends Main implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final HttpSession httpSession;

    // TODO Replace with your own main view.

    MainView(CurrentUser currentUser, UserAccountRepository userAccountRepository, HttpSession httpSession) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.httpSession = httpSession;
        
        addClassName(LumoUtility.Padding.MEDIUM);
        add(new ViewToolbar("Hauptansicht"));
        add(new Div("Bitte wählen Sie einen Bereich aus dem Menü auf der linken Seite."));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if user is authenticated
        if (!currentUser.get().isPresent()) {
            return; // Let Spring Security handle unauthenticated access
        }

        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount == null) {
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
        });
    }

    /**
     * Navigates to the main view.
     */
    public static void showMainView() {
        UI.getCurrent().navigate(MainView.class);
    }
}
