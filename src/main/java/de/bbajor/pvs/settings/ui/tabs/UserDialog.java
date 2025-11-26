package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

import java.util.UUID;

/**
 * Dialog for creating and editing users.
 */
public class UserDialog extends Dialog {

    private final UserAccountRepository userAccountRepository;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;
    private UserAccount userAccount;
    private Runnable onSaveCallback;

    private TextField usernameField;
    private TextField fullNameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private Select<String> roleSelect;
    private ComboBox<Location> locationComboBox;
    private Checkbox enabledCheckbox;
    private Button saveButton;

    public UserDialog(
            UserAccountRepository userAccountRepository,
            LocationService locationService,
            InstitutionRepository institutionRepository,
            UserAccount userAccount) {
        this.userAccountRepository = userAccountRepository;
        this.locationService = locationService;
        this.institutionRepository = institutionRepository;
        this.userAccount = userAccount != null ? userAccount : new UserAccount();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("500px");

        initializeDialog();
    }

    private void initializeDialog() {
        String titleText = userAccount.getId() != null ? "Benutzer bearbeiten" : "Neuer Benutzer";
        H3 title = new H3(titleText);
        
        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
        
        // Load form data if editing
        if (userAccount.getId() != null) {
            loadUserData();
        }
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        usernameField = new TextField("Benutzername");
        usernameField.setRequired(true);
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setEnabled(userAccount.getId() == null); // Disable for existing users
        usernameField.setWidthFull();

        fullNameField = new TextField("Vollständiger Name");
        fullNameField.setWidthFull();

        emailField = new EmailField("E-Mail");
        emailField.setWidthFull();

        passwordField = new PasswordField("Passwort");
        passwordField.setRequired(userAccount.getId() == null); // Required for new users
        passwordField.setRequiredIndicatorVisible(userAccount.getId() == null);
        passwordField.setWidthFull();

        roleSelect = new Select<>();
        roleSelect.setLabel("Rolle");
        roleSelect.setItems(AppRoles.ADMIN, AppRoles.OWNER, AppRoles.DOCTOR, AppRoles.MEDICAL_STAFF, 
                AppRoles.TECH_USER, AppRoles.USER);
        roleSelect.setItemLabelGenerator(this::translateRole);
        roleSelect.setEmptySelectionAllowed(false);
        roleSelect.setRequiredIndicatorVisible(true);
        roleSelect.setWidthFull();

        locationComboBox = new ComboBox<>("Standort (optional)");
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Load locations when dialog is opened
            locationComboBox.setItems(locationService.getAllLocations(true)); // Only active locations
            locationComboBox.setItemLabelGenerator(loc -> loc != null && loc.getLocationName() != null ? loc.getLocationName() : "");
        }
        locationComboBox.setClearButtonVisible(true);
        locationComboBox.setWidthFull();

        enabledCheckbox = new Checkbox("Aktiv");
        enabledCheckbox.setWidthFull();

        // Arrange components side by side
        layout.add(usernameField, 2);
        layout.add(fullNameField, emailField);
        layout.add(passwordField, roleSelect);
        layout.add(locationComboBox, enabledCheckbox);
        
        return layout;
    }

    private String translateRole(String role) {
        return switch (role) {
            case AppRoles.ADMIN -> "Administrator";
            case AppRoles.OWNER -> "Praxisinhaber";
            case AppRoles.DOCTOR -> "Arzt";
            case AppRoles.MEDICAL_STAFF -> "Medizinisches Personal (MFA)";
            case AppRoles.TECH_USER -> "Technischer Benutzer";
            case AppRoles.USER -> "Benutzer";
            default -> role;
        };
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button cancelButton = new Button("Abbrechen", event -> close());
        
        saveButton = new Button("Speichern", event -> saveUser());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(cancelButton, saveButton);
        return layout;
    }

    private void loadUserData() {
        usernameField.setValue(userAccount.getUsername() != null ? userAccount.getUsername() : "");
        fullNameField.setValue(userAccount.getFullName() != null ? userAccount.getFullName() : "");
        emailField.setValue(userAccount.getEmail() != null ? userAccount.getEmail() : "");
        passwordField.clear(); // Don't show password
        if (!userAccount.getRoles().isEmpty()) {
            roleSelect.setValue(userAccount.getRoles().iterator().next());
        }
        // Reload locations to ensure they are available
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            locationComboBox.setItems(locationService.getAllLocations(true));
        }
        locationComboBox.setValue(userAccount.getPreferredLocation());
        enabledCheckbox.setValue(userAccount.isEnabled());
    }

    private void saveUser() {
        String username = usernameField.getValue();
        String role = roleSelect.getValue();
        
        if (username == null || username.trim().isEmpty() || role == null) {
            showError("Bitte geben Sie Benutzername und Rolle ein");
            return;
        }

        // For new users, password is required
        if (userAccount.getId() == null) {
            String password = passwordField.getValue();
            if (password == null || password.trim().isEmpty()) {
                showError("Bitte geben Sie ein Passwort ein");
                return;
            }
        }

        try {
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                showError("Keine Institution ausgewählt");
                return;
            }
            
            // Check if username already exists (for new users)
            if (userAccount.getId() == null) {
                if (userAccountRepository.findByUsername(username).isPresent()) {
                    showError("Benutzername existiert bereits");
                    return;
                }
            }
            
            userAccount.setUsername(username);
            userAccount.setFullName(fullNameField.getValue());
            userAccount.setEmail(emailField.getValue());
            
            // Update password only if provided
            String password = passwordField.getValue();
            if (password != null && !password.trim().isEmpty()) {
                userAccount.setPasswordHash("{noop}" + password);
            }
            
            // Set roles (only one role per user)
            userAccount.getRoles().clear();
            userAccount.getRoles().add(role);
            
            // Set institution if not set
            if (userAccount.getInstitution() == null) {
                institutionRepository.findById(institutionId)
                        .ifPresent(userAccount::setInstitution);
            }
            
            // Set preferred location
            Location selectedLocation = locationComboBox.getValue();
            userAccount.setPreferredLocation(selectedLocation);
            
            // Set userId if not set
            if (userAccount.getUserId() == null || userAccount.getUserId().isEmpty()) {
                userAccount.setUserId(UUID.randomUUID().toString());
            }
            
            // Set enabled status from checkbox (for editing) or based on password/email (for new users)
            if (userAccount.getId() != null) {
                // For existing users, use checkbox value
                userAccount.setEnabled(enabledCheckbox.getValue());
            } else {
                // For new users, only active if password and email are set
                boolean hasPassword = userAccount.getPasswordHash() != null && !userAccount.getPasswordHash().isEmpty();
                boolean hasEmail = userAccount.getEmail() != null && !userAccount.getEmail().trim().isEmpty();
                userAccount.setEnabled(hasPassword && hasEmail);
            }
            
            userAccountRepository.save(userAccount);
            
            showSuccess("Benutzer wurde erfolgreich gespeichert!");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
}

