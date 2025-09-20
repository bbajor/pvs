package de.bbajor.pvs.ivomplan.ui;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.base.ui.view.AddressField;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.TimePeriod;
import de.bbajor.pvs.ivomplan.dto.TimeSlotRepetition;

public class SurgeryUnitForm extends VerticalLayout {

    private final Binder<SurgeryUnitDto> binder = new Binder<>();

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Kontakt");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontakt");
    private final AddressField addressForm = new AddressField("Adresse");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>(
            "Laufzeit (späteres Hinzufügen möglich.)");
    private final ComboBox<TimeSlotRepetition> timeSlotRepetition = new ComboBox<>(
            "Wie oft? (z.B. \"An jedem Werktag\", \"wöchentlich\",\"Alle 2 Wochen\", etc.)");
    private final ComboBox<DayOfWeek> dayOfWeek = new ComboBox<>("Wochentag");
    private final DatePicker startOfTimeSlots = new DatePicker("Termine erstellen ab");
    private final TimePicker timeSlotStartPicker = new TimePicker("Beginn");
    private final TimePicker timeSlotEndPicker = new TimePicker("Ende");
    private final Button addTimeSlotSeriesButton = new Button("+ hinzufügen");

    public SurgeryUnitForm() {

        setSizeFull();

        timeSlotRepetition.setItems(TimeSlotRepetition.values());
        timePeriodComboBox.setItems(TimePeriod.values());
        dayOfWeek.setItems(DayOfWeek.values());

        binder.forField(unitNameField).bind(SurgeryUnitDto::getName, SurgeryUnitDto::setName);
        binder.forField(addressForm).bind(SurgeryUnitDto::getAddress, SurgeryUnitDto::setAddress);
        binder.forField(phoneField).bind(SurgeryUnitDto::getPhone, SurgeryUnitDto::setPhone);
        binder.forField(emailField).bind(SurgeryUnitDto::getEmail, SurgeryUnitDto::setEmail);
        binder.forField(contactField).bind(SurgeryUnitDto::getContact, SurgeryUnitDto::setEmail);
        binder.forField(phoneContactField).bind(SurgeryUnitDto::getPhoneContact, SurgeryUnitDto::setPhoneContact);

        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setMinColumns(4);
        form.add(unitNameField, phoneField, emailField, contactField, phoneContactField);
        AccordionPanel accordion = new AccordionPanel("Allgemeine Informationen", form);
        accordion.addThemeVariants(DetailsVariant.SMALL);
        accordion.setOpened(true);
        add(accordion);

        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setSizeFull();
        addressFormLayout.add(addressForm);
        addressFormLayout.setColspan(addressForm, 2);
        AccordionPanel addressAccordion = new AccordionPanel("Adresse", addressFormLayout);
        addressAccordion.addThemeVariants(DetailsVariant.SMALL);
        addressAccordion.setOpened(true);
        add(addressAccordion);
        

        FormLayout timeSlotCreation = new FormLayout();
        timeSlotCreation.setSizeFull();
        timeSlotCreation.add(timePeriodComboBox, timeSlotRepetition, dayOfWeek, startOfTimeSlots, timeSlotStartPicker, timeSlotEndPicker,
                addTimeSlotSeriesButton);
        add(new NativeLabel("OP-Slot hinzufügen"));
        add(timeSlotCreation);

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
                slots.add(new Paragraph("Beginn: " + selectedTimeSlotBegin));
                slots.add(new Paragraph("Ende: " + selectedTimeSlotEnd));
                cardContainer.add(slots);
            } else {
                Notification.show("Einer oder mehrere Eingaben für die OP-Slots sind ungültig." +
                        "Bitte überprüfen Sie die Angaben und versuchen es erneut.");
            }
        });
        add(cardContainer);

    }

    private boolean isValidSlotConstellation() {
        // TODO später auslagern
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
    }

    public SurgeryUnitDto getBean() {
        return binder.getBean();
    }

}
