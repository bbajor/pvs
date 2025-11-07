package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tab for managing users of the current institution.
 * Allows creating, editing, activating/deactivating users and assigning them to locations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class UserSettingsTab extends VerticalLayout {

    private final UserAccountRepository userAccountRepository;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;

    private TextField usernameField;
    private TextField fullNameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private Select<String> roleSelect;
    private ComboBox<Location> locationComboBox;
    private Checkbox enabledCheckbox;
    private Button saveButton;
    private Button cancelButton;
    
    private UserAccount selectedUser;
    private List<UserAccount> allUsers;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Benutzerverwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H3 title = new H3("Benutzerverwaltung");

        // Initialize fields
        usernameField = new TextField("Benutzername");
        usernameField.setRequired(true);
        usernameField.setWidthFull();

        fullNameField = new TextField("Vollständiger Name");
        fullNameField.setWidthFull();

        emailField = new EmailField("E-Mail");
        emailField.setWidthFull();

        passwordField = new PasswordField("Passwort");
        passwordField.setWidthFull();

        roleSelect = new Select<>();
        roleSelect.setLabel("Rolle");
        roleSelect.setItems(AppRoles.ADMIN, AppRoles.OWNER, AppRoles.DOCTOR, AppRoles.MEDICAL_STAFF, 
                AppRoles.TECH_USER, AppRoles.USER);
        roleSelect.setEmptySelectionAllowed(false);
        roleSelect.setWidthFull();

        locationComboBox = new ComboBox<>("Standort (optional)");
        locationComboBox.setItems(locationService.getAllLocations(true)); // Only active locations
        locationComboBox.setRenderer(new TextRenderer<>(Location::getLocationName));
        locationComboBox.setItemLabelGenerator(loc -> loc.getLocationName() != null ? loc.getLocationName() : "");
        locationComboBox.setClearButtonVisible(true);
        locationComboBox.setWidthFull();

        enabledCheckbox = new Checkbox("Aktiv");
        enabledCheckbox.setValue(true);

        saveButton = new Button("Speichern", e -> saveUser());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("Abbrechen", e -> clearForm());

        FormLayout formLayout = new FormLayout();
        formLayout.add(usernameField, fullNameField, emailField, passwordField, 
                roleSelect, locationComboBox, enabledCheckbox, saveButton, cancelButton);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        add(title, formLayout);
        
        refreshUsers();
    }

    private void saveUser() {
        String username = usernameField.getValue();
        String role = roleSelect.getValue();
        
        if (username == null || username.trim().isEmpty() || role == null) {
            Notification.show("Bitte geben Sie Benutzername und Rolle ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            UserAccount userAccount = selectedUser != null && selectedUser.getId() != null
                    ? selectedUser
                    : userAccountRepository.findByUsername(username).orElseGet(UserAccount::new);
            
            // Check if user belongs to current institution
            Long institutionId = InstitutionContext.getInstitutionId();
            if (userAccount.getInstitution() != null && 
                !userAccount.getInstitution().getId().equals(institutionId)) {
                Notification.show("Benutzer gehört zu einer anderen Institution", 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            userAccount.setUsername(username);
            userAccount.setFullName(fullNameField.getValue());
            userAccount.setEmail(emailField.getValue());
            userAccount.getRoles().clear();
            userAccount.getRoles().add(role);
            userAccount.setEnabled(enabledCheckbox.getValue());
            
            // Set institution if not set
            if (userAccount.getInstitution() == null && institutionId != null) {
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
            
            // Update password only if provided
            String password = passwordField.getValue();
            if (password != null && !password.isEmpty()) {
                userAccount.setPasswordHash("{noop}" + password);
            } else if (userAccount.getPasswordHash() == null || userAccount.getPasswordHash().isEmpty()) {
                // Set default password if none exists
                userAccount.setPasswordHash("{noop}123");
            }
            
            userAccountRepository.save(userAccount);
            refreshUsers();
            clearForm();
            
            Notification.show("Benutzer wurde erfolgreich gespeichert!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error saving user: {}", e.getMessage(), e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearForm() {
        selectedUser = null;
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        roleSelect.clear();
        locationComboBox.clear();
        enabledCheckbox.setValue(true);
    }

    private void refreshUsers() {
        // Refresh location list in case locations were added/removed
        locationComboBox.setItems(locationService.getAllLocations(true));
        
        // TODO: Load users for current institution
        // For now, this is a simple form
    }
}

