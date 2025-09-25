package de.bbajor.pvs.surgicalcenter.ui;

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
import de.bbajor.pvs.intravitreal.treatment.ui.TimeSlotConfigForm;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterAddressDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;

public class SurgicalCenterLayout extends HorizontalLayout {

    private final Binder<SurgicalCenterDto> binder = new Binder<>(SurgicalCenterDto.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Kontakt");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontakt");
    private final AddressField<SurgicalCenterAddressDto> addressForm = new AddressField<>("Adresse",
            new SurgicalCenterAddressDto());
    private TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    private final Grid<SurgicalCenterTimeSlotDto> availableTimeSlots = new Grid<>();

    public SurgicalCenterLayout() {
        setSizeFull();

        binder.forField(unitNameField).bind(SurgicalCenterDto::getName, SurgicalCenterDto::setName);
        binder.forField(addressForm).bind(SurgicalCenterDto::getSurgicalCenterAddress, SurgicalCenterDto::setSurgicalCenterAddress);
        binder.forField(phoneField).bind(SurgicalCenterDto::getPhone, SurgicalCenterDto::setPhone);
        binder.forField(emailField).bind(SurgicalCenterDto::getEmail, SurgicalCenterDto::setEmail);
        binder.forField(contactField).bind(SurgicalCenterDto::getContact, SurgicalCenterDto::setContact);
        binder.forField(phoneContactField).bind(SurgicalCenterDto::getPhoneContact, SurgicalCenterDto::setPhoneContact);

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
        availableTimeSlots.addColumn(SurgicalCenterTimeSlotDto::getDate).setHeader("Tag");
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

    public void setBean(SurgicalCenterDto dto) {
        binder.setBean(dto);
        if (dto != null && dto.getAvailableTimeSlots() != null) {
            availableTimeSlots.setItems(dto.getAvailableTimeSlots());
        }
    }

    public SurgicalCenterDto getBean() {
        SurgicalCenterDto surgeryUnitDto = binder.getBean();
        return surgeryUnitDto;
    }

    public List<TimeSlotConfig> getTimeSlotsToCreate() {
        return timeSlotConfigForm.getTimeSlotConfigList();
    }

}
