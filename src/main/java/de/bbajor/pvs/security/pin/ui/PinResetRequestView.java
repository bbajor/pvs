package de.bbajor.pvs.security.pin.ui;

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
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.email.EmailService;
import de.bbajor.pvs.security.pin.service.PinResetService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * View for requesting PIN reset via recovery email.
 * Users must be authenticated and have a verified recovery email.
 */
@Route("pin-reset-request")
@PageTitle("PIN zurücksetzen")
@AnonymousAllowed
public class PinResetRequestView extends VerticalLayout implements BeforeEnterObserver {

    private final UserAccountRepository userAccountRepository;
    private final PinResetService pinResetService;
    private final EmailService emailService;

    private final H2 title = new H2("PIN zurücksetzen");
    private final Paragraph instructions = new Paragraph(
            "Eine E-Mail mit einem Reset-Link wird an Ihre Recovery-E-Mail-Adresse gesendet.");
    private final TextField usernameField = new TextField("Benutzername oder E-Mail");
    private final Button requestButton = new Button("Reset-Link anfordern");

    public PinResetRequestView(UserAccountRepository userAccountRepository,
                              PinResetService pinResetService,
                              EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.pinResetService = pinResetService;
        this.emailService = emailService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("500px");
        setAlignItems(Alignment.CENTER);

        usernameField.setWidthFull();
        usernameField.setRequired(true);

        requestButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        requestButton.addClickListener(e -> requestReset());

        add(title, instructions, usernameField, requestButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Allow unauthenticated access for PIN reset request
    }

    private void requestReset() {
        String identifier = usernameField.getValue();

        if (identifier == null || identifier.isEmpty()) {
            Notification.show("Bitte geben Sie einen Benutzernamen oder eine E-Mail-Adresse ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        UserAccount userAccount = userAccountRepository.findByUsernameOrEmail(identifier).orElse(null);
        if (userAccount == null) {
            // Don't reveal if user exists or not for security
            Notification.show("Falls ein Konto mit diesem Benutzernamen und einer verifizierten Recovery-E-Mail existiert, wurde eine E-Mail gesendet.", 
                    5000, Notification.Position.MIDDLE);
            return;
        }

        // Check if user has verified recovery email
        if (!pinResetService.hasVerifiedRecoveryEmail(userAccount)) {
            // Don't reveal if user exists or not for security
            Notification.show("Falls ein Konto mit diesem Benutzernamen und einer verifizierten Recovery-E-Mail existiert, wurde eine E-Mail gesendet.", 
                    5000, Notification.Position.MIDDLE);
            return;
        }

        // Check if user has MFA enabled (required for PIN login)
        if (!userAccount.isMfaEnabled() || userAccount.getMfaSecret() == null) {
            Notification.show("MFA muss aktiviert sein, um PIN-Login zu nutzen", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            // Generate reset token
            String token = pinResetService.createResetToken(userAccount);
            
            // Create reset URL
            String resetUrl = getUI().map(ui -> {
                try {
                    jakarta.servlet.http.HttpServletRequest request = com.vaadin.flow.server.VaadinServletRequest.getCurrent().getHttpServletRequest();
                    String scheme = request.getScheme();
                    String host = request.getServerName();
                    int port = request.getServerPort();
                    String contextPath = request.getContextPath();
                    return scheme + "://" + host + (port != 80 && port != 443 ? ":" + port : "") 
                            + contextPath + "/pin-reset?token=" + token;
                } catch (Exception e) {
                    return "https://example.com/pin-reset?token=" + token;
                }
            }).orElse("https://example.com/pin-reset?token=" + token);
            
            // Send email with reset link (will be encrypted if PGP key is available)
            String emailText = String.format(
                    "Hallo %s,\n\n" +
                    "Sie haben eine PIN-Zurücksetzung angefordert.\n\n" +
                    "Klicken Sie auf den folgenden Link, um Ihre PIN zurückzusetzen:\n" +
                    "%s\n\n" +
                    "Dieser Link ist 24 Stunden gültig.\n\n" +
                    "Falls Sie diese Anfrage nicht gestellt haben, ignorieren Sie diese E-Mail.\n\n" +
                    "Mit freundlichen Grüßen,\n" +
                    "PVS System",
                    userAccount.getUsername(), resetUrl);
            
            emailService.sendEmail(userAccount.getRecoveryEmail(), "PIN zurücksetzen", emailText);
            
            Notification.show("Eine E-Mail mit einem Reset-Link wurde an Ihre Recovery-E-Mail-Adresse gesendet.", 
                    5000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Fehler beim Senden der E-Mail: " + e.getMessage(), 
                    5000, Notification.Position.MIDDLE);
        }
    }
}

