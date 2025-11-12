package de.bbajor.pvs.appointment.ui;

import java.time.DayOfWeek;
import java.time.LocalTime;

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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.service.OfficeHoursService;

/**
 * Dialog for creating and editing office hours.
 */
public class OfficeHoursDialog extends Dialog {

    private final OfficeHoursService officeHoursService;
    private final AppointmentScheduler scheduler;
    private OfficeHours officeHours;
    private Runnable onSaveCallback;

    private ComboBox<DayOfWeek> dayOfWeekComboBox;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private IntegerField slotDurationField;
    private Checkbox activeCheckbox;

    public OfficeHoursDialog(
            OfficeHoursService officeHoursService,
            AppointmentScheduler scheduler,
            OfficeHours officeHours) {
        this.officeHoursService = officeHoursService;
        this.scheduler = scheduler;
        this.officeHours = officeHours != null ? officeHours : createNewOfficeHours();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("500px");

        initializeDialog();
    }

    private OfficeHours createNewOfficeHours() {
        OfficeHours oh = new OfficeHours();
        oh.setScheduler(scheduler);
        oh.setActive(true);
        oh.setSlotDurationMinutes(5);
        oh.setStartTime(LocalTime.of(8, 0));
        oh.setEndTime(LocalTime.of(12, 0));
        return oh;
    }

    private void initializeDialog() {
        H3 title = new H3(officeHours.getId() != null ? "Sprechzeit bearbeiten" : "Neue Sprechzeit");
        
        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        dayOfWeekComboBox = new ComboBox<>("Wochentag");
        dayOfWeekComboBox.setItems(DayOfWeek.values());
        dayOfWeekComboBox.setValue(officeHours.getDayOfWeek());
        dayOfWeekComboBox.setItemLabelGenerator(this::translateDayOfWeek);
        dayOfWeekComboBox.setRequiredIndicatorVisible(true);

        startTimePicker = new TimePicker("Von");
        startTimePicker.setValue(officeHours.getStartTime());
        startTimePicker.setRequiredIndicatorVisible(true);
        startTimePicker.setStep(java.time.Duration.ofMinutes(15));

        endTimePicker = new TimePicker("Bis");
        endTimePicker.setValue(officeHours.getEndTime());
        endTimePicker.setRequiredIndicatorVisible(true);
        endTimePicker.setStep(java.time.Duration.ofMinutes(15));

        slotDurationField = new IntegerField("Slot-Dauer (Minuten)");
        slotDurationField.setValue(officeHours.getSlotDurationMinutes());
        slotDurationField.setRequiredIndicatorVisible(true);
        slotDurationField.setMin(5);
        slotDurationField.setMax(120);
        slotDurationField.setStep(5);
        slotDurationField.setHelperText("Zwischen 5 und 120 Minuten");

        activeCheckbox = new Checkbox("Aktiv");
        activeCheckbox.setValue(officeHours.isActive());

        layout.add(dayOfWeekComboBox, 2);
        layout.add(startTimePicker, 1);
        layout.add(endTimePicker, 1);
        layout.add(slotDurationField, 1);
        layout.add(activeCheckbox, 1);

        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button cancelButton = new Button("Abbrechen", event -> close());
        
        Button saveButton = new Button("Speichern", event -> saveOfficeHours());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button deleteButton = new Button("Löschen", event -> deleteOfficeHours());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(officeHours.getId() != null);

        layout.add(deleteButton, cancelButton, saveButton);
        return layout;
    }

    private void saveOfficeHours() {
        try {
            if (dayOfWeekComboBox.getValue() == null) {
                showError("Bitte wählen Sie einen Wochentag aus");
                return;
            }
            if (startTimePicker.getValue() == null) {
                showError("Bitte geben Sie eine Startzeit an");
                return;
            }
            if (endTimePicker.getValue() == null) {
                showError("Bitte geben Sie eine Endzeit an");
                return;
            }
            if (slotDurationField.getValue() == null) {
                showError("Bitte geben Sie eine Slot-Dauer an");
                return;
            }

            officeHours.setDayOfWeek(dayOfWeekComboBox.getValue());
            officeHours.setStartTime(startTimePicker.getValue());
            officeHours.setEndTime(endTimePicker.getValue());
            officeHours.setSlotDurationMinutes(slotDurationField.getValue());
            officeHours.setActive(activeCheckbox.getValue());

            officeHoursService.save(officeHours);

            showSuccess("Sprechzeit erfolgreich gespeichert");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void deleteOfficeHours() {
        try {
            officeHoursService.delete(officeHours);
            showSuccess("Sprechzeit erfolgreich gelöscht");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Fehler beim Löschen: " + e.getMessage());
        }
    }

    private String translateDayOfWeek(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Montag";
            case TUESDAY -> "Dienstag";
            case WEDNESDAY -> "Mittwoch";
            case THURSDAY -> "Donnerstag";
            case FRIDAY -> "Freitag";
            case SATURDAY -> "Samstag";
            case SUNDAY -> "Sonntag";
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
