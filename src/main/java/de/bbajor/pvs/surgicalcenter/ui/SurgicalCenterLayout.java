package de.bbajor.pvs.surgicalcenter.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TextRenderer;

import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.PhoneUtils;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;

public class SurgicalCenterLayout extends HorizontalLayout {

    private final Binder<SurgicalCenter> binder = new Binder<>(SurgicalCenter.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Name Kontaktperson");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontaktperson");
    private final AddressField<Address> addressForm = new AddressField<>("Adresse",
            new Address());
    private TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    private final Grid<SurgicalCenterTimeSlot> availableTimeSlots = new Grid<>();

    public SurgicalCenterLayout() {
        setSizeFull();

        phoneField.setPrefixComponent(new Button(new Icon(VaadinIcon.PHONE), e -> {
            if (phoneField.getValue() != null && !phoneField.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("tel:" + phoneField.getValue(), "_self");
            }
        }));

        phoneContactField.setPrefixComponent(new Button(new Icon(VaadinIcon.PHONE), e -> {
            if (phoneContactField.getValue() != null && !phoneContactField.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("tel:" + phoneContactField.getValue(), "_self");
            }
        }));

        emailField.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        binder.forField(unitNameField).asRequired()
                .withNullRepresentation("")
                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 200,
                        "Bitte geben Sie einen gültigen Namen ein (max. 200 Zeichen)")
                .bind(SurgicalCenter::getName, SurgicalCenter::setName);

        binder.forField(addressForm).asRequired().withValidator(
                address -> address != null && address.getStreet() != null && !address.getStreet().trim().isEmpty()
                        && address.getHouseNo() != null && !address.getHouseNo().trim().isEmpty()
                        && address.getPostalCode() != null
                        && address.getPostalCode() >= 1000
                        && address.getPostalCode() <= 99999
                        && address.getCity() != null && !address.getCity().trim().isEmpty(),
                "Bitte geben Sie eine gültige Adresse ein").bind(SurgicalCenter::getAddress,
                        SurgicalCenter::setAddress);

        binder.forField(phoneField).withValidator(item -> {
            if (item == null || item.trim().isEmpty()) {
                return true; // Allow empty phone numbers
            }
            return item.trim().length() <= 50;
        }, "Bitte geben Sie eine gültige Telefonnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Allow empty phone numbers
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getPhone, SurgicalCenter::setPhone);

        binder.forField(emailField)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Email is optional
                    }
                    return item.trim().length() <= 100 && item.contains("@") && item.contains(".");
                }, "Bitte geben Sie eine gültige E-Mail-Adresse ein(max. 100 Zeichen)")
                .bind(SurgicalCenter::getEmail, SurgicalCenter::setEmail);

        binder.forField(contactField)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getContact, SurgicalCenter::setContact);
        binder.forField(phoneContactField)
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Kontakt-Telefon ist optional
                    }
                    return item.trim().length() <= 50;
                }, "Bitte geben Sie eine gültige Telefonnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Kontakt-Telefon ist optional
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getPhoneContact, SurgicalCenter::setPhoneContact);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Create Details Tab Content
        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();
        detailsLayout.setPadding(true);

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

        // Create TimeSlots Tab Content
        VerticalLayout timeSlotsLayout = new VerticalLayout();
        timeSlotsLayout.setSizeFull();
        timeSlotsLayout.setPadding(true);

        AccordionPanel timeSlotCreationAccordion = new AccordionPanel("OP-Slot hinzufügen", timeSlotConfigForm);
        timeSlotCreationAccordion.addThemeVariants(DetailsVariant.SMALL);
        timeSlotCreationAccordion.setOpened(true);
        timeSlotsLayout.add(timeSlotCreationAccordion);
        
        VerticalLayout availableTimeSlotsLayout = new VerticalLayout();
        availableTimeSlotsLayout.setSizeFull();
        availableTimeSlotsLayout.setMinHeight("600px");
        availableTimeSlotsLayout.add(new Div("Vorhandene Zeitslots"));
        availableTimeSlots.addColumn(dto -> DateAndTimeUtils.getGermanDateTimeFormatter().format(dto.getDate()))
                .setHeader("Datum");
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
        availableTimeSlots.addColumn(dto -> dto.getPatientCount()).setHeader("Anzahl Patienten");
        availableTimeSlots.setSizeFull();
        availableTimeSlotsLayout.add(availableTimeSlots);
        timeSlotsLayout.add(availableTimeSlotsLayout);

        // Add tabs to TabSheet
        tabSheet.add("Stammdaten", detailsLayout);
        tabSheet.add("OP-Slots", timeSlotsLayout);

        add(tabSheet);
    }

    public void setBean(SurgicalCenter dto) {
        binder.setBean(dto);
        if (dto != null && dto.getAvailableTimeSlots() != null) {
            availableTimeSlots.setItems(dto.getAvailableTimeSlots());
        }
    }

    public SurgicalCenter getBean() {
        SurgicalCenter surgicalCenter = binder.getBean();
        return surgicalCenter;
    }

    public List<TimeSlotConfig> getTimeSlotsToCreate() {
        return timeSlotConfigForm.getTimeSlotConfigList();
    }

}
