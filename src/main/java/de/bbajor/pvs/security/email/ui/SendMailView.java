package de.bbajor.pvs.security.email.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.email.service.EncryptedEmailService;
import de.bbajor.pvs.security.email.service.SmtpConfigService;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

/**
 * View for manually sending encrypted emails.
 * Allows Super-Admins to send encrypted emails with optional OpenPGP public key.
 */
@Route("admin/send-mail")
@PageTitle("E-Mail senden")
@RolesAllowed({AppRoles.SUPER_ADMIN})
@RequiredArgsConstructor
public class SendMailView extends VerticalLayout {

    private final EncryptedEmailService encryptedEmailService;
    private final SmtpConfigService smtpConfigService;

    private EmailField recipientField;
    private TextField subjectField;
    private TextArea bodyArea;
    private TextArea pgpPublicKeyArea;
    private Button sendButton;
    private Paragraph statusParagraph;

    @jakarta.annotation.PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        add(new ViewToolbar("E-Mail senden"));

        H3 title = new H3("Verschlüsselte E-Mail senden");
        statusParagraph = new Paragraph();
        statusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Form fields
        recipientField = new EmailField("Empfänger E-Mail");
        recipientField.setRequired(true);
        recipientField.setPlaceholder("z.B. empfaenger@example.com");
        recipientField.setWidthFull();

        subjectField = new TextField("Betreff");
        subjectField.setRequired(true);
        subjectField.setPlaceholder("E-Mail-Betreff");
        subjectField.setWidthFull();

        bodyArea = new TextArea("Nachrichtentext");
        bodyArea.setRequired(true);
        bodyArea.setPlaceholder("Ihre Nachricht...");
        bodyArea.setWidthFull();
        bodyArea.setMinHeight("200px");

        pgpPublicKeyArea = new TextArea("Öffentlicher OpenPGP-Schlüssel (optional)");
        pgpPublicKeyArea.setPlaceholder("-----BEGIN PGP PUBLIC KEY BLOCK-----\n...\n-----END PGP PUBLIC KEY BLOCK-----");
        pgpPublicKeyArea.setWidthFull();
        pgpPublicKeyArea.setMinHeight("150px");
        pgpPublicKeyArea.setHelperText(
                "Wenn kein Schlüssel angegeben wird, wird nach einem gespeicherten Schlüssel für die Empfänger-E-Mail gesucht. " +
                "Falls vorhanden, wird die E-Mail automatisch verschlüsselt.");

        sendButton = new Button("Verschlüsselt senden");
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendEmail());

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(recipientField, 2);
        formLayout.add(subjectField, 2);
        formLayout.add(bodyArea, 2);
        formLayout.add(pgpPublicKeyArea, 2);

        add(title, statusParagraph, formLayout, sendButton);

        // Check SMTP configuration status
        updateStatus();
    }

    private void updateStatus() {
        var config = smtpConfigService.getSmtpConfig();
        boolean enabled = config.getEnabled() != null && config.getEnabled();
        
        if (!enabled) {
            statusParagraph.setText("⚠️ E-Mail-Versand ist deaktiviert. Bitte aktivieren Sie den SMTP-Server in den Einstellungen.");
            statusParagraph.getStyle().set("color", "var(--lumo-error-color)");
            sendButton.setEnabled(false);
        } else {
            statusParagraph.setText("✓ E-Mail-Versand ist aktiviert. E-Mails werden über " + 
                    (config.getHost() != null ? config.getHost() : "konfigurierten Server") + " gesendet.");
            statusParagraph.getStyle().set("color", "var(--lumo-success-color)");
            sendButton.setEnabled(true);
        }
    }

    private void sendEmail() {
        // Validate fields
        if (recipientField.getValue() == null || recipientField.getValue().isEmpty()) {
            Notification.show("Bitte geben Sie eine Empfänger-E-Mail-Adresse ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (subjectField.getValue() == null || subjectField.getValue().isEmpty()) {
            Notification.show("Bitte geben Sie einen Betreff ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (bodyArea.getValue() == null || bodyArea.getValue().isEmpty()) {
            Notification.show("Bitte geben Sie eine Nachricht ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get from address from SMTP config
        var config = smtpConfigService.getSmtpConfig();
        String fromAddress = config.getFromAddress();
        if (fromAddress == null || fromAddress.isEmpty()) {
            Notification.show("Fehler: Keine Absender-E-Mail-Adresse konfiguriert. Bitte konfigurieren Sie den SMTP-Server.", 
                    5000, Notification.Position.MIDDLE);
            return;
        }

        // Get optional PGP key
        String pgpKey = pgpPublicKeyArea.getValue();
        if (pgpKey != null && pgpKey.trim().isEmpty()) {
            pgpKey = null;
        }

        // Send email
        try {
            boolean sent = encryptedEmailService.sendEmail(
                    recipientField.getValue(),
                    subjectField.getValue(),
                    bodyArea.getValue(),
                    fromAddress,
                    pgpKey != null ? pgpKey.trim() : null
            );

            if (sent) {
                Notification notification = Notification.show(
                        "E-Mail erfolgreich gesendet an " + recipientField.getValue(), 
                        3000, 
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                // Clear form
                recipientField.clear();
                subjectField.clear();
                bodyArea.clear();
                pgpPublicKeyArea.clear();
            } else {
                Notification notification = Notification.show(
                        "Fehler beim Senden der E-Mail. Bitte prüfen Sie die SMTP-Konfiguration.", 
                        5000, 
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Fehler beim Senden der E-Mail: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

