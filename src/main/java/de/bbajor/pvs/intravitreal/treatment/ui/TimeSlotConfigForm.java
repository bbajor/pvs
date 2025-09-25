package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.bbajor.pvs.base.dto.TimePeriod;
import de.bbajor.pvs.base.dto.TimeSlotRepetition;
import de.bbajor.pvs.base.util.DayOfWeekItemLabelGenerator;
import de.bbajor.pvs.intravitreal.treatment.dto.State;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;

@Component
public class TimeSlotConfigForm extends HorizontalLayout {

    @Value("${domain.bundesland}")
    private String bundesland;

    private final DatePicker periodStartPicker = new DatePicker("Termine erstellen ab");
    private final Checkbox singleAppointmentCheckBox = new Checkbox("Einzeltermin");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>(
            "Laufzeit (späteres Hinzufügen möglich.)");
    private final ComboBox<TimeSlotRepetition> timeSlotRepetitionComboBox = new ComboBox<>(
            "Wie oft? (z.B. \"An jedem Werktag\", \"wöchentlich\",\"Alle 2 Wochen\", etc.)");
    private final ComboBox<DayOfWeek> dayOfWeekComboBox = new ComboBox<>("Wochentag");
    private final TimePicker timeSlotStartPicker = new TimePicker("Beginn");
    private final TimePicker timeSlotEndPicker = new TimePicker("Ende");

    private final Button addTimeSlotSeriesButton = new Button("+ hinzufügen");
    private final VirtualList<TimeSlotConfig> timeSlotsToCreateVirtualList = new VirtualList<>();
    private final List<TimeSlotConfig> timeSlotsToCreateList = new ArrayList<>();
    private final Binder<TimeSlotConfig> binder = new Binder<>();

    public TimeSlotConfigForm() {
        setSizeFull();
        binder.setBean(new TimeSlotConfig().setPeriodStartDate(LocalDate.now()));

        timeSlotRepetitionComboBox.setItems(TimeSlotRepetition.values());
        timePeriodComboBox.setItems(TimePeriod.values());
        dayOfWeekComboBox.setItems(DayOfWeek.values());
        dayOfWeekComboBox.setItemLabelGenerator(new DayOfWeekItemLabelGenerator(Locale.GERMAN));

        timeSlotStartPicker.setMin(LocalTime.of(6, 0));
        timeSlotStartPicker.setMax(LocalTime.of(19, 0));

        timeSlotEndPicker.setMin(LocalTime.of(7, 0));
        timeSlotEndPicker.setMax(LocalTime.of(20, 0));

        singleAppointmentCheckBox.addValueChangeListener(event -> {
            boolean isChecked = event.getValue();
            enableRelevantComponents(isChecked);
        });

        FormLayout filterLayout = new FormLayout();
        filterLayout.add(periodStartPicker, dayOfWeekComboBox, timeSlotStartPicker, timeSlotEndPicker,
                singleAppointmentCheckBox, timePeriodComboBox, timeSlotRepetitionComboBox,
                addTimeSlotSeriesButton);
        add(filterLayout);

        binder.forField(timePeriodComboBox).asRequired((timePeriod, t) -> {
            Boolean isSingleAppointment = singleAppointmentCheckBox.getValue() != null
                    && singleAppointmentCheckBox.getValue();
            if (!isSingleAppointment && timePeriod == null) {
                return ValidationResult.error("Bitte wählen Sie eine Laufzeit für die Terminserie aus.");
            } else {
                return ValidationResult.ok();
            }
        }).bind(TimeSlotConfig::getTimePeriod, TimeSlotConfig::setTimePeriod);
        binder.forField(timeSlotRepetitionComboBox).asRequired().asRequired((repetition, t) -> {
            Boolean isSingleAppointment = singleAppointmentCheckBox.getValue() != null
                    && singleAppointmentCheckBox.getValue();
            if (!isSingleAppointment && repetition == null) {
                return ValidationResult.error("Bitte wählen Sie aus, wie oft der Termin wiederholt werden soll.");
            } else {
                return ValidationResult.ok();
            }
        }).bind(TimeSlotConfig::getTimeSlotRepetition,
                TimeSlotConfig::setTimeSlotRepetition);
        binder.forField(periodStartPicker).asRequired().bind(TimeSlotConfig::getPeriodStartDate,
                TimeSlotConfig::setPeriodStartDate);
        binder.forField(dayOfWeekComboBox).asRequired().bind(TimeSlotConfig::getDayOfWeek,
                TimeSlotConfig::setDayOfWeek);
        binder.forField(timeSlotStartPicker).asRequired().bind(TimeSlotConfig::getStartTime,
                TimeSlotConfig::setStartTime);
        binder.forField(timeSlotEndPicker).asRequired().bind(TimeSlotConfig::getEndTime, TimeSlotConfig::setEndTime);
        binder.forField(singleAppointmentCheckBox).bind(TimeSlotConfig::isSingleAppointment,
                TimeSlotConfig::setSingleAppointment);

        timeSlotsToCreateVirtualList.setHeight("400px");
        timeSlotsToCreateVirtualList.setMinWidth("300px");

        timeSlotsToCreateVirtualList.setItems(timeSlotsToCreateList);
        timeSlotsToCreateVirtualList
                .setRenderer(new ComponentRenderer<>(config -> new TimeSlotConfigCard(config, deleteEvent -> {
                    timeSlotsToCreateList.remove(config);
                    timeSlotsToCreateVirtualList.setItems(timeSlotsToCreateList);
                })));

        addTimeSlotSeriesButton.addClickListener(event -> {
            if (isValidSlotConstellation()) {
                TimePeriod selectedTimePeriod = timePeriodComboBox.getValue();
                TimeSlotRepetition selectedTimeSlotRepetition = timeSlotRepetitionComboBox.getValue();
                LocalTime selectedTimeSlotBegin = timeSlotStartPicker.getValue();
                LocalTime selectedTimeSlotEnd = timeSlotEndPicker.getValue();
                DayOfWeek selectedDayOfWeek = dayOfWeekComboBox.getValue();
                boolean isSingleAppointment = singleAppointmentCheckBox.getValue() != null
                        && singleAppointmentCheckBox.getValue();
                TimeSlotConfig config = new TimeSlotConfig()
                        .setBundesland(State.byString(bundesland))
                        .setDayOfWeek(selectedDayOfWeek)
                        .setPeriodStartDate(periodStartPicker.getValue())
                        .setTimePeriod(isSingleAppointment ? TimePeriod.NONE : selectedTimePeriod)
                        .setStartTime(selectedTimeSlotBegin)
                        .setEndTime(selectedTimeSlotEnd)
                        .setTimeSlotRepetition(
                                isSingleAppointment ? TimeSlotRepetition.NO_REPETITION : selectedTimeSlotRepetition);
                if (!timeSlotsToCreateList.contains(config)) {
                    timeSlotsToCreateList.add(config);
                }
                timeSlotsToCreateVirtualList.setItems(timeSlotsToCreateList);
            } else {
                Notification.show("Einer oder mehrere Eingaben für die OP-Slots sind ungültig." +
                        "Bitte überprüfen Sie die Angaben und versuchen es erneut.");
            }
        });
        add(timeSlotsToCreateVirtualList);
    }

    private void enableRelevantComponents(boolean isSingleAppointment) {
        timePeriodComboBox.setEnabled(!isSingleAppointment);
        timeSlotRepetitionComboBox.setEnabled(!isSingleAppointment);
    }

    public List<TimeSlotConfig> getTimeSlotConfigList() {
        return timeSlotsToCreateList;
    }

    private boolean isValidSlotConstellation() {
        TimePeriod selectedTimePeriod = timePeriodComboBox.getValue();
        TimeSlotRepetition selectedTimeSlotRepetition = timeSlotRepetitionComboBox.getValue();
        LocalTime startTime = timeSlotStartPicker.getValue();
        LocalTime selectedTimeSlotEnd = timeSlotEndPicker.getValue();
        boolean isSingleAppointment = singleAppointmentCheckBox.getValue() != null
                && singleAppointmentCheckBox.getValue();
        return (isSingleAppointment || (selectedTimePeriod != null && selectedTimeSlotRepetition != null))
                && startTime != null && selectedTimeSlotEnd != null
                && startTime.isBefore(selectedTimeSlotEnd);
    }

    public TimeSlotConfig getCurrentConfig() {
        if (binder.getBean() == null) {
            binder.setBean(new TimeSlotConfig().setBundesland(State.byString(bundesland)));
        }
        return binder.getBean();
    }

}
