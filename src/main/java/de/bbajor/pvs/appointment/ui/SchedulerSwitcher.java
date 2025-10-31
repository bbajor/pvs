package de.bbajor.pvs.appointment.ui;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;

/**
 * Component for switching between different appointment schedulers.
 * Displays the current scheduler prominently and allows switching.
 */
public class SchedulerSwitcher extends HorizontalLayout {

    private final AppointmentSchedulerService schedulerService;
    private final ComboBox<AppointmentScheduler> schedulerComboBox;
    private final Span currentSchedulerLabel;
    private final Div indicator;

    private Consumer<AppointmentScheduler> onSchedulerChange;

    public SchedulerSwitcher(AppointmentSchedulerService schedulerService) {
        this.schedulerService = schedulerService;

        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        setPadding(true);

        getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius)")
            .set("padding", "var(--lumo-space-m)");

        // Indicator to make current scheduler visually prominent
        indicator = new Div();
        indicator.getStyle()
            .set("width", "12px")
            .set("height", "12px")
            .set("border-radius", "50%")
            .set("background-color", "var(--lumo-success-color)")
            .set("margin-right", "var(--lumo-space-s)");

        currentSchedulerLabel = new Span("Aktueller Terminplaner:");
        currentSchedulerLabel.getStyle()
            .set("font-weight", "bold")
            .set("margin-right", "var(--lumo-space-m)");

        schedulerComboBox = new ComboBox<>();
        schedulerComboBox.setPlaceholder("Terminplaner wählen");
        schedulerComboBox.setWidth("300px");
        schedulerComboBox.setItemLabelGenerator(scheduler -> 
            scheduler.getName() + " (" + scheduler.getType() + ")"
        );

        schedulerComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null && onSchedulerChange != null) {
                onSchedulerChange.accept(event.getValue());
                updateIndicator(event.getValue());
            }
        });

        add(indicator, currentSchedulerLabel, schedulerComboBox);
        
        refreshSchedulers();
    }

    /**
     * Refresh the list of available schedulers.
     */
    public void refreshSchedulers() {
        List<AppointmentScheduler> schedulers = schedulerService.findAll();
        schedulerComboBox.setItems(schedulers);

        // Auto-select first scheduler if none selected
        if (schedulerComboBox.getValue() == null && !schedulers.isEmpty()) {
            schedulerComboBox.setValue(schedulers.get(0));
        }
    }

    /**
     * Get the currently selected scheduler.
     */
    public AppointmentScheduler getCurrentScheduler() {
        return schedulerComboBox.getValue();
    }

    /**
     * Set the current scheduler.
     */
    public void setCurrentScheduler(AppointmentScheduler scheduler) {
        schedulerComboBox.setValue(scheduler);
        updateIndicator(scheduler);
    }

    /**
     * Set callback for when scheduler changes.
     */
    public void setOnSchedulerChange(Consumer<AppointmentScheduler> callback) {
        this.onSchedulerChange = callback;
    }

    /**
     * Update visual indicator based on scheduler.
     */
    private void updateIndicator(AppointmentScheduler scheduler) {
        if (scheduler == null) {
            indicator.getStyle().set("background-color", "var(--lumo-error-color)");
            return;
        }

        // Change indicator color based on scheduler type
        String color = switch (scheduler.getType()) {
            case DOCTOR -> "var(--lumo-primary-color)";
            case MEDICAL_STAFF -> "var(--lumo-success-color)";
            case PRE_EXAMINATION -> "var(--lumo-warning-color)";
            case GENERAL -> "var(--lumo-contrast-50pct)";
        };

        indicator.getStyle().set("background-color", color);
    }
}
