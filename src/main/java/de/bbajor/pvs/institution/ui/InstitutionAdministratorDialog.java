package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dialog for creating administrators for an institution.
 * Sends initial password via email and requires password change on first login.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InstitutionAdministratorDialog extends Dialog {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private TextField usernameField;
    private TextField fullNameField;
    private EmailField emailField;
    private Button saveButton;
    private Button cancelButton;

    private Institution institution;

    public void openForInstitution(Institution institution) {
        this.institution = institution;
        
        // Set InstitutionContext for the institution we're creating an admin for
        // This ensures that any service calls (e.g., email service) use the correct institution context
        if (institution != null && institution.getId() != null) {
            InstitutionContext.setInstitutionId(institution.getId());
            log.debug("InstitutionContext set for InstitutionAdministratorDialog: {}", institution.getId());
        }
        
        removeAll();
        
        H3 title = new H3("Administrator für " + institution.getInstitutionName() + " anlegen");
        
        usernameField = new TextField("Benutzername");
        usernameField.setRequired(true);
        usernameField.setWidthFull();
        
        fullNameField = new TextField("Vollständiger Name");
        fullNameField.setWidthFull();
        
        emailField = new EmailField("E-Mail-Adresse");
        emailField.setRequired(true);
        emailField.setWidthFull();
        emailField.setHelperText("Das initiale Passwort wird an diese E-Mail-Adresse gesendet.");
        
        saveButton = new Button("Administrator anlegen", e -> createAdministrator());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        cancelButton = new Button("Abbrechen", e -> close());
        
        FormLayout formLayout = new FormLayout();
        formLayout.add(usernameField, fullNameField, emailField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        
        VerticalLayout content = new VerticalLayout(title, formLayout, buttonLayout);
        content.setSpacing(true);
        content.setPadding(true);
        
        add(content);
        
        setWidth("600px");
        setCloseOnOutsideClick(false);
        open();
    }

    private void createAdministrator() {
        String username = usernameField.getValue();
        String email = emailField.getValue();
        
        if (username == null || username.trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Benutzernamen ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        if (email == null || email.trim().isEmpty()) {
            Notification.show("Bitte geben Sie eine E-Mail-Adresse ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        // Check if username already exists
        if (userAccountRepository.findByUsername(username).isPresent()) {
            Notification.show("Benutzername existiert bereits", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        try {
            // Generate random password
            String initialPassword = generateRandomPassword();
            
            // Create user account
            UserAccount admin = new UserAccount();
            admin.setUsername(username);
            admin.setFullName(fullNameField.getValue());
            admin.setEmail(email);
            admin.setInstitution(institution);
            admin.setPasswordHash(passwordEncoder.encode(initialPassword));
            admin.getRoles().add(AppRoles.INSTITUTION_ADMIN);
            admin.setEnabled(true);
            admin.setPasswordChangeRequired(true); // Force password change on first login
            admin.setInitialPasswordSet(false);
            admin.setUserId(UUID.randomUUID().toString());
            
            userAccountRepository.save(admin);
            
            // Send email with initial password
            sendInitialPasswordEmail(email, username, initialPassword);
            
            Notification.show("Administrator wurde erfolgreich angelegt. Das initiale Passwort wurde per E-Mail versendet.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            // Clear InstitutionContext after dialog closes (it was only set for this operation)
            // The context will be restored by VaadinInstitutionContextInitializer on next navigation
            close();
        } catch (Exception e) {
            Notification.show("Fehler beim Anlegen des Administrators: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String generateRandomPassword() {
        // Generate a secure random password (12 characters, alphanumeric + special chars)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private void sendInitialPasswordEmail(String email, String username, String password) {
        String messageText = String.format(
                "Hallo %s,\n\n" +
                "Ihr Administrator-Konto wurde für die Institution '%s' angelegt.\n\n" +
                "Ihre Anmeldedaten:\n" +
                "Benutzername: %s\n" +
                "Initiales Passwort: %s\n\n" +
                "Bitte ändern Sie Ihr Passwort beim ersten Login.\n" +
                "Nach der Passwort-Änderung müssen Sie die Multi-Faktor-Authentifizierung (MFA) einrichten.\n\n" +
                "Mit freundlichen Grüßen,\n" +
                "PVS System",
                username, institution.getInstitutionName(), username, password);
        
        emailService.sendEmail(email, "Administrator-Konto angelegt - Initiales Passwort", messageText);
    }
}

