package de.bbajor.pvs.appointment.ui;

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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.SchedulerType;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.practice.service.PracticeService;

/**
 * Dialog for creating and editing appointment schedulers.
 */
public class SchedulerDialog extends Dialog {

    private final AppointmentSchedulerService schedulerService;
    private final PracticeService practiceService;
    private AppointmentScheduler scheduler;
    private Runnable onSaveCallback;

    private TextField nameField;
    private TextArea descriptionArea;
    private ComboBox<Practice> practiceComboBox;
    private ComboBox<SchedulerType> typeComboBox;
    private Checkbox activeCheckbox;

    public SchedulerDialog(
            AppointmentSchedulerService schedulerService,
            PracticeService practiceService,
            AppointmentScheduler scheduler) {
        this.schedulerService = schedulerService;
        this.practiceService = practiceService;
        this.scheduler = scheduler != null ? scheduler : new AppointmentScheduler();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("500px");

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

        practiceComboBox = new ComboBox<>("Praxis");
        practiceComboBox.setItems(practiceService.findAll());
        practiceComboBox.setItemLabelGenerator(Practice::getPracticeName);
        practiceComboBox.setValue(scheduler.getPractice());
        practiceComboBox.setRequiredIndicatorVisible(true);

        typeComboBox = new ComboBox<>("Typ");
        typeComboBox.setItems(SchedulerType.values());
        typeComboBox.setValue(scheduler.getType());
        typeComboBox.setItemLabelGenerator(this::translateType);

        activeCheckbox = new Checkbox("Aktiv");
        activeCheckbox.setValue(scheduler.isActive());

        layout.add(nameField, descriptionArea, practiceComboBox, typeComboBox, activeCheckbox);
        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button cancelButton = new Button("Abbrechen", event -> close());
        
        Button saveButton = new Button("Speichern", event -> saveScheduler());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(cancelButton, saveButton);
        return layout;
    }

    private void saveScheduler() {
        try {
            if (nameField.getValue() == null || nameField.getValue().isBlank()) {
                showError("Bitte geben Sie einen Namen an");
                return;
            }
            if (practiceComboBox.getValue() == null) {
                showError("Bitte wählen Sie eine Praxis aus");
                return;
            }

            scheduler.setName(nameField.getValue());
            scheduler.setDescription(descriptionArea.getValue());
            scheduler.setPractice(practiceComboBox.getValue());
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
}
