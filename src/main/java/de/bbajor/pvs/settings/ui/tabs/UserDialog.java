package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Dialog for creating and editing users.
 */
@Slf4j
public class UserDialog extends Dialog {

    private final UserAccountRepository userAccountRepository;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;
    private final CurrentUser currentUser;
    private final UserAccount userAccount;
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
            CurrentUser currentUser,
            UserAccount userAccount) {
        this.userAccountRepository = userAccountRepository;
        this.locationService = locationService;
        this.institutionRepository = institutionRepository;
        this.currentUser = currentUser;
        this.userAccount = userAccount != null ? userAccount : new UserAccount();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("500px");
        setCloseOnOutsideClick(false);
        
        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

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
        // Ensure InstitutionContext is set before loading locations
        ensureInstitutionContext();
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Load locations for current institution
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

        saveButton = new Button("Speichern", event -> saveUser());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(saveButton);
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
        // Ensure InstitutionContext is set before loading locations
        ensureInstitutionContext();
        // Reload locations to ensure they are available
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            locationComboBox.setItems(locationService.getAllLocations(true));
        }
        Location preferredLocation = userAccount.getPreferredLocation();
        locationComboBox.setValue(preferredLocation);
        enabledCheckbox.setValue(userAccount.isEnabled());
        
        // Disable enabled checkbox if current user is trying to deactivate themselves
        // and they are an ADMIN (InstitutionAdmin)
        if (userAccount.getId() != null) {
            currentUser.get().ifPresent(appUser -> {
                String preferredUsername = appUser.getPreferredUsername();
                if (preferredUsername != null) {
                    UserAccount currentUserAccount = userAccountRepository.findByUsername(preferredUsername).orElse(null);
                    if (currentUserAccount != null && currentUserAccount.getId() != null 
                            && currentUserAccount.getId().equals(userAccount.getId())) {
                        // Check if current user has ADMIN role (InstitutionAdmin)
                        if (currentUserAccount.getRoles() != null && currentUserAccount.getRoles().contains(AppRoles.ADMIN)) {
                            enabledCheckbox.setEnabled(false);
                            enabledCheckbox.setHelperText("Sie können sich nicht selbst deaktivieren");
                        }
                    }
                }
            });
        }
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
            // Ensure InstitutionContext is set before saving
            ensureInstitutionContext();
            
            Long institutionId = InstitutionContext.getInstitutionId();
            
            // Fallback: If InstitutionContext is not set, try to get institution from userAccount
            if (institutionId == null && userAccount.getInstitution() != null) {
                institutionId = userAccount.getInstitution().getId();
                InstitutionContext.setInstitutionId(institutionId);
            }
            
            // If still no institution, try to get it from current user
            if (institutionId == null) {
                currentUser.get().ifPresent(appUser -> {
                    String preferredUsername = appUser.getPreferredUsername();
                    if (preferredUsername != null) {
                        UserAccount currentUserAccount = userAccountRepository.findByUsername(preferredUsername).orElse(null);
                        if (currentUserAccount != null && currentUserAccount.getInstitution() != null) {
                            Long currentUserInstitutionId = currentUserAccount.getInstitution().getId();
                            InstitutionContext.setInstitutionId(currentUserInstitutionId);
                        }
                    }
                });
                institutionId = InstitutionContext.getInstitutionId();
            }
            
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
                // But prevent InstitutionAdmin from deactivating themselves
                boolean newEnabledValue = enabledCheckbox.getValue();
                boolean canProceed = true;
                var currentUserOpt = currentUser.get();
                if (currentUserOpt.isPresent()) {
                    var appUser = currentUserOpt.get();
                    String preferredUsername = appUser.getPreferredUsername();
                    if (preferredUsername != null) {
                        UserAccount currentUserAccount = userAccountRepository.findByUsername(preferredUsername).orElse(null);
                        if (currentUserAccount != null && currentUserAccount.getId() != null 
                                && currentUserAccount.getId().equals(userAccount.getId())) {
                            // Check if current user has ADMIN role (InstitutionAdmin)
                            if (currentUserAccount.getRoles() != null && currentUserAccount.getRoles().contains(AppRoles.ADMIN)) {
                                if (!newEnabledValue) {
                                    showError("Sie können sich nicht selbst deaktivieren");
                                    canProceed = false;
                                }
                            }
                        }
                    }
                }
                if (!canProceed) {
                    return;
                }
                userAccount.setEnabled(newEnabledValue);
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
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set, especially for InstitutionAdmins.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
                
                if (account != null && account.getInstitution() != null) {
                    Long institutionId = account.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, account.getInstitution().getInstitutionCode());
                } else {
                    log.warn("UserAccount has no institution - cannot set InstitutionContext");
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
            }
        } else {
            log.debug("Authentication type: {}, Principal type: {} - cannot set InstitutionContext",
                    authentication != null ? authentication.getClass().getSimpleName() : "null",
                    authentication != null && authentication.getPrincipal() != null 
                        ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        }
    }
}

