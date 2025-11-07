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

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.email.EmailService;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;
import de.bbajor.pvs.security.mfa.MfaService;
import de.bbajor.pvs.security.mfa.service.MfaResetService;
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
    private final MfaResetService mfaResetService;
    private final EmailService emailService;
    private final AuthenticationContext authenticationContext;
    private final HttpSession httpSession;

    private final H2 title = new H2("Multi-Faktor-Authentifizierung");
    private final Paragraph instructions = new Paragraph(
            "Bitte geben Sie den 6-stelligen Code aus Ihrer Authenticator-App ein.");
    private final TextField codeField = new TextField("MFA-Code");
    private final Button verifyButton = new Button("Verifizieren");
    private final Button resetViaEmailButton = new Button("MFA per E-Mail zurücksetzen");

    public MfaVerificationView(
            CurrentUser currentUser,
            UserAccountRepository userAccountRepository,
            MfaService mfaService,
            MfaResetService mfaResetService,
            EmailService emailService,
            AuthenticationContext authenticationContext,
            HttpSession httpSession) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.mfaService = mfaService;
        this.mfaResetService = mfaResetService;
        this.emailService = emailService;
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
            if ("Enter".equals(e.getKey())) {
                verifyCode();
            }
        });

        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.addClickListener(e -> verifyCode());

        resetViaEmailButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetViaEmailButton.addClickListener(e -> sendResetEmail());

        add(title, instructions, codeField, verifyButton, resetViaEmailButton);
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
            
            // Show/hide reset button based on recovery email availability
            if (userAccount != null) {
                boolean isSuperAdmin = userAccount.getRoles() != null 
                        && userAccount.getRoles().contains(AppRoles.SUPER_ADMIN);
                boolean hasRecoveryEmail = mfaResetService.hasVerifiedRecoveryEmail(userAccount);
                resetViaEmailButton.setVisible(isSuperAdmin && hasRecoveryEmail);
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
            
            // Show reset option if available
            boolean isSuperAdmin = userAccount.getRoles() != null 
                    && userAccount.getRoles().contains(AppRoles.SUPER_ADMIN);
            if (isSuperAdmin && mfaResetService.hasVerifiedRecoveryEmail(userAccount)) {
                resetViaEmailButton.setVisible(true);
            }
            return;
        }

        // Mark MFA as verified in session
        MfaAuthenticationFilter.markMfaVerified(httpSession);

        Notification.show("MFA erfolgreich verifiziert!", 2000, Notification.Position.MIDDLE);

        // For SUPER_ADMIN: Check if SMTP and recovery email need to be configured
        boolean isSuperAdmin = userAccount.getRoles() != null 
                && userAccount.getRoles().contains(AppRoles.SUPER_ADMIN);
        
        if (isSuperAdmin && !userAccount.isRecoveryEmailVerified()) {
            // Redirect to recovery email setup
            getUI().ifPresent(ui -> ui.navigate("/admin/super-settings"));
            return;
        }

        // Check if password change is required
        if (userAccount.isPasswordChangeRequired()) {
            getUI().ifPresent(ui -> ui.navigate("/password-change"));
        } else {
            getUI().ifPresent(ui -> ui.navigate("/"));
        }
    }

    private void sendResetEmail() {
        String username = MfaAuthenticationFilter.getMfaUsername(httpSession);
        if (username == null) {
            Notification.show("Sitzung abgelaufen. Bitte melden Sie sich erneut an.", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }

        UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
        if (userAccount == null) {
            Notification.show("Benutzerkonto nicht gefunden", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Check if user is SUPER_ADMIN and has verified recovery email
        boolean isSuperAdmin = userAccount.getRoles() != null 
                && userAccount.getRoles().contains(AppRoles.SUPER_ADMIN);
        if (!isSuperAdmin) {
            Notification.show("MFA-Reset per E-Mail ist nur für Super-Admins verfügbar", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (!mfaResetService.hasVerifiedRecoveryEmail(userAccount)) {
            Notification.show("Keine verifizierte Recovery-E-Mail-Adresse hinterlegt. Bitte konfigurieren Sie diese zuerst in den Einstellungen.", 
                    5000, Notification.Position.MIDDLE);
            return;
        }

        try {
            // Generate reset token
            String token = mfaResetService.createResetToken(userAccount);
            
            // Create reset URL - use current request URL
            String resetUrl = getUI().map(ui -> {
                try {
                    jakarta.servlet.http.HttpServletRequest request = com.vaadin.flow.server.VaadinServletRequest.getCurrent().getHttpServletRequest();
                    String scheme = request.getScheme();
                    String host = request.getServerName();
                    int port = request.getServerPort();
                    String contextPath = request.getContextPath();
                    return scheme + "://" + host + (port != 80 && port != 443 ? ":" + port : "") 
                            + contextPath + "/mfa-reset?token=" + token;
                } catch (Exception e) {
                    return "https://example.com/mfa-reset?token=" + token;
                }
            }).orElse("https://example.com/mfa-reset?token=" + token);
            
            // Send email with reset link (will be encrypted if PGP key is available)
            String emailText = String.format(
                    "Hallo %s,\n\n" +
                    "Sie haben eine MFA-Zurücksetzung angefordert.\n\n" +
                    "Klicken Sie auf den folgenden Link, um die Multi-Faktor-Authentifizierung zurückzusetzen:\n" +
                    "%s\n\n" +
                    "Dieser Link ist 24 Stunden gültig.\n\n" +
                    "Falls Sie diese Anfrage nicht gestellt haben, ignorieren Sie diese E-Mail.\n\n" +
                    "Mit freundlichen Grüßen,\n" +
                    "PVS System",
                    userAccount.getUsername(), resetUrl);
            
            emailService.sendEmail(userAccount.getRecoveryEmail(), "MFA zurücksetzen", emailText);
            
            Notification.show("Eine E-Mail mit einem Reset-Link wurde an Ihre Recovery-E-Mail-Adresse gesendet.", 
                    5000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Fehler beim Senden der E-Mail: " + e.getMessage(), 
                    5000, Notification.Position.MIDDLE);
        }
    }
}
