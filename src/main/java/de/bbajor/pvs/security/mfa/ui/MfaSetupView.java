package de.bbajor.pvs.security.mfa.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaService;
import jakarta.annotation.security.RolesAllowed;

/**
 * View for setting up Multi-Factor Authentication (MFA) using TOTP.
 * 
 * <p>
 * This view allows users (specifically Super-Admin) to:
 * <ul>
 * <li>Generate a TOTP secret</li>
 * <li>Display a QR code for scanning with an authenticator app</li>
 * <li>Verify the setup by entering a TOTP code</li>
 * <li>Enable MFA for their account</li>
 * </ul>
 * </p>
 */
@Route("mfa-setup")
@PageTitle("MFA Einrichtung")
@RolesAllowed({ AppRoles.SUPER_ADMIN })
public class MfaSetupView extends VerticalLayout implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final MfaService mfaService;

    private final H2 title = new H2("Multi-Faktor-Authentifizierung einrichten");
    private final Paragraph instructions = new Paragraph(
            "Scannen Sie den QR-Code mit Ihrer Authenticator-App (z.B. Google Authenticator, Microsoft Authenticator) " +
            "und geben Sie dann einen Code ein, um die Einrichtung zu bestätigen.");
    private final Div qrCodeContainer = new Div();
    private final TextField verificationCodeField = new TextField("Verifizierungscode");
    private final Button verifyButton = new Button("Verifizieren und aktivieren");
    private final Button generateButton = new Button("Neuen QR-Code generieren");

    private String currentSecret;
    private UserAccount userAccount;

    public MfaSetupView(CurrentUser currentUser, UserAccountRepository userAccountRepository, MfaService mfaService) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.mfaService = mfaService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("600px");
        setAlignItems(Alignment.CENTER);

        verificationCodeField.setPlaceholder("000000");
        verificationCodeField.setMaxLength(6);
        verificationCodeField.setPattern("[0-9]{6}");
        verificationCodeField.setHelperText("6-stelliger Code aus Ihrer Authenticator-App");

        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        verifyButton.addClickListener(e -> verifyAndEnableMfa());

        generateButton.addClickListener(e -> generateNewSecret());

        add(title, instructions, qrCodeContainer, verificationCodeField, verifyButton, generateButton);

        // Load user account
        currentUser.get().ifPresent(user -> {
            userAccount = userAccountRepository.findByUsername(user.getUsername())
                    .orElse(null);
            if (userAccount != null) {
                if (userAccount.isMfaEnabled()) {
                    title.setText("MFA ist bereits aktiviert");
                    instructions.setText("Multi-Faktor-Authentifizierung ist für Ihr Konto bereits aktiviert.");
                    generateButton.setVisible(false);
                    verificationCodeField.setVisible(false);
                    verifyButton.setVisible(false);
                } else {
                    generateNewSecret();
                }
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUser.get().isPresent()) {
            event.forwardTo("/");
        }
    }

    private void generateNewSecret() {
        currentSecret = mfaService.generateSecret();
        
        currentUser.get().ifPresent(user -> {
            String qrCodeBase64 = mfaService.generateQrCode(user.getUsername(), currentSecret);
            
            // Display QR code
            Image qrCodeImage = new Image();
            qrCodeImage.setSrc("data:image/png;base64," + qrCodeBase64);
            qrCodeImage.setAlt("QR Code für MFA");
            qrCodeImage.setWidth("300px");
            qrCodeImage.setHeight("300px");
            
            qrCodeContainer.removeAll();
            qrCodeContainer.add(qrCodeImage);
            
            verificationCodeField.clear();
            verificationCodeField.setEnabled(true);
            verifyButton.setEnabled(true);
        });
    }

    private void verifyAndEnableMfa() {
        String code = verificationCodeField.getValue();
        
        if (code == null || code.length() != 6) {
            Notification.show("Bitte geben Sie einen 6-stelligen Code ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (currentSecret == null) {
            Notification.show("Bitte generieren Sie zuerst einen QR-Code", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (userAccount == null) {
            Notification.show("Benutzerkonto nicht gefunden", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Verify the code
        if (!mfaService.verifyCode(currentSecret, code)) {
            Notification.show("Ungültiger Code. Bitte versuchen Sie es erneut.", 3000, Notification.Position.MIDDLE);
            verificationCodeField.clear();
            return;
        }

        // Enable MFA for the user
        userAccount.setMfaSecret(currentSecret);
        userAccount.setMfaEnabled(true);
        userAccountRepository.save(userAccount);

        Notification.show("MFA erfolgreich aktiviert!", 3000, Notification.Position.MIDDLE);
        
        // Update UI
        title.setText("MFA ist aktiviert");
        instructions.setText("Multi-Faktor-Authentifizierung wurde erfolgreich für Ihr Konto aktiviert.");
        qrCodeContainer.removeAll();
        verificationCodeField.setVisible(false);
        verifyButton.setVisible(false);
        generateButton.setVisible(false);
    }
}
