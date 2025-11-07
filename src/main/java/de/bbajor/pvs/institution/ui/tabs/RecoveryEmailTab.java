package de.bbajor.pvs.institution.ui.tabs;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;

import de.bbajor.pvs.institution.model.InstitutionEmailContact;
import de.bbajor.pvs.institution.repository.InstitutionEmailContactRepository;
import de.bbajor.pvs.institution.service.InstitutionEmailContactService;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.email.EmailService;
import de.bbajor.pvs.security.email.service.SmtpConfigService;
import de.bbajor.pvs.security.mfa.service.MfaResetService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

    /**
     * Tab for configuring recovery email and PGP key for Super-Admin.
     * Used in SuperAdminSettingsView.
     */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class RecoveryEmailTab extends VerticalLayout {

    /**
     * Refreshes the tab content, especially SMTP configuration status.
     * Call this when the tab is selected to update the UI state.
     */
    public void refresh() {
        loadConfiguration();
    }

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final SmtpConfigService smtpConfigService;
    private final EmailService emailService;
    private final de.bbajor.pvs.security.email.service.EncryptedEmailService encryptedEmailService;
    private final MfaResetService mfaResetService;
    private final InstitutionEmailContactService emailContactService;
    private final InstitutionEmailContactRepository emailContactRepository;

    private EmailField recoveryEmailField;
    private TextArea pgpPublicKeyField;
    private Button saveButton;
    private Button testEmailButton;
    private Button verifyEmailButton;
    private Paragraph statusParagraph;
    private Paragraph pgpStatusParagraph;

    private UserAccount userAccount;
    private String verificationCode;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        H3 title = new H3("Recovery-E-Mail & PGP-Schlüssel");

        // Load user account
        currentUser.get().ifPresent(user -> {
            userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
        });

        if (userAccount == null) {
            add(new Paragraph("Benutzerkonto nicht gefunden."));
            return;
        }

        statusParagraph = new Paragraph();
        statusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");

        pgpStatusParagraph = new Paragraph();
        pgpStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Recovery Email Section
        recoveryEmailField = new EmailField("Recovery-E-Mail-Adresse");
        recoveryEmailField.setRequired(true);
        recoveryEmailField.setWidthFull();
        recoveryEmailField.setHelperText(
                "Diese E-Mail wird für MFA-Reset verwendet. " +
                "Der OpenPGP Public Key wird automatisch von keys.openpgp.org abgeholt, falls vorhanden. " +
                "Die E-Mails werden automatisch verschlüsselt, wenn ein Key verfügbar ist.");

        testEmailButton = new Button("Test-E-Mail senden", e -> sendTestEmail());
        testEmailButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        verifyEmailButton = new Button("E-Mail-Adresse verifizieren", e -> verifyEmail());
        verifyEmailButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        verifyEmailButton.setEnabled(false);

        // PGP Public Key Section
        pgpPublicKeyField = new TextArea("OpenPGP Public Key (optional)");
        pgpPublicKeyField.setWidthFull();
        pgpPublicKeyField.setHeight("200px");
        pgpPublicKeyField.setHelperText(
                "Optional: Fügen Sie hier Ihren OpenPGP Public Key ein, falls er noch nicht auf keys.openpgp.org hochgeladen wurde. " +
                "Wenn kein Key angegeben wird, wird automatisch versucht, den Key von keys.openpgp.org abzurufen. " +
                "Die Recovery-E-Mails werden automatisch verschlüsselt, wenn ein Key verfügbar ist.");

        saveButton = new Button("Speichern", e -> saveConfiguration());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        FormLayout formLayout = new FormLayout();
        formLayout.add(recoveryEmailField, 2);
        formLayout.add(testEmailButton, verifyEmailButton);
        formLayout.add(pgpPublicKeyField, 2);

        VerticalLayout recoverySection = new VerticalLayout(
                new Paragraph("Recovery-E-Mail-Konfiguration"),
                recoveryEmailField,
                new com.vaadin.flow.component.orderedlayout.HorizontalLayout(testEmailButton, verifyEmailButton),
                statusParagraph
        );
        recoverySection.setSpacing(true);

        VerticalLayout pgpSection = new VerticalLayout(
                new Paragraph("OpenPGP Public Key (für verschlüsselte E-Mails)"),
                pgpPublicKeyField,
                pgpStatusParagraph
        );
        pgpSection.setSpacing(true);

        add(title, recoverySection, pgpSection, saveButton);

        loadConfiguration();
    }

    private void loadConfiguration() {
        if (userAccount != null) {
            recoveryEmailField.setValue(userAccount.getRecoveryEmail() != null ? userAccount.getRecoveryEmail() : "");
            
            // Check if SMTP is configured
            var smtpConfig = smtpConfigService.getSmtpConfig();
            boolean smtpConfigured = smtpConfig.getEnabled() != null && smtpConfig.getEnabled()
                    && smtpConfig.getHost() != null && !smtpConfig.getHost().isEmpty();
            
            if (!smtpConfigured) {
                statusParagraph.setText("⚠️ Bitte konfigurieren Sie zuerst den SMTP-Server im Tab 'Mail-Server' und aktivieren Sie ihn.");
                testEmailButton.setEnabled(false);
            } else {
                statusParagraph.setText("✅ SMTP-Server ist konfiguriert. Sie können eine Test-E-Mail senden.");
                testEmailButton.setEnabled(true);
            }

            // Check if recovery email is verified
            if (userAccount.isRecoveryEmailVerified()) {
                statusParagraph.setText("✅ Recovery-E-Mail ist verifiziert: " + userAccount.getRecoveryEmail());
                verifyEmailButton.setEnabled(false);
            }

            // Load PGP key from InstitutionEmailContact if available
            emailContactRepository.findByEmail(userAccount.getRecoveryEmail())
                    .ifPresent(contact -> {
                        if (contact.getOpenpgpPublicKey() != null) {
                            pgpPublicKeyField.setValue(contact.getOpenpgpPublicKey());
                        }
                    });
            updatePgpStatus();
        }
    }

    private void updatePgpStatus() {
        // Check if PGP key exists for recovery email
        if (userAccount.getRecoveryEmail() != null && !userAccount.getRecoveryEmail().isEmpty()) {
            emailContactRepository.findByEmail(userAccount.getRecoveryEmail())
                    .ifPresentOrElse(
                            contact -> {
                                if (contact.getOpenpgpPublicKey() != null && !contact.getOpenpgpPublicKey().isEmpty()) {
                                    pgpStatusParagraph.setText("✅ PGP-Key in Datenbank hinterlegt: Key ID " + 
                                            (contact.getKeyId() != null ? contact.getKeyId() : "unbekannt") +
                                            ". E-Mails werden verschlüsselt.");
                                } else {
                                    pgpStatusParagraph.setText(
                                            "ℹ️ Kein PGP-Key in Datenbank. " +
                                            "Beim Versenden wird automatisch versucht, den Key von keys.openpgp.org abzurufen. " +
                                            "Falls kein Key gefunden wird, wird die E-Mail unverschlüsselt versendet.");
                                }
                            },
                            () -> pgpStatusParagraph.setText(
                                    "ℹ️ Kein PGP-Key in Datenbank. " +
                                    "Beim Versenden wird automatisch versucht, den Key von keys.openpgp.org abzurufen. " +
                                    "Falls kein Key gefunden wird, wird die E-Mail unverschlüsselt versendet.")
                    );
        } else {
            pgpStatusParagraph.setText("Bitte geben Sie zuerst eine Recovery-E-Mail-Adresse ein.");
        }
    }

    private void sendTestEmail() {
        String email = recoveryEmailField.getValue();
        if (email == null || email.trim().isEmpty()) {
            Notification.show("Bitte geben Sie eine E-Mail-Adresse ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Generate verification code
        SecureRandom random = new SecureRandom();
        byte[] codeBytes = new byte[6];
        random.nextBytes(codeBytes);
        verificationCode = Base64.getEncoder().withoutPadding().encodeToString(codeBytes).substring(0, 6).toUpperCase();

        try {
            String emailText = String.format(
                    "Hallo %s,\n\n" +
                    "Dies ist eine Test-E-Mail zur Verifizierung Ihrer Recovery-E-Mail-Adresse.\n\n" +
                    "Ihr Verifizierungscode lautet: %s\n\n" +
                    "Bitte geben Sie diesen Code in den Einstellungen ein, um Ihre E-Mail-Adresse zu verifizieren.\n\n" +
                    "Mit freundlichen Grüßen,\n" +
                    "PVS System",
                    userAccount.getUsername(), verificationCode);

            // Send email - encryption will be automatic if key is available (from DB or keys.openpgp.org)
            // If a temporary PGP key is provided in the textarea, use it (for keys not yet uploaded to keyserver)
            String temporaryPgpKey = pgpPublicKeyField.getValue();
            if (temporaryPgpKey != null && !temporaryPgpKey.trim().isEmpty()) {
                // Use temporary key if provided (e.g., key not yet uploaded to keys.openpgp.org)
                String fromAddress = smtpConfigService.getSmtpConfig().getFromAddress();
                if (fromAddress == null || fromAddress.isEmpty()) {
                    fromAddress = "noreply@pvs.local";
                }
                boolean sent = encryptedEmailService.sendEmail(email, "Recovery-E-Mail Verifizierung", emailText, fromAddress, temporaryPgpKey);
                if (!sent) {
                    throw new RuntimeException("Fehler beim Senden der E-Mail");
                }
            } else {
                // Use EmailService which will automatically fetch key from keys.openpgp.org if not in DB
                // The verification code will be encrypted automatically if a key is found
                emailService.sendEmail(email, "Recovery-E-Mail Verifizierung", emailText);
            }

            Notification.show("Test-E-Mail wurde gesendet. Bitte prüfen Sie Ihr Postfach und geben Sie den Verifizierungscode ein.", 
                    5000, Notification.Position.MIDDLE);
            verifyEmailButton.setEnabled(true);
        } catch (Exception e) {
            Notification.show("Fehler beim Senden der Test-E-Mail: " + e.getMessage(), 
                    5000, Notification.Position.MIDDLE);
        }
    }

    private void verifyEmail() {
        // For now, we'll use a simple dialog to enter the verification code
        // In a real implementation, you might want a proper dialog
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        com.vaadin.flow.component.textfield.TextField codeField = new com.vaadin.flow.component.textfield.TextField("Verifizierungscode");
        com.vaadin.flow.component.button.Button verifyButton = new com.vaadin.flow.component.button.Button("Verifizieren", e -> {
            String enteredCode = codeField.getValue();
            if (verificationCode != null && verificationCode.equals(enteredCode)) {
                userAccount.setRecoveryEmail(recoveryEmailField.getValue());
                userAccount.setRecoveryEmailVerified(true);
                userAccountRepository.save(userAccount);
                
                Notification.show("Recovery-E-Mail wurde erfolgreich verifiziert!", 3000, Notification.Position.MIDDLE);
                dialog.close();
                loadConfiguration();
            } else {
                Notification.show("Ungültiger Verifizierungscode", 3000, Notification.Position.MIDDLE);
            }
        });
        verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(codeField, verifyButton);
        dialog.open();
    }

    private void saveConfiguration() {
        if (userAccount == null) {
            return;
        }

        // Recovery email must be verified first
        if (userAccount.getRecoveryEmail() == null || !userAccount.isRecoveryEmailVerified()) {
            Notification.show("Bitte verifizieren Sie zuerst Ihre Recovery-E-Mail-Adresse", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Save PGP key if provided
        String pgpKey = pgpPublicKeyField.getValue();
        if (pgpKey != null && !pgpKey.trim().isEmpty()) {
            try {
                // Validate PGP key
                if (!emailContactService.isValidOpenPgpKey(pgpKey)) {
                    Notification.show("Ungültiger OpenPGP-Schlüssel", 3000, Notification.Position.MIDDLE);
                    return;
                }

                // Find or create email contact for recovery email (without institution for Super-Admin)
                InstitutionEmailContact contact = emailContactRepository.findByEmail(userAccount.getRecoveryEmail())
                        .orElseGet(() -> {
                            InstitutionEmailContact newContact = new InstitutionEmailContact();
                            newContact.setEmail(userAccount.getRecoveryEmail());
                            newContact.setDisplayName("Super-Admin Recovery");
                            newContact.setInstitution(null); // No institution for Super-Admin
                            newContact.setActive(true);
                            return newContact;
                        });

                contact.setOpenpgpPublicKey(pgpKey.trim());
                emailContactService.save(contact);

                Notification.show("PGP-Key wurde erfolgreich gespeichert", 3000, Notification.Position.MIDDLE);
                updatePgpStatus();
            } catch (Exception e) {
                Notification.show("Fehler beim Speichern des PGP-Keys: " + e.getMessage(), 
                        5000, Notification.Position.MIDDLE);
            }
        } else {
            // Remove PGP key if field is empty
            emailContactRepository.findByEmail(userAccount.getRecoveryEmail())
                    .ifPresent(contact -> {
                        contact.setOpenpgpPublicKey(null);
                        contact.setKeyId(null);
                        contact.setKeyFingerprint(null);
                        emailContactRepository.save(contact);
                        updatePgpStatus();
                    });
        }

        Notification.show("Konfiguration gespeichert", 3000, Notification.Position.MIDDLE);
    }
}

