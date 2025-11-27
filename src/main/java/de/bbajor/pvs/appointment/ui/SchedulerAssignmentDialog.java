package de.bbajor.pvs.appointment.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

import java.util.List;

/**
 * Dialog for assigning users or roles to appointment schedulers.
 */
public class SchedulerAssignmentDialog extends Dialog {

    private final AppointmentSchedulerService schedulerService;
    private final UserAccountRepository userAccountRepository;
    private final AppointmentScheduler scheduler;
    private Runnable onSaveCallback;

    private RadioButtonGroup<String> assignmentTypeGroup;
    private ComboBox<UserAccount> userComboBox;
    private ComboBox<String> roleComboBox;

    public SchedulerAssignmentDialog(
            AppointmentSchedulerService schedulerService,
            UserAccountRepository userAccountRepository,
            AppointmentScheduler scheduler) {
        this.schedulerService = schedulerService;
        this.userAccountRepository = userAccountRepository;
        this.scheduler = scheduler;

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("500px");
        setCloseOnOutsideClick(false);

        initializeDialog();
    }

    private void initializeDialog() {
        H3 title = new H3("Zuordnung erstellen");
        
        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Assignment type selection
        assignmentTypeGroup = new RadioButtonGroup<>();
        assignmentTypeGroup.setLabel("Zuordnungstyp");
        assignmentTypeGroup.setItems("Benutzer", "Rolle");
        assignmentTypeGroup.setValue("Benutzer");
        assignmentTypeGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        assignmentTypeGroup.addValueChangeListener(e -> updateFormVisibility());

        // User selection
        userComboBox = new ComboBox<>("Benutzer");
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            List<UserAccount> users = userAccountRepository.findAllByInstitutionIdWithPreferredLocation(institutionId);
            userComboBox.setItems(users);
            userComboBox.setItemLabelGenerator(ua -> {
                String name = ua.getFullName() != null ? ua.getFullName() : ua.getUsername();
                return name + " (" + ua.getUsername() + ")";
            });
        }
        userComboBox.setRequiredIndicatorVisible(true);

        // Role selection
        roleComboBox = new ComboBox<>("Rolle");
        roleComboBox.setItems(
            AppRoles.ADMIN,
            AppRoles.OWNER,
            AppRoles.DOCTOR,
            AppRoles.MEDICAL_STAFF,
            AppRoles.TECH_USER,
            AppRoles.USER
        );
        roleComboBox.setItemLabelGenerator(this::translateRole);
        roleComboBox.setRequiredIndicatorVisible(true);
        roleComboBox.setVisible(false);

        layout.add(assignmentTypeGroup, userComboBox, roleComboBox);
        return layout;
    }

    private void updateFormVisibility() {
        String selectedType = assignmentTypeGroup.getValue();
        boolean isUser = "Benutzer".equals(selectedType);
        userComboBox.setVisible(isUser);
        userComboBox.setRequired(isUser);
        roleComboBox.setVisible(!isUser);
        roleComboBox.setRequired(!isUser);
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
        
        Button saveButton = new Button("Speichern", event -> saveAssignment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(cancelButton, saveButton);
        return layout;
    }

    private void saveAssignment() {
        try {
            String assignmentType = assignmentTypeGroup.getValue();
            
            if ("Benutzer".equals(assignmentType)) {
                UserAccount user = userComboBox.getValue();
                if (user == null) {
                    showError("Bitte wählen Sie einen Benutzer aus");
                    return;
                }
                
                // Check if assignment already exists
                if (schedulerService.getAssignments(scheduler).stream()
                    .anyMatch(a -> a.getUserAccount() != null && a.getUserAccount().getId().equals(user.getId()))) {
                    showError("Dieser Benutzer ist bereits zugeordnet");
                    return;
                }
                
                schedulerService.assignUser(scheduler, user);
                showSuccess("Benutzer erfolgreich zugeordnet");
            } else {
                String role = roleComboBox.getValue();
                if (role == null || role.isBlank()) {
                    showError("Bitte wählen Sie eine Rolle aus");
                    return;
                }
                
                // Check if assignment already exists
                if (schedulerService.getAssignments(scheduler).stream()
                    .anyMatch(a -> role.equals(a.getRole()))) {
                    showError("Diese Rolle ist bereits zugeordnet");
                    return;
                }
                
                schedulerService.assignRole(scheduler, role);
                showSuccess("Rolle erfolgreich zugeordnet");
            }
            
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

