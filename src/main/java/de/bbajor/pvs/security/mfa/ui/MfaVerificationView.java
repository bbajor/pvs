package de.bbajor.pvs.security.mfa.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;

/**
 * View for MFA code verification after password authentication.
 * 
 * <p>
 * This view is displayed when a user has successfully authenticated with their password
 * but MFA is enabled and the code has not been verified yet.
 * </p>
 */
@Route("mfa-verify")
@PageTitle("MFA Verifizierung")
@PermitAll
public class MfaVerificationView extends VerticalLayout implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final MfaService mfaService;
    private final AuthenticationContext authenticationContext;
    private final HttpSession httpSession;

    private final H2 title = new H2("Multi-Faktor-Authentifizierung");
    private final Paragraph instructions = new Paragraph(
            "Bitte geben Sie den 6-stelligen Code aus Ihrer Authenticator-App ein.");
    private final TextField codeField = new TextField("MFA-Code");
    private final Button verifyButton = new Button("Verifizieren");

    public MfaVerificationView(
            CurrentUser currentUser,
            UserAccountRepository userAccountRepository,
            MfaService mfaService,
            AuthenticationContext authenticationContext,
            HttpSession httpSession) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.mfaService = mfaService;
        this.authenticationContext = authenticationContext;
        this.httpSession = httpSession;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("500px");
        setAlignItems(Alignment.CENTER);

        codeField.setPlaceholder("000000");
        codeField.setMaxLength(6);
        codeField.setPattern("[0-9]{6}");
        codeField.setHelperText("6-stelliger Code aus Ihrer Authenticator-App");
        codeField.addKeyPressListener(e -> {
            if (e.getKey().equals("Enter")) {
                verifyCode();
            }
        });

        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.addClickListener(e -> verifyCode());

        add(title, instructions, codeField, verifyButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if user is authenticated
        if (!currentUser.get().isPresent()) {
            event.forwardTo("/");
            return;
        }

        // Check if MFA is actually required
        if (!MfaAuthenticationFilter.isMfaRequired(httpSession)) {
            // MFA not required, redirect to main view
            event.forwardTo("/");
            return;
        }

        // Check if password change is required
        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount != null && userAccount.isPasswordChangeRequired()) {
                // Password change required, redirect to password change view
                event.forwardTo("/password-change");
            }
        });
    }

    private void verifyCode() {
        String code = codeField.getValue();

        if (code == null || code.length() != 6) {
            Notification.show("Bitte geben Sie einen 6-stelligen Code ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get username from session
        String username = MfaAuthenticationFilter.getMfaUsername(httpSession);
        if (username == null) {
            Notification.show("Sitzung abgelaufen. Bitte melden Sie sich erneut an.", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }

        // Get user account
        UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
        if (userAccount == null || !userAccount.isMfaEnabled() || userAccount.getMfaSecret() == null) {
            Notification.show("MFA ist nicht aktiviert für dieses Konto", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }

        // Verify the code
        if (!mfaService.verifyCode(userAccount.getMfaSecret(), code)) {
            Notification.show("Ungültiger Code. Bitte versuchen Sie es erneut.", 3000, Notification.Position.MIDDLE);
            codeField.clear();
            codeField.focus();
            return;
        }

        // Mark MFA as verified in session
        MfaAuthenticationFilter.markMfaVerified(httpSession);

        Notification.show("MFA erfolgreich verifiziert!", 2000, Notification.Position.MIDDLE);

        // Check if password change is required
        if (userAccount.isPasswordChangeRequired()) {
            getUI().ifPresent(ui -> ui.navigate("/password-change"));
        } else {
            getUI().ifPresent(ui -> ui.navigate("/"));
        }
    }
}
