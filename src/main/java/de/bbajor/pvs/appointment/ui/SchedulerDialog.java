package de.bbajor.pvs.appointment.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerType;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;

/**
 * Dialog for creating and editing appointment schedulers.
 */
public class SchedulerDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(SchedulerDialog.class);

    private final AppointmentSchedulerService schedulerService;
    private final LocationService locationService;
    private final UserAccountRepository userAccountRepository;
    private AppointmentScheduler scheduler;
    private Runnable onSaveCallback;

    private TextField nameField;
    private TextArea descriptionArea;
    private ComboBox<Location> locationComboBox;
    private ComboBox<SchedulerType> typeComboBox;
    private Checkbox activeCheckbox;

    public SchedulerDialog(
            AppointmentSchedulerService schedulerService,
            LocationService locationService,
            AppointmentScheduler scheduler) {
        this(schedulerService, locationService, scheduler, null);
    }

    public SchedulerDialog(
            AppointmentSchedulerService schedulerService,
            LocationService locationService,
            AppointmentScheduler scheduler,
            UserAccountRepository userAccountRepository) {
        this.schedulerService = schedulerService;
        this.locationService = locationService;
        this.userAccountRepository = userAccountRepository;
        this.scheduler = scheduler != null ? scheduler : new AppointmentScheduler();

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
        H3 title = new H3(scheduler.getId() != null ? "Terminplaner bearbeiten" : "Neuer Terminplaner");
        
        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        nameField = new TextField("Name");
        nameField.setValue(scheduler.getName() != null ? scheduler.getName() : "");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder("z.B. Dr. Schmidt - Sprechstunde");

        descriptionArea = new TextArea("Beschreibung");
        descriptionArea.setValue(scheduler.getDescription() != null ? scheduler.getDescription() : "");
        descriptionArea.setPlaceholder("Optionale Beschreibung des Terminplaners");

        // Ensure InstitutionContext is set before loading locations
        ensureInstitutionContext();

        locationComboBox = new ComboBox<>("Standort");
        locationComboBox.setItems(locationService.getAllLocations());
        locationComboBox.setItemLabelGenerator(Location::getLocationName);
        locationComboBox.setValue(scheduler.getLocation());
        locationComboBox.setRequiredIndicatorVisible(true);

        typeComboBox = new ComboBox<>("Typ");
        typeComboBox.setItems(SchedulerType.values());
        typeComboBox.setValue(scheduler.getType());
        typeComboBox.setItemLabelGenerator(this::translateType);

        activeCheckbox = new Checkbox("Aktiv");
        activeCheckbox.setValue(scheduler.isActive());

        layout.add(nameField, descriptionArea, locationComboBox, typeComboBox, activeCheckbox);
        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button saveButton = new Button("Speichern", event -> saveScheduler());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(saveButton);
        return layout;
    }

    private void saveScheduler() {
        try {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                showError("Bitte geben Sie einen Namen an");
                return;
            }
            if (locationComboBox.getValue() == null) {
                showError("Bitte wählen Sie einen Standort aus");
                return;
            }

            scheduler.setName(nameField.getValue());
            scheduler.setDescription(descriptionArea.getValue());
            scheduler.setLocation(locationComboBox.getValue());
            scheduler.setType(typeComboBox.getValue());
            scheduler.setActive(activeCheckbox.getValue());

            schedulerService.save(scheduler);

            showSuccess("Terminplaner erfolgreich gespeichert");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    private String translateType(SchedulerType type) {
        return switch (type) {
            case DOCTOR -> "Arzt";
            case MEDICAL_STAFF -> "Medizinisches Personal (MFA)";
            case PRE_EXAMINATION -> "Voruntersuchung";
            case GENERAL -> "Allgemein";
        };
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
     * so the context might not be set.
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
            if (userAccountRepository != null) {
                try {
                    String username = adapter.getUsername();
                    UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                        log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                                institutionId, userAccount.getInstitution().getInstitutionCode());
                    }
                } catch (Exception e) {
                    log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
                }
            }
        }
    }
}
