package de.bbajor.pvs.ivomplan.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TextRenderer;

import de.bbajor.pvs.base.ui.view.AddressField;
import de.bbajor.pvs.ivomplan.controller.TimeSlotConfig;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitAddressDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;

public class SurgeryUnitForm extends HorizontalLayout {

    private final Binder<SurgeryUnitDto> binder = new Binder<>(SurgeryUnitDto.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Kontakt");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontakt");
    private final AddressField<SurgeryUnitAddressDto> addressForm = new AddressField<>("Adresse");
    private TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    private final Grid<SurgeryUnitTimeSlotDto> availableTimeSlots = new Grid<>();

    public SurgeryUnitForm() {
        setSizeFull();

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
        AccordionPanel generalAccordion = new AccordionPanel("Allgemeine Informationen", form);
        generalAccordion.addThemeVariants(DetailsVariant.SMALL);
        generalAccordion.setOpened(true);
        detailsLayout.add(generalAccordion);

        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setSizeFull();
        addressFormLayout.add(addressForm);
        addressFormLayout.setColspan(addressForm, 2);
        AccordionPanel addressAccordion = new AccordionPanel("Adresse", addressFormLayout);
        addressAccordion.addThemeVariants(DetailsVariant.SMALL);
        addressAccordion.setOpened(true);
        detailsLayout.add(addressAccordion);

        AccordionPanel timeSlotCreationAccordion = new AccordionPanel("OP-Slot hinzufügen", timeSlotConfigForm);
        timeSlotCreationAccordion.addThemeVariants(DetailsVariant.SMALL);
        timeSlotCreationAccordion.setOpened(true);
        detailsLayout.add(timeSlotCreationAccordion);
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
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            String start = slot.getStartTime() == null ? "-" : slot.getStartTime().toString();
            String end = slot.getEndTime() == null ? "-" : slot.getEndTime().toString();
            return start + " - " + end + " Uhr";
        })).setHeader("Uhrzeit");
        availableTimeSlots.setSizeFull();
        availableTimeSlotsLayout.add(availableTimeSlots);

        add(availableTimeSlotsLayout);
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
        return timeSlotConfigForm.getTimeSlotConfigList();
    }

}
