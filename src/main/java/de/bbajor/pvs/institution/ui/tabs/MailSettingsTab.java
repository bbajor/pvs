package de.bbajor.pvs.institution.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import de.bbajor.pvs.institution.model.EmailEncryptionMethod;
import de.bbajor.pvs.security.email.model.SmtpConfig;
import de.bbajor.pvs.security.email.model.SmtpSecurityMethod;
import de.bbajor.pvs.security.email.service.OpenPgpService;
import de.bbajor.pvs.security.email.service.SmtpConfigService;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tab component for SMTP mail server configuration.
 * Used in SuperAdminSettingsView.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MailSettingsTab extends VerticalLayout {

    private final SmtpConfigService smtpConfigService;
    private final OpenPgpService openPgpService;

    private TextField hostField;
    private IntegerField portField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField fromAddressField;
    private ComboBox<SmtpSecurityMethod> securityMethodComboBox;
    private ComboBox<EmailEncryptionMethod> defaultEncryptionMethodComboBox;
    private Checkbox enabledCheckbox;
    
    // OpenPGP signing fields
    private Upload privateKeyUpload;
    private final AtomicReference<byte[]> pendingPrivateKeyUpload = new AtomicReference<>();
    private PasswordField privateKeyPassphraseField;
    private Paragraph privateKeyStatusParagraph;
    private Button removePrivateKeyButton;
    
    // OpenPGP public key fields (optional, for Autocrypt header)
    private Upload publicKeyUpload;
    private final AtomicReference<byte[]> pendingPublicKeyUpload = new AtomicReference<>();
    private Paragraph publicKeyStatusParagraph;
    private Button removePublicKeyButton;
    
    private Button saveButton;
    private Button testButton;
    private Paragraph statusParagraph;
    
    private String uploadedPrivateKey; // Store uploaded key temporarily
    private String uploadedPublicKey; // Store uploaded public key temporarily

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        H3 title = new H3("SMTP-Mail-Server-Konfiguration");
        statusParagraph = new Paragraph();
        statusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Create form
        hostField = new TextField("SMTP-Host");
        hostField.setRequired(true);
        hostField.setPlaceholder("z.B. smtp.example.com");
        hostField.setWidthFull();

        portField = new IntegerField("SMTP-Port");
        portField.setRequired(true);
        portField.setValue(587);
        portField.setMin(1);
        portField.setMax(65535);
        portField.setWidthFull();

        usernameField = new TextField("Benutzername");
        usernameField.setPlaceholder("SMTP-Benutzername");
        usernameField.setWidthFull();

        passwordField = new PasswordField("Passwort");
        passwordField.setPlaceholder("SMTP-Passwort (leer lassen, um nicht zu ändern)");
        passwordField.setWidthFull();

        fromAddressField = new TextField("Absender-E-Mail-Adresse");
        fromAddressField.setPlaceholder("z.B. noreply@example.com");
        fromAddressField.setWidthFull();

        securityMethodComboBox = new ComboBox<>("SMTP-Verschlüsselungsmethode");
        securityMethodComboBox.setItems(SmtpSecurityMethod.values());
        securityMethodComboBox.setItemLabelGenerator(SmtpSecurityMethod::getDisplayName);
        securityMethodComboBox.setValue(SmtpSecurityMethod.STARTTLS);
        securityMethodComboBox.setWidthFull();
        securityMethodComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                portField.setValue(e.getValue().getDefaultPort());
            }
        });

        defaultEncryptionMethodComboBox = new ComboBox<>("Standard E-Mail-Verschlüsselung");
        defaultEncryptionMethodComboBox.setItems(EmailEncryptionMethod.values());
        defaultEncryptionMethodComboBox.setItemLabelGenerator(method -> method.getDisplayName() + " - " + method.getDescription());
        defaultEncryptionMethodComboBox.setWidthFull();
        defaultEncryptionMethodComboBox.setRequired(true);
        defaultEncryptionMethodComboBox.setValue(EmailEncryptionMethod.NONE); // Default value
        defaultEncryptionMethodComboBox.setHelperText(
                "Diese Methode wird verwendet, wenn für einen Empfänger keine spezifische Verschlüsselungsmethode konfiguriert ist. " +
                "Kann pro E-Mail-Kontakt überschrieben werden.");

        enabledCheckbox = new Checkbox("E-Mail-Versand aktiviert");
        enabledCheckbox.setValue(false);

        // OpenPGP Signing Section
        H3 signingTitle = new H3("OpenPGP-Signatur (Digitale Signatur)");
        signingTitle.getStyle().set("margin-top", "2em");
        
        Paragraph signingInfo = new Paragraph(
                "Laden Sie hier Ihren privaten OpenPGP-Schlüssel hoch, um ausgehende E-Mails digital zu signieren. " +
                "Der Schlüssel wird verschlüsselt gespeichert. Sie können den Schlüssel in Thunderbird erstellen " +
                "und dann exportieren (Einstellungen → OpenPGP → Schlüssel verwalten → Schlüssel exportieren → Privater Schlüssel).");
        signingInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        signingInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        privateKeyUpload = new Upload(new InMemoryUploadHandler((metadata, data) -> {
            pendingPrivateKeyUpload.set(data);
            getUI().ifPresent(ui -> ui.access(this::handlePrivateKeyUpload));
        }));
        privateKeyUpload.setAcceptedFileTypes(".asc", ".key", "application/pgp-keys", "application/pgp");
        privateKeyUpload.setMaxFileSize(100 * 1024); // 100 KB max
        privateKeyUpload.setDropLabel(new com.vaadin.flow.component.html.Span("Privaten Schlüssel hier ablegen oder klicken zum Auswählen"));
        privateKeyUpload.addFileRejectedListener(e -> {
            Notification notification = Notification.show(
                    "Fehler beim Hochladen: " + e.getErrorMessage(), 
                    5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        
        privateKeyPassphraseField = new PasswordField("Passphrase für privaten Schlüssel (optional)");
        privateKeyPassphraseField.setPlaceholder("Nur erforderlich, wenn der Schlüssel passwortgeschützt ist");
        privateKeyPassphraseField.setWidthFull();
        privateKeyPassphraseField.setHelperText(
                "Geben Sie die Passphrase ein, wenn Ihr privater Schlüssel passwortgeschützt ist. " +
                "Die Passphrase wird verschlüsselt gespeichert.");
        
        privateKeyStatusParagraph = new Paragraph();
        privateKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
        privateKeyStatusParagraph.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        removePrivateKeyButton = new Button("Privaten Schlüssel entfernen");
        removePrivateKeyButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removePrivateKeyButton.addClickListener(e -> removePrivateKey());
        removePrivateKeyButton.setVisible(false);
        
        // Public key section (optional, for Autocrypt header)
        H3 publicKeyTitle = new H3("Öffentlicher OpenPGP-Schlüssel (optional)");
        publicKeyTitle.getStyle().set("margin-top", "2em");
        
        Paragraph publicKeyInfo = new Paragraph(
                "Optional: Laden Sie hier Ihren öffentlichen OpenPGP-Schlüssel hoch. " +
                "Dieser wird für den Autocrypt-Header in signierten E-Mails verwendet und verbessert die Thunderbird-Kompatibilität. " +
                "Wenn nicht hochgeladen, wird der öffentliche Schlüssel automatisch aus dem privaten Schlüssel extrahiert. " +
                "In Thunderbird: Einstellungen → OpenPGP → Schlüssel verwalten → Schlüssel exportieren → Öffentlicher Schlüssel.");
        publicKeyInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        publicKeyInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        publicKeyUpload = new Upload(new InMemoryUploadHandler((metadata, data) -> {
            pendingPublicKeyUpload.set(data);
            getUI().ifPresent(ui -> ui.access(this::handlePublicKeyUpload));
        }));
        publicKeyUpload.setAcceptedFileTypes(".asc", ".key", "application/pgp-keys", "application/pgp");
        publicKeyUpload.setMaxFileSize(100 * 1024); // 100 KB max
        publicKeyUpload.setDropLabel(new com.vaadin.flow.component.html.Span("Öffentlichen Schlüssel hier ablegen oder klicken zum Auswählen"));
        publicKeyUpload.addFileRejectedListener(e -> {
            Notification notification = Notification.show(
                    "Fehler beim Hochladen: " + e.getErrorMessage(), 
                    5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });
        
        publicKeyStatusParagraph = new Paragraph();
        publicKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
        publicKeyStatusParagraph.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        removePublicKeyButton = new Button("Öffentlichen Schlüssel entfernen");
        removePublicKeyButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removePublicKeyButton.addClickListener(e -> removePublicKey());
        removePublicKeyButton.setVisible(false);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(hostField, 2);
        formLayout.add(portField);
        formLayout.add(usernameField, 2);
        formLayout.add(passwordField, 2);
        formLayout.add(fromAddressField, 2);
        formLayout.add(securityMethodComboBox);
        formLayout.add(defaultEncryptionMethodComboBox, 2);
        formLayout.add(enabledCheckbox);

        saveButton = new Button("Speichern");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> saveConfiguration());

        testButton = new Button("Verbindung testen");
        testButton.addClickListener(e -> testConnection());

        // Test button next to form
        HorizontalLayout testButtonLayout = new HorizontalLayout(testButton);
        testButtonLayout.setSpacing(true);

        // OpenPGP signing section layout
        VerticalLayout signingLayout = new VerticalLayout();
        signingLayout.setSpacing(true);
        signingLayout.setPadding(false);
        signingLayout.add(signingTitle, signingInfo, privateKeyUpload, privateKeyPassphraseField, 
                privateKeyStatusParagraph, removePrivateKeyButton,
                publicKeyTitle, publicKeyInfo, publicKeyUpload, 
                publicKeyStatusParagraph, removePublicKeyButton);

        // Create a container for the main content
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSpacing(true);
        contentLayout.setPadding(false);
        contentLayout.setWidthFull();
        contentLayout.add(title, statusParagraph, formLayout, testButtonLayout, 
                new Hr(), signingLayout);
        
        // Add content layout with flex grow to push save button down
        add(contentLayout);
        setFlexGrow(1.0, contentLayout);
        
        // Add save button at the bottom - always visible
        HorizontalLayout saveButtonContainer = new HorizontalLayout(saveButton);
        saveButtonContainer.setWidthFull();
        saveButtonContainer.setJustifyContentMode(JustifyContentMode.END);
        saveButtonContainer.getStyle().set("margin-top", "2em");
        saveButtonContainer.getStyle().set("padding-top", "1em");
        saveButtonContainer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        add(saveButtonContainer);

        loadConfiguration();
    }

    private void loadConfiguration() {
        SmtpConfig config = smtpConfigService.getSmtpConfig();
        
        // Load values from DB, fallback to ENV variables if DB value is empty/null
        // Note: Password and private key are NEVER read from ENV for security reasons
        String host = config.getHost();
        if (host == null || host.isEmpty()) {
            host = getEnvValue("SMTP_HOST", "");
        }
        hostField.setValue(host);
        
        Integer port = config.getPort();
        if (port == null) {
            String envPort = getEnvValue("SMTP_PORT", null);
            if (envPort != null && !envPort.isEmpty()) {
                try {
                    port = Integer.parseInt(envPort);
                } catch (NumberFormatException e) {
                    port = 587; // Default
                }
            } else {
                port = 587; // Default
            }
        }
        portField.setValue(port);
        
        String username = config.getUsername();
        if (username == null || username.isEmpty()) {
            username = getEnvValue("SMTP_USERNAME", "");
        }
        usernameField.setValue(username);
        
        passwordField.setValue(""); // Don't show password - never read from ENV
        
        String fromAddress = config.getFromAddress();
        if (fromAddress == null || fromAddress.isEmpty()) {
            fromAddress = getEnvValue("SMTP_FROM_ADDRESS", "");
        }
        fromAddressField.setValue(fromAddress);
        
        // Set security method - migrate from useTls if needed, fallback to ENV
        SmtpSecurityMethod securityMethod = config.getSecurityMethod();
        if (securityMethod == null) {
            // Migration: convert old useTls to securityMethod
            if (config.getUseTls() != null && config.getUseTls()) {
                securityMethod = SmtpSecurityMethod.STARTTLS;
            } else {
                // Try to read from ENV
                String envSecurityMethod = getEnvValue("SMTP_SECURITY_METHOD", null);
                if (envSecurityMethod != null && !envSecurityMethod.isEmpty()) {
                    try {
                        securityMethod = SmtpSecurityMethod.valueOf(envSecurityMethod.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        securityMethod = SmtpSecurityMethod.STARTTLS; // Default
                    }
                } else {
                    securityMethod = SmtpSecurityMethod.STARTTLS; // Default
                }
            }
        }
        securityMethodComboBox.setValue(securityMethod);
        
        // Load default encryption method
        EmailEncryptionMethod defaultEncryptionMethod = config.getDefaultEncryptionMethod();
        if (defaultEncryptionMethod == null) {
            defaultEncryptionMethod = EmailEncryptionMethod.NONE; // Default
        }
        defaultEncryptionMethodComboBox.setValue(defaultEncryptionMethod);
        
        // Important: enabled must be explicitly checked - default to false if null
        Boolean enabled = config.getEnabled();
        if (enabled == null) {
            String envEnabled = getEnvValue("SMTP_ENABLED", null);
            if (envEnabled != null && !envEnabled.isEmpty()) {
                enabled = Boolean.parseBoolean(envEnabled);
            } else {
                enabled = false; // Default
            }
        }
        enabledCheckbox.setValue(enabled);
        
        if (config.getId() != null) {
            statusParagraph.setText("Konfiguration geladen. Passwort wird nicht angezeigt.");
        } else {
            String envInfo = hasEnvConfig() ? " (ENV-Variablen gefunden)" : "";
            statusParagraph.setText("Keine Konfiguration vorhanden. Bitte konfigurieren Sie den SMTP-Server." + envInfo);
        }
        
        // Load private key status
        updatePrivateKeyStatus(config);
        
        // Load public key status
        updatePublicKeyStatus(config);
    }
    
    /**
     * Reads an environment variable value.
     * 
     * @param envVarName the environment variable name
     * @param defaultValue the default value if the variable is not set
     * @return the environment variable value or the default value
     */
    private String getEnvValue(String envVarName, String defaultValue) {
        String value = System.getenv(envVarName);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Checks if any SMTP-related environment variables are set.
     * 
     * @return true if at least one SMTP ENV variable is set
     */
    private boolean hasEnvConfig() {
        return getEnvValue("SMTP_HOST", null) != null
                || getEnvValue("SMTP_PORT", null) != null
                || getEnvValue("SMTP_USERNAME", null) != null
                || getEnvValue("SMTP_FROM_ADDRESS", null) != null
                || getEnvValue("SMTP_SECURITY_METHOD", null) != null
                || getEnvValue("SMTP_ENABLED", null) != null;
    }
    
    private void updatePrivateKeyStatus(SmtpConfig config) {
        if (config.getOpenpgpPrivateKey() != null && !config.getOpenpgpPrivateKey().isEmpty()) {
            privateKeyStatusParagraph.setText("✅ Privater Schlüssel ist hinterlegt. E-Mails werden digital signiert.");
            privateKeyStatusParagraph.getStyle().set("color", "var(--lumo-success-color)");
            removePrivateKeyButton.setVisible(true);
            privateKeyPassphraseField.setValue(""); // Don't show passphrase
        } else {
            privateKeyStatusParagraph.setText("ℹ️ Kein privater Schlüssel hinterlegt. E-Mails werden nicht signiert.");
            privateKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
            removePrivateKeyButton.setVisible(false);
        }
    }
    
    private void handlePrivateKeyUpload() {
        byte[] data = pendingPrivateKeyUpload.getAndSet(null);
        if (data == null || data.length == 0) {
            Notification notification = Notification.show(
                    "Fehler beim Lesen der Datei: Datei ist leer oder konnte nicht gelesen werden.",
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPrivateKey = null;
            return;
        }

        String privateKeyContent = new String(data, StandardCharsets.UTF_8);

        if (!privateKeyContent.contains("-----BEGIN PGP PRIVATE KEY BLOCK-----")
                && !privateKeyContent.contains("-----BEGIN PGP SECRET KEY BLOCK-----")) {
            Notification notification = Notification.show(
                    "Die Datei scheint kein privater OpenPGP-Schlüssel zu sein. " +
                    "Bitte stellen Sie sicher, dass Sie den privaten Schlüssel exportiert haben.",
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPrivateKey = null;
            return;
        }

        try {
            openPgpService.parseSecretKey(privateKeyContent);
            uploadedPrivateKey = privateKeyContent;

            Notification notification = Notification.show(
                    "Privater Schlüssel erfolgreich hochgeladen. Bitte speichern Sie die Konfiguration.",
                    3000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            privateKeyStatusParagraph.setText("✅ Privater Schlüssel hochgeladen. Bitte speichern Sie die Konfiguration.");
            privateKeyStatusParagraph.getStyle().set("color", "var(--lumo-success-color)");
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Fehler beim Validieren des privaten Schlüssels: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPrivateKey = null;
        }
    }
    
    private void removePrivateKey() {
        uploadedPrivateKey = null;
        pendingPrivateKeyUpload.set(null);
        privateKeyPassphraseField.setValue("");
        privateKeyStatusParagraph.setText("ℹ️ Privater Schlüssel wird beim Speichern entfernt.");
        privateKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
        removePrivateKeyButton.setVisible(false);
    }
    
    private void updatePublicKeyStatus(SmtpConfig config) {
        if (config.getOpenpgpPublicKey() != null && !config.getOpenpgpPublicKey().isEmpty()) {
            publicKeyStatusParagraph.setText("✅ Öffentlicher Schlüssel ist hinterlegt. Wird für Autocrypt-Header verwendet.");
            publicKeyStatusParagraph.getStyle().set("color", "var(--lumo-success-color)");
            removePublicKeyButton.setVisible(true);
        } else {
            publicKeyStatusParagraph.setText("ℹ️ Kein öffentlicher Schlüssel hinterlegt. Wird automatisch aus dem privaten Schlüssel extrahiert.");
            publicKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
            removePublicKeyButton.setVisible(false);
        }
    }
    
    private void handlePublicKeyUpload() {
        byte[] data = pendingPublicKeyUpload.getAndSet(null);
        if (data == null || data.length == 0) {
            Notification notification = Notification.show(
                    "Fehler beim Lesen der Datei: Datei ist leer oder konnte nicht gelesen werden.",
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPublicKey = null;
            return;
        }

        String publicKeyContent = new String(data, StandardCharsets.UTF_8);

        if (!publicKeyContent.contains("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
            Notification notification = Notification.show(
                    "Die Datei scheint kein öffentlicher OpenPGP-Schlüssel zu sein. " +
                    "Bitte stellen Sie sicher, dass Sie den öffentlichen Schlüssel exportiert haben.",
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPublicKey = null;
            return;
        }

        try {
            openPgpService.parsePublicKey(publicKeyContent);
            uploadedPublicKey = publicKeyContent;

            Notification notification = Notification.show(
                    "Öffentlicher Schlüssel erfolgreich hochgeladen. Bitte speichern Sie die Konfiguration.",
                    3000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            publicKeyStatusParagraph.setText("✅ Öffentlicher Schlüssel hochgeladen. Bitte speichern Sie die Konfiguration.");
            publicKeyStatusParagraph.getStyle().set("color", "var(--lumo-success-color)");
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Fehler beim Validieren des öffentlichen Schlüssels: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            uploadedPublicKey = null;
        }
    }
    
    private void removePublicKey() {
        uploadedPublicKey = null;
        pendingPublicKeyUpload.set(null);
        publicKeyStatusParagraph.setText("ℹ️ Öffentlicher Schlüssel wird beim Speichern entfernt.");
        publicKeyStatusParagraph.getStyle().set("color", "var(--lumo-secondary-text-color)");
        removePublicKeyButton.setVisible(false);
    }

    private void saveConfiguration() {
        try {
            SmtpConfig config = smtpConfigService.getSmtpConfig();
            
            if (hostField.getValue() == null || hostField.getValue().isEmpty()) {
                Notification.show("SMTP-Host ist erforderlich", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            config.setHost(hostField.getValue());
            config.setPort(portField.getValue() != null ? portField.getValue() : 587);
            config.setUsername(usernameField.getValue());
            
            // Only update password if a new one was entered
            if (passwordField.getValue() != null && !passwordField.getValue().isEmpty()) {
                config.setPassword(passwordField.getValue());
            }
            
            config.setFromAddress(fromAddressField.getValue());
            
            // Set security method
            SmtpSecurityMethod selectedMethod = securityMethodComboBox.getValue();
            if (selectedMethod != null) {
                config.setSecurityMethod(selectedMethod);
                // Also set useTls for backward compatibility (deprecated)
                config.setUseTls(selectedMethod != SmtpSecurityMethod.NONE);
            } else {
                config.setSecurityMethod(SmtpSecurityMethod.STARTTLS);
                config.setUseTls(true);
            }
            
            // Ensure enabled is explicitly set (not null)
            config.setEnabled(enabledCheckbox.getValue() != null && enabledCheckbox.getValue());
            
            // Set default encryption method
            EmailEncryptionMethod selectedEncryptionMethod = defaultEncryptionMethodComboBox.getValue();
            if (selectedEncryptionMethod != null) {
                config.setDefaultEncryptionMethod(selectedEncryptionMethod);
            } else {
                config.setDefaultEncryptionMethod(EmailEncryptionMethod.NONE);
            }
            
            // Handle private key upload
            if (uploadedPrivateKey != null && !uploadedPrivateKey.isEmpty()) {
                // New private key uploaded - always set it
                config.setOpenpgpPrivateKey(uploadedPrivateKey);
                // Set passphrase if provided, otherwise clear it (key might not be password-protected)
                if (privateKeyPassphraseField.getValue() != null 
                        && !privateKeyPassphraseField.getValue().isEmpty()) {
                    config.setOpenpgpPrivateKeyPassphrase(privateKeyPassphraseField.getValue());
                } else {
                    // No passphrase provided - clear it (for non-password-protected keys)
                    config.setOpenpgpPrivateKeyPassphrase(null);
                }
            } else if (config.getId() != null) {
                // No new key uploaded - check if user wants to remove existing key
                if (privateKeyStatusParagraph.getText() != null 
                        && privateKeyStatusParagraph.getText().contains("wird beim Speichern entfernt")) {
                    // User wants to remove the private key
                    config.setOpenpgpPrivateKey(null);
                    config.setOpenpgpPrivateKeyPassphrase(null);
                } else {
                    // Keep existing private key and passphrase (handled by SmtpConfigService)
                    // But if passphrase was changed, update it
                    if (privateKeyPassphraseField.getValue() != null 
                            && !privateKeyPassphraseField.getValue().isEmpty()) {
                        // User updated the passphrase
                        config.setOpenpgpPrivateKeyPassphrase(privateKeyPassphraseField.getValue());
                    }
                    // If passphrase field is empty and key exists, keep old passphrase (handled by SmtpConfigService)
                }
            }
            
            // Handle public key upload
            if (uploadedPublicKey != null && !uploadedPublicKey.isEmpty()) {
                // New public key uploaded - always set it
                config.setOpenpgpPublicKey(uploadedPublicKey);
            } else if (config.getId() != null) {
                // No new key uploaded - check if user wants to remove existing key
                if (publicKeyStatusParagraph.getText() != null 
                        && publicKeyStatusParagraph.getText().contains("wird beim Speichern entfernt")) {
                    // User wants to remove the public key
                    config.setOpenpgpPublicKey(null);
                }
                // Otherwise keep existing public key (no need to update if not changed)
            }

            smtpConfigService.saveSmtpConfig(config);
            
            // Clear uploaded keys after saving
            uploadedPrivateKey = null;
            uploadedPublicKey = null;
            
            Notification notification = Notification.show("SMTP-Konfiguration gespeichert", 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Reload configuration to ensure UI reflects saved state
            loadConfiguration();
            passwordField.setValue(""); // Clear password field after saving
        } catch (Exception e) {
            Notification notification = Notification.show("Fehler beim Speichern: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void testConnection() {
        try {
            // Create test config from current form values
            SmtpConfig testConfig = new SmtpConfig();
            testConfig.setHost(hostField.getValue());
            testConfig.setPort(portField.getValue() != null ? portField.getValue() : 587);
            testConfig.setUsername(usernameField.getValue());
            
            // Use password from field if provided, otherwise get from saved config
            if (passwordField.getValue() != null && !passwordField.getValue().isEmpty()) {
                testConfig.setPassword(passwordField.getValue());
            } else {
                // Get saved password for testing
                SmtpConfig savedConfig = smtpConfigService.getSmtpConfig();
                testConfig.setPassword(savedConfig.getPassword());
            }
            
            // Set security method
            SmtpSecurityMethod selectedMethod = securityMethodComboBox.getValue();
            if (selectedMethod != null) {
                testConfig.setSecurityMethod(selectedMethod);
                testConfig.setUseTls(selectedMethod != SmtpSecurityMethod.NONE);
            } else {
                testConfig.setSecurityMethod(SmtpSecurityMethod.STARTTLS);
                testConfig.setUseTls(true);
            }
            
            // Disable button during test
            testButton.setEnabled(false);
            testButton.setText("Teste Verbindung...");
            
            // Perform connection test (this may take a few seconds)
            boolean isConnected = smtpConfigService.testConnection(testConfig);
            
            // Re-enable button
            testButton.setEnabled(true);
            testButton.setText("Verbindung testen");
            
            if (isConnected) {
                Notification notification = Notification.show(
                        "Verbindung zum SMTP-Server erfolgreich hergestellt", 
                        5000, 
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification notification = Notification.show(
                        "Verbindung zum SMTP-Server fehlgeschlagen. " +
                        "Bitte überprüfen Sie Host, Port, Benutzername, Passwort und Verschlüsselungsmethode.", 
                        8000, 
                        Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            testButton.setEnabled(true);
            testButton.setText("Verbindung testen");
            Notification notification = Notification.show(
                    "Fehler beim Testen der Verbindung: " + e.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

