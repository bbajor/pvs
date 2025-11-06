package de.bbajor.pvs.security.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.annotation.security.RolesAllowed;

/**
 * Settings view for Super-Admin users.
 * 
 * <p>
 * This view provides access to Super-Admin specific settings, including MFA setup.
 * </p>
 */
@Route("admin/super-admin-settings")
@PageTitle("Super-Admin Einstellungen")
@RolesAllowed({ AppRoles.SUPER_ADMIN })
public class SuperAdminSettingsView extends VerticalLayout implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;

    private final H2 title = new H2("Super-Admin Einstellungen");
    private final Paragraph mfaStatus = new Paragraph();
    private final Button mfaSetupButton = new Button("MFA einrichten");

    public SuperAdminSettingsView(CurrentUser currentUser, UserAccountRepository userAccountRepository) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        mfaSetupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        mfaSetupButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("/mfa-setup"));
        });

        add(title, mfaStatus, mfaSetupButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUser.get().isPresent()) {
            event.forwardTo("/");
            return;
        }

        // Update MFA status
        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getUsername()).orElse(null);
            if (userAccount != null) {
                if (userAccount.isMfaEnabled()) {
                    mfaStatus.setText("Multi-Faktor-Authentifizierung: Aktiviert");
                    mfaSetupButton.setText("MFA-Einstellungen ändern");
                } else {
                    mfaStatus.setText("Multi-Faktor-Authentifizierung: Nicht aktiviert");
                    mfaSetupButton.setText("MFA einrichten");
                }
            }
        });
    }
}
