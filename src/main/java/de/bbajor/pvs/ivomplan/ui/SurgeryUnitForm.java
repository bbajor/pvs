package de.bbajor.pvs.ivomplan.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TextRenderer;

import de.bbajor.pvs.base.misc.DayOfWeekItemLabelGenerator;
import de.bbajor.pvs.base.ui.view.AddressField;
import de.bbajor.pvs.ivomplan.controller.TimeSlotConfig;
import de.bbajor.pvs.ivomplan.dto.Bundesland;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitAddressDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.dto.TimePeriod;
import de.bbajor.pvs.ivomplan.dto.TimeSlotRepetition;

public class SurgeryUnitForm extends HorizontalLayout {

    private final Binder<SurgeryUnitDto> binder = new Binder<>(SurgeryUnitDto.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Kontakt");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontakt");
    private final AddressField<SurgeryUnitAddressDto> addressForm = new AddressField<>("Adresse");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>(
            "Laufzeit (späteres Hinzufügen möglich.)");
    private final ComboBox<TimeSlotRepetition> timeSlotRepetition = new ComboBox<>(
            "Wie oft? (z.B. \"An jedem Werktag\", \"wöchentlich\",\"Alle 2 Wochen\", etc.)");
    private final ComboBox<DayOfWeek> dayOfWeek = new ComboBox<>("Wochentag");
    private final DatePicker startOfTimeSlots = new DatePicker("Termine erstellen ab");
    private final TimePicker timeSlotStartPicker = new TimePicker("Beginn");
    private final TimePicker timeSlotEndPicker = new TimePicker("Ende");
    private final Button addTimeSlotSeriesButton = new Button("+ hinzufügen");
    private final Grid<SurgeryUnitTimeSlotDto> availableTimeSlots = new Grid<>();
    private final List<TimeSlotConfig> timeSlotsToCreate = new ArrayList<>();

    public SurgeryUnitForm(Bundesland bundesland) {
        setSizeFull();

        timeSlotRepetition.setItems(TimeSlotRepetition.values());
        timePeriodComboBox.setItems(TimePeriod.values());
        dayOfWeek.setItems(DayOfWeek.values());
        dayOfWeek.setItemLabelGenerator(new DayOfWeekItemLabelGenerator(Locale.GERMAN));

        binder.forField(unitNameField).bind(SurgeryUnitDto::getName, SurgeryUnitDto::setName);
        binder.forField(addressForm).bind(SurgeryUnitDto::getSurgeryUnitAddress, SurgeryUnitDto::setSurgeryUnitAddress);
        binder.forField(phoneField).bind(SurgeryUnitDto::getPhone, SurgeryUnitDto::setPhone);
        binder.forField(emailField).bind(SurgeryUnitDto::getEmail, SurgeryUnitDto::setEmail);
        binder.forField(contactField).bind(SurgeryUnitDto::getContact, SurgeryUnitDto::setContact);
        binder.forField(phoneContactField).bind(SurgeryUnitDto::getPhoneContact, SurgeryUnitDto::setPhoneContact);

        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();

        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setMinColumns(4);
        form.add(unitNameField, phoneField, emailField, contactField, phoneContactField);
        AccordionPanel accordion = new AccordionPanel("Allgemeine Informationen", form);
        accordion.addThemeVariants(DetailsVariant.SMALL);
        accordion.setOpened(true);
        detailsLayout.add(accordion);

        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setSizeFull();
        addressFormLayout.add(addressForm);
        addressFormLayout.setColspan(addressForm, 2);
        AccordionPanel addressAccordion = new AccordionPanel("Adresse", addressFormLayout);
        addressAccordion.addThemeVariants(DetailsVariant.SMALL);
        addressAccordion.setOpened(true);
        detailsLayout.add(addressAccordion);

        FormLayout timeSlotCreation = new FormLayout();
        timeSlotCreation.setSizeFull();
        timeSlotCreation.setMinColumns(4);
        timeSlotCreation.add(timePeriodComboBox, timeSlotRepetition, dayOfWeek, startOfTimeSlots, timeSlotStartPicker,
                timeSlotEndPicker,
                addTimeSlotSeriesButton);
        AccordionPanel timeSlotCreationAccordion = new AccordionPanel("OP-Slot hinzufügen", timeSlotCreation);
        timeSlotCreationAccordion.addThemeVariants(DetailsVariant.SMALL);
        timeSlotCreationAccordion.setOpened(true);
        detailsLayout.add(timeSlotCreationAccordion);

        FormLayout cardContainer = new FormLayout();
        cardContainer.setSizeFull();

        addTimeSlotSeriesButton.addClickListener(event -> {
            if (isValidSlotConstellation()) {
                Card slots = new Card();
                TimePeriod selectedTimePeriod = timePeriodComboBox.getValue();
                TimeSlotRepetition selectedTimeSlotRepetition = timeSlotRepetition.getValue();
                LocalTime selectedTimeSlotBegin = timeSlotStartPicker.getValue();
                LocalTime selectedTimeSlotEnd = timeSlotEndPicker.getValue();
                DayOfWeek selectedDayOfWeek = dayOfWeek.getValue();
                slots.setTitle(new Div("Für " + selectedTimePeriod.toString()));
                slots.setSubtitle(new Div("Jeden " + selectedDayOfWeek.toString()));
                slots.add(new Paragraph(
                        "Erzeugt neue OP-Slots über folgenden Zeitraum: " + selectedTimePeriod.toString()));
                slots.add(new Paragraph(
                        "Die Slots werden an folgendem Wochentag erstellt: " + selectedDayOfWeek.toString()));
                slots.add(new Paragraph("Erstelle: " + selectedTimeSlotRepetition));
                slots.add(new Paragraph("Von: " + selectedTimeSlotBegin));
                slots.add(new Paragraph("Bis: " + selectedTimeSlotEnd));
                cardContainer.add(slots);
                TimeSlotConfig config = new TimeSlotConfig()
                        .setBundesland(bundesland)
                        .setDayOfWeek(selectedDayOfWeek)
                        .setPeriodStart(startOfTimeSlots.getValue())
                        .setTimePeriod(selectedTimePeriod)
                        .setStartTime(selectedTimeSlotBegin)
                        .setEndTime(selectedTimeSlotEnd)
                        .setTimeSlotRepetition(selectedTimeSlotRepetition);
                timeSlotsToCreate.add(config);
            } else {
                Notification.show("Einer oder mehrere Eingaben für die OP-Slots sind ungültig." +
                        "Bitte überprüfen Sie die Angaben und versuchen es erneut.");
            }
        });
        detailsLayout.add(cardContainer);
        add(detailsLayout);

        VerticalLayout availableTimeSlotsLayout = new VerticalLayout();
        availableTimeSlotsLayout.setSizeFull();
        availableTimeSlotsLayout.setMinHeight("500px");
        availableTimeSlotsLayout.add(new Div("Vorhandene Zeitslots"));
        availableTimeSlots.addColumn(SurgeryUnitTimeSlotDto::getDate).setHeader("Tag");
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            LocalDate date = slot.getDate();
            if (date == null) {
                return "";
            }
            Locale locale = Locale.GERMAN;
            DayOfWeek dow = date.getDayOfWeek();
            return dow.getDisplayName(TextStyle.FULL, locale);
        })).setHeader("Wochentag");
        availableTimeSlots.addColumn(SurgeryUnitTimeSlotDto::getStartTime).setHeader("Von");
        availableTimeSlots.addColumn(SurgeryUnitTimeSlotDto::getEndTime).setHeader("Bis");
        availableTimeSlots.setSizeFull();
        availableTimeSlotsLayout.add(availableTimeSlots);

        add(availableTimeSlotsLayout);
    }

    private boolean isValidSlotConstellation() {
        TimePeriod selectedTimePeriod = timePeriodComboBox.getValue();
        TimeSlotRepetition selectedTimeSlotPeriod = timeSlotRepetition.getValue();
        LocalTime selectedTimeSlotBegin = timeSlotStartPicker.getValue();
        LocalTime selectedTimeSlotEnd = timeSlotEndPicker.getValue();
        return selectedTimePeriod != null && selectedTimeSlotPeriod != null
                && selectedTimeSlotBegin != null && selectedTimeSlotEnd != null
                && selectedTimeSlotBegin.isBefore(selectedTimeSlotEnd);
    }

    public void setBean(SurgeryUnitDto dto) {
        binder.setBean(dto);
        if (dto != null && dto.getAvailableTimeSlots() != null) {
            availableTimeSlots.setItems(dto.getAvailableTimeSlots());
        }
    }

    public SurgeryUnitDto getBean() {
        return binder.getBean();
    }

    public List<TimeSlotConfig> getTimeSlotsToCreate() {
        return timeSlotsToCreate;
    }

}
