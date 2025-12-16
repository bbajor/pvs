package de.bbajor.pvs.surgicalcenter.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.PhoneUtils;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotCreator;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;

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
    private final Grid<Treatment> plannedTreatmentsGrid = new Grid<>();
    private final Grid<SurgicalCenterTimeSlot> newTimeSlotsGrid = new Grid<>();
    private final ApplicationContext applicationContext;
    private SurgicalCenterTimeSlot selectedTimeSlot;
    private boolean showPastSlots = false;
    private Button togglePastSlotsButton;
    private final List<SurgicalCenterTimeSlot> newTimeSlotsList = new ArrayList<>();
    private Div plannedTreatmentsSection;
    private Runnable binderChangeListener; // Listener für Binder-Änderungen
    private Runnable tabChangeListener; // Listener für Tab-Wechsel (zum Speichern beim Verlassen des Stammdaten-Tabs)
    private Runnable saveListener; // Listener für explizites Speichern (z.B. beim Hinzufügen von OP-Slots)

    public SurgicalCenterLayout(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
                        // Prüfe das rohe Eingabeformat: muss Ziffern enthalten
                        String cleaned = item.replaceAll("[^0-9+]", "");
                        if (cleaned.isEmpty()) {
                            return false;
                        }
                        // Prüfe nur, ob die Formatierung funktioniert (keine Exception)
                        // Das formatierte Ergebnis wird direkt verwendet, ohne weitere Validierung
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        // Formatierung erfolgreich, wenn Ergebnis nicht leer ist und mit +49 beginnt
                        return formatted != null && !formatted.isEmpty() && formatted.startsWith("+49");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein")
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
                        // Prüfe das rohe Eingabeformat: muss Ziffern enthalten
                        String cleaned = item.replaceAll("[^0-9+]", "");
                        if (cleaned.isEmpty()) {
                            return false;
                        }
                        // Prüfe nur, ob die Formatierung funktioniert (keine Exception)
                        // Das formatierte Ergebnis wird direkt verwendet, ohne weitere Validierung
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        // Formatierung erfolgreich, wenn Ergebnis nicht leer ist und mit +49 beginnt
                        return formatted != null && !formatted.isEmpty() && formatted.startsWith("+49");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein")
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
        
        // Binder-Änderungen überwachen für Auto-Save
        binder.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Create Details Tab Content
        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();
        detailsLayout.setPadding(true);

        // Sections "Allgemeine Informationen" und "Adresse" nebeneinander
        HorizontalLayout infoAndAddressLayout = new HorizontalLayout();
        infoAndAddressLayout.setSizeFull();
        infoAndAddressLayout.setSpacing(true);
        infoAndAddressLayout.setPadding(false);
        
        // Section "Allgemeine Informationen" statt Accordion
        Div generalSection = createSection("Allgemeine Informationen");
        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("800px", 3),
                new FormLayout.ResponsiveStep("1200px", 4)
        );
        form.add(unitNameField, phoneField, emailField, contactField, phoneContactField);
        // Telefon- und E-Mail-Felder auf normale Größe setzen
        phoneField.setWidthFull();
        emailField.setWidthFull();
        phoneContactField.setWidthFull();
        generalSection.add(form);
        infoAndAddressLayout.add(generalSection);
        infoAndAddressLayout.setFlexGrow(1, generalSection);

        // Section "Adresse" statt Accordion
        Div addressSection = createSection("Adresse");
        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setSizeFull();
        addressFormLayout.add(addressForm);
        addressFormLayout.setColspan(addressForm, 2);
        addressSection.add(addressFormLayout);
        infoAndAddressLayout.add(addressSection);
        infoAndAddressLayout.setFlexGrow(1, addressSection);
        
        detailsLayout.add(infoAndAddressLayout);

        // Zwei Grids nebeneinander: "Vorhandene Zeitslots" und "Geplante Behandlungen"
        HorizontalLayout gridsLayout = new HorizontalLayout();
        gridsLayout.setSizeFull();
        gridsLayout.setSpacing(true);
        gridsLayout.setPadding(false);

        // Grid "Vorhandene Zeitslots"
        VerticalLayout availableTimeSlotsLayout = new VerticalLayout();
        availableTimeSlotsLayout.setSizeFull();
        availableTimeSlotsLayout.setPadding(false);
        availableTimeSlotsLayout.setSpacing(false);
        
        Div availableSlotsSection = createSection("Vorhandene Zeitslots");
        availableSlotsSection.setHeightFull();
        
        // Header mit Toggle-Button
        HorizontalLayout slotsHeader = new HorizontalLayout();
        slotsHeader.setWidthFull();
        slotsHeader.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        Div slotsTitle = new Div("Vorhandene Zeitslots");
        slotsTitle.getStyle().set("font-weight", "bold");
        slotsTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
        
        togglePastSlotsButton = new Button("Vergangene Termine anzeigen", VaadinIcon.CALENDAR.create());
        togglePastSlotsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        togglePastSlotsButton.addClickListener(e -> {
            showPastSlots = !showPastSlots;
            togglePastSlotsButton.setText(showPastSlots ? "Vergangene Termine ausblenden" : "Vergangene Termine anzeigen");
            refreshTimeSlotsGrid();
        });
        
        slotsHeader.add(slotsTitle);
        slotsHeader.setFlexGrow(1, slotsTitle);
        slotsHeader.add(togglePastSlotsButton);
        availableSlotsSection.add(slotsHeader);
        
        // Grid konfigurieren
        availableTimeSlots.addColumn(dto -> DateAndTimeUtils.getGermanDateTimeFormatter().format(dto.getDate()))
                .setHeader("Datum").setWidth("120px").setFlexGrow(0);
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            LocalDate date = slot.getDate();
            if (date == null) {
                return "";
            }
            Locale locale = Locale.GERMAN;
            DayOfWeek dow = date.getDayOfWeek();
            return dow.getDisplayName(TextStyle.FULL, locale);
        })).setHeader("Wochentag").setWidth("120px").setFlexGrow(0);
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            String start = slot.getStartTime() == null ? "-" : slot.getStartTime().toString();
            String end = slot.getEndTime() == null ? "-" : slot.getEndTime().toString();
            return start + " - " + end + " Uhr";
        })).setHeader("Uhrzeit").setWidth("150px").setFlexGrow(0);
        availableTimeSlots.addColumn(dto -> dto.getPatientCount()).setHeader("Anzahl Patienten")
                .setWidth("120px").setFlexGrow(0);
        
        // PDF-Icon-Spalte für Sammelbericht (nur wenn Patienten vorhanden)
        availableTimeSlots.addColumn(
            new ComponentRenderer<>(slot -> {
                Integer patientCount = slot.getPatientCount();
                if (patientCount == null || patientCount == 0) {
                    return new Div("-");
                }
                Button pdfButton = new Button(new Icon(VaadinIcon.FILE_TEXT));
                pdfButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
                pdfButton.setTooltipText("Sammelbericht generieren");
                pdfButton.addClickListener(e -> {
                    selectedTimeSlot = slot;
                    generateCombinedReport();
                });
                return pdfButton;
            })
        ).setHeader("Bericht").setWidth("100px").setFlexGrow(0);
        
        // CSS-Klassen für vergangene Slots
        availableTimeSlots.setClassNameGenerator(slot -> {
            if (slot.getDate() != null && slot.getDate().isBefore(LocalDate.now())) {
                return "past-time-slot";
            }
            return null;
        });
        
        // Inline CSS für graue Hinterlegung
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  vaadin-grid-row[class*='past-time-slot'] { background-color: #f5f5f5 !important; }" +
            "`;" +
            "document.head.appendChild(style);"
        );
        
        availableTimeSlots.setSizeFull();
        availableTimeSlots.setMinHeight("400px");
        availableTimeSlots.setPageSize(20);
        availableTimeSlots.setSelectionMode(SelectionMode.SINGLE);
        availableTimeSlots.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresentOrElse(
                slot -> {
                    selectedTimeSlot = slot;
                    loadPlannedTreatments(slot);
                },
                () -> {
                    selectedTimeSlot = null;
                    plannedTreatmentsGrid.setItems(List.of());
                    plannedTreatmentsSection.setVisible(false);
                }
            );
        });
        
        availableSlotsSection.add(availableTimeSlots);
        availableTimeSlotsLayout.add(availableSlotsSection);
        availableTimeSlotsLayout.setFlexGrow(1, availableSlotsSection);
        gridsLayout.add(availableTimeSlotsLayout);
        gridsLayout.setFlexGrow(1, availableTimeSlotsLayout);

        // Grid "Geplante Behandlungen" mit Patientendaten-Renderer
        VerticalLayout plannedTreatmentsLayout = new VerticalLayout();
        plannedTreatmentsLayout.setSizeFull();
        plannedTreatmentsLayout.setPadding(false);
        plannedTreatmentsLayout.setSpacing(false);
        
        plannedTreatmentsSection = createSection("Geplante Behandlungen");
        plannedTreatmentsSection.setHeightFull();
        plannedTreatmentsSection.setVisible(false); // Initial nicht sichtbar
        
        // Grid konfigurieren - gleiche Höhe wie "Vorhandene Zeitslots"
        plannedTreatmentsGrid.setSizeFull();
        plannedTreatmentsGrid.setMinHeight("400px");
        plannedTreatmentsGrid.setSelectionMode(SelectionMode.NONE);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        
        // Patientendaten-Renderer (eine Spalte)
        plannedTreatmentsGrid.addColumn(new ComponentRenderer<>(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            if (patient == null) {
                return new Span("-");
            }
            VerticalLayout patientLayout = new VerticalLayout();
            patientLayout.setSpacing(false);
            patientLayout.setPadding(false);
            
            String name = "";
            if (patient.getLastName() != null) {
                name = patient.getLastName();
            }
            if (patient.getFirstName() != null) {
                name += (name.isEmpty() ? "" : ", ") + patient.getFirstName();
            }
            if (!name.isEmpty()) {
                Span nameSpan = new Span(name);
                nameSpan.getStyle().set("font-weight", "600");
                patientLayout.add(nameSpan);
            }
            
            if (patient.getBirth() != null) {
                Span birthSpan = new Span("geb. " + dateFormatter.format(patient.getBirth()));
                birthSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
                birthSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                patientLayout.add(birthSpan);
            }
            
            if (patient.getHealthInsurance() != null && 
                patient.getHealthInsurance().getBillingCarrierName() != null) {
                Span insuranceSpan = new Span(patient.getHealthInsurance().getBillingCarrierName());
                insuranceSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
                insuranceSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                patientLayout.add(insuranceSpan);
            }
            
            return patientLayout;
        })).setHeader("Patient").setWidth("200px").setFlexGrow(1);
        
        plannedTreatmentsGrid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
            .setHeader("Auge").setWidth("100px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> {
            if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament").setWidth("200px").setFlexGrow(1);
        
        plannedTreatmentsSection.add(plannedTreatmentsGrid);
        plannedTreatmentsLayout.add(plannedTreatmentsSection);
        plannedTreatmentsLayout.setFlexGrow(1, plannedTreatmentsSection);
        gridsLayout.add(plannedTreatmentsLayout);
        gridsLayout.setFlexGrow(1, plannedTreatmentsLayout);
        
        detailsLayout.add(gridsLayout);
        detailsLayout.setFlexGrow(1, gridsLayout);

        // Create TimeSlots Tab Content
        VerticalLayout timeSlotsLayout = new VerticalLayout();
        timeSlotsLayout.setSizeFull();
        timeSlotsLayout.setPadding(true);

        // Section "OP-Slot hinzufügen" statt Accordion - kompakter
        Div timeSlotCreationSection = createSection("OP-Slot hinzufügen");
        timeSlotCreationSection.getStyle().set("max-height", "300px");
        timeSlotCreationSection.getStyle().set("overflow", "auto");
        timeSlotCreationSection.add(timeSlotConfigForm);
        timeSlotsLayout.add(timeSlotCreationSection);
        
        // Grid "Neue Zeitslots" für noch nicht persistierte Slots
        Div newTimeSlotsSection = createSection("Neue Zeitslots");
        
        // Grid konfigurieren
        newTimeSlotsGrid.setSizeFull();
        newTimeSlotsGrid.setMinHeight("400px");
        newTimeSlotsGrid.setSelectionMode(SelectionMode.MULTI);
        
        newTimeSlotsGrid.addColumn(dto -> DateAndTimeUtils.getGermanDateTimeFormatter().format(dto.getDate()))
                .setHeader("Datum").setWidth("120px").setFlexGrow(0);
        newTimeSlotsGrid.addColumn(new TextRenderer<>(slot -> {
            LocalDate date = slot.getDate();
            if (date == null) {
                return "";
            }
            Locale locale = Locale.GERMAN;
            DayOfWeek dow = date.getDayOfWeek();
            return dow.getDisplayName(TextStyle.FULL, locale);
        })).setHeader("Wochentag").setWidth("120px").setFlexGrow(0);
        newTimeSlotsGrid.addColumn(new TextRenderer<>(slot -> {
            String start = slot.getStartTime() == null ? "-" : slot.getStartTime().toString();
            String end = slot.getEndTime() == null ? "-" : slot.getEndTime().toString();
            return start + " - " + end + " Uhr";
        })).setHeader("Uhrzeit").setWidth("150px").setFlexGrow(0);
        
        // Spalte für Löschen-Button
        newTimeSlotsGrid.addColumn(new ComponentRenderer<>(slot -> {
            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteButton.setTooltipText("Entfernen");
            deleteButton.addClickListener(e -> {
                newTimeSlotsList.remove(slot);
                refreshNewTimeSlotsGrid();
            });
            return deleteButton;
        })).setHeader("Aktion").setWidth("100px").setFlexGrow(0);
        
        // CSS-Klassen für ungültige Slots (Überschneidungen)
        newTimeSlotsGrid.setClassNameGenerator(slot -> {
            if (hasOverlap(slot)) {
                return "invalid-time-slot";
            }
            return null;
        });
        
        // Inline CSS für rote Hinterlegung bei Überschneidungen
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  vaadin-grid-row[class*='invalid-time-slot'] { background-color: #ffebee !important; }" +
            "`;" +
            "document.head.appendChild(style);"
        );
        
        newTimeSlotsSection.add(newTimeSlotsGrid);
        timeSlotsLayout.add(newTimeSlotsSection);
        timeSlotsLayout.setFlexGrow(1, newTimeSlotsSection);
        
        // Setze Callback für TimeSlotConfigForm
        timeSlotConfigForm.setOnSlotAddedCallback(this::handleAddTimeSlot);

        // Add tabs to TabSheet
        tabSheet.add("Stammdaten", detailsLayout);
        tabSheet.add("OP-Slots", timeSlotsLayout);
        
        // Listener für Tab-Wechsel: Speichere Änderungen beim Verlassen des Stammdaten-Tabs
        tabSheet.addSelectedChangeListener(event -> {
            com.vaadin.flow.component.tabs.Tab selectedTab = event.getSelectedTab();
            // Wenn zum OP-Slots-Tab gewechselt wird, speichere Änderungen
            if (selectedTab != null && "OP-Slots".equals(selectedTab.getLabel()) && tabChangeListener != null) {
                tabChangeListener.run();
            }
        });

        add(tabSheet);
    }

    public void setBean(SurgicalCenter dto) {
        binder.setBean(dto);
        
        // Stelle sicher, dass die Adresse explizit im AddressField gesetzt wird
        // AddressField ist ein AbstractCompositeField mit eigenem Binder,
        // daher muss der Wert explizit gesetzt werden
        if (dto != null && dto.getAddress() != null) {
            addressForm.setValue(dto.getAddress());
        } else if (dto != null) {
            // Wenn keine Adresse vorhanden ist, erstelle eine neue
            addressForm.setValue(new de.bbajor.pvs.patient.model.Address());
        }
        
        // Setze neue Slots-Liste zurück
        newTimeSlotsList.clear();
        refreshNewTimeSlotsGrid();
        if (dto != null && dto.getAvailableTimeSlots() != null) {
            refreshTimeSlotsGrid();
        }
    }
    
    private void refreshTimeSlotsGrid() {
        SurgicalCenter dto = binder.getBean();
        if (dto == null || dto.getAvailableTimeSlots() == null) {
            return;
        }
        
        ensureInstitutionContext();
        
        // Sortiere Slots nach Datum (aufsteigend)
        List<SurgicalCenterTimeSlot> sortedSlots = dto.getAvailableTimeSlots().stream()
            .sorted(Comparator
                .comparing((SurgicalCenterTimeSlot slot) -> slot.getDate() != null ? slot.getDate() : LocalDate.MAX)
                .thenComparing(slot -> slot.getStartTime() != null ? slot.getStartTime() : java.time.LocalTime.MAX))
            .collect(Collectors.toList());
        
        // Filtere vergangene Slots wenn nicht angezeigt werden sollen
        if (!showPastSlots) {
            LocalDate today = LocalDate.now();
            sortedSlots = sortedSlots.stream()
                .filter(slot -> slot.getDate() == null || !slot.getDate().isBefore(today))
                .collect(Collectors.toList());
        }
        
        availableTimeSlots.setItems(sortedSlots);
        
        // Springe zum nächsten anstehenden Termin
        if (!sortedSlots.isEmpty()) {
            LocalDate today = LocalDate.now();
            SurgicalCenterTimeSlot nextSlot = sortedSlots.stream()
                .filter(slot -> slot.getDate() != null && 
                    (slot.getDate().isAfter(today) || slot.getDate().equals(today)))
                .findFirst()
                .orElse(sortedSlots.get(0)); // Falls kein zukünftiger Slot, nimm den ersten
            
            if (nextSlot != null) {
                // Wähle den Slot aus und scrolle zu ihm
                availableTimeSlots.select(nextSlot);
                // Scrolle zum ausgewählten Item
                availableTimeSlots.getDataProvider().refreshAll();
                UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => {" +
                    "  const grid = $0;" +
                    "  const selectedItem = grid.selectedItems[0];" +
                    "  if (selectedItem) {" +
                    "    const index = Array.from(grid.items).indexOf(selectedItem);" +
                    "    if (index >= 0) {" +
                    "      grid.scrollToIndex(index);" +
                    "    }" +
                    "  }" +
                    "}, 100);",
                    availableTimeSlots.getElement()
                );
            }
        }
    }

    public SurgicalCenter getBean() {
        SurgicalCenter surgicalCenter = binder.getBean();
        return surgicalCenter;
    }

    public List<SurgicalCenterTimeSlot> getTimeSlotsToCreate() {
        return new ArrayList<>(newTimeSlotsList);
    }
    
    /**
     * Setzt einen Listener, der aufgerufen wird, wenn sich der Binder ändert.
     */
    public void setBinderChangeListener(Runnable listener) {
        this.binderChangeListener = listener;
    }
    
    /**
     * Setzt einen Listener, der aufgerufen wird, wenn der Tab gewechselt wird.
     * Wird verwendet, um Änderungen beim Verlassen des Stammdaten-Tabs zu speichern.
     */
    public void setTabChangeListener(Runnable listener) {
        this.tabChangeListener = listener;
    }
    
    /**
     * Setzt einen Listener, der aufgerufen wird, wenn explizit gespeichert werden soll.
     * Wird verwendet, z.B. beim Hinzufügen von OP-Slots, wenn das SurgicalCenter noch nicht persistiert ist.
     */
    public void setSaveListener(Runnable listener) {
        this.saveListener = listener;
    }
    
    /**
     * Prüft, ob Änderungen am SurgicalCenter vorgenommen wurden.
     */
    public boolean hasChanges() {
        if (binder == null || binder.getBean() == null) {
            return false;
        }
        
        // Prüfe Binder-Änderungen
        boolean binderHasChanges = binder.hasChanges();
        
        // Prüfe explizit, ob sich die Adresse geändert hat
        // AddressField ist ein AbstractCompositeField mit eigenem Binder,
        // daher wird es möglicherweise nicht vom Hauptbinder erkannt
        boolean addressHasChanges = false;
        if (addressForm != null && binder.getBean() != null) {
            de.bbajor.pvs.patient.model.Address currentAddress = binder.getBean().getAddress();
            de.bbajor.pvs.patient.model.Address formAddress = addressForm.getValue();
            
            if (currentAddress == null && formAddress != null) {
                addressHasChanges = true;
            } else if (currentAddress != null && formAddress == null) {
                addressHasChanges = true;
            } else if (currentAddress != null && formAddress != null) {
                // Vergleiche Adressfelder
                addressHasChanges = !java.util.Objects.equals(currentAddress.getStreet(), formAddress.getStreet()) ||
                                   !java.util.Objects.equals(currentAddress.getHouseNo(), formAddress.getHouseNo()) ||
                                   !java.util.Objects.equals(currentAddress.getPostalCode(), formAddress.getPostalCode()) ||
                                   !java.util.Objects.equals(currentAddress.getCity(), formAddress.getCity()) ||
                                   !java.util.Objects.equals(currentAddress.getCountry(), formAddress.getCountry());
            }
        }
        
        return binderHasChanges || addressHasChanges;
    }
    
    /**
     * Schreibt alle Änderungen aus den UI-Feldern in das Bean.
     * Dies stellt sicher, dass alle Werte (inkl. Adresse, Name, Telefonnummern, E-Mail, Kontaktperson) korrekt gesetzt werden.
     */
    public void writeBean() {
        // Stelle sicher, dass ein Bean existiert (auch bei Neuanlage)
        if (binder == null) {
            return;
        }
        
        SurgicalCenter bean = binder.getBean();
        if (bean == null) {
            // Bei Neuanlage: Erstelle neues Bean, falls noch keines existiert
            bean = new SurgicalCenter();
            binder.setBean(bean);
        }
        
        // Stelle sicher, dass die Adresse explizit aus dem AddressField gelesen wird
        // AddressField ist ein AbstractCompositeField mit eigenem Binder,
        // daher muss der Wert explizit gesetzt werden
        if (addressForm != null) {
            de.bbajor.pvs.patient.model.Address addressValue = addressForm.getValue();
            if (addressValue != null) {
                bean.setAddress(addressValue);
            }
        }
        
        // Stelle sicher, dass ALLE Felder explizit aus den UI-Feldern gesetzt werden
        // (da Binder manchmal nicht korrekt mit den UI-Feldern synchronisiert ist, besonders bei Neuanlage)
        // WICHTIG: Explizite Setzung VOR binder.writeBean(), damit die Werte auch bei neuen Beans gesetzt werden
        
        // Name
        String nameValue = unitNameField.getValue();
        if (nameValue != null && !nameValue.trim().isEmpty()) {
            bean.setName(nameValue.trim());
        } else {
            bean.setName(null);
        }
        
        // Telefonnummer mit Formatierung
        String phoneValue = phoneField.getValue();
        if (phoneValue != null && !phoneValue.trim().isEmpty()) {
            try {
                bean.setPhone(PhoneUtils.formatPhoneNumber(phoneValue));
            } catch (Exception e) {
                // Falls Formatierung fehlschlägt, verwende den rohen Wert
                bean.setPhone(phoneValue.trim());
            }
        } else {
            bean.setPhone(null);
        }
        
        // E-Mail
        String emailValue = emailField.getValue();
        if (emailValue != null && !emailValue.trim().isEmpty()) {
            bean.setEmail(emailValue.trim());
        } else {
            bean.setEmail(null);
        }
        
        // Kontaktperson
        String contactValue = contactField.getValue();
        if (contactValue != null && !contactValue.trim().isEmpty()) {
            bean.setContact(contactValue.trim());
        } else {
            bean.setContact(null);
        }
        
        // Kontakt-Telefonnummer mit Formatierung
        String phoneContactValue = phoneContactField.getValue();
        if (phoneContactValue != null && !phoneContactValue.trim().isEmpty()) {
            try {
                bean.setPhoneContact(PhoneUtils.formatPhoneNumber(phoneContactValue));
            } catch (Exception e) {
                // Falls Formatierung fehlschlägt, verwende den rohen Wert
                bean.setPhoneContact(phoneContactValue.trim());
            }
        } else {
            bean.setPhoneContact(null);
        }
        
        // Versuche, alle anderen Felder aus dem Binder zu schreiben
        // (kann bei neuen Beans fehlschlagen, aber die explizite Setzung oben hat bereits alle Werte gesetzt)
        try {
            binder.writeBean(bean);
        } catch (Exception e) {
            // Fehler beim Schreiben - ignorieren, da alle Werte bereits explizit gesetzt wurden
        }
    }
    
    /**
     * Setzt den Binder zurück, damit hasChanges() false wird.
     */
    public void resetBinder() {
        if (binder != null && binder.getBean() != null) {
            binder.readBean(binder.getBean());
        }
    }
    
    private void loadPlannedTreatments(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null || timeSlot.getId() == null) {
            plannedTreatmentsGrid.setItems(List.of());
            plannedTreatmentsSection.setVisible(false);
            return;
        }
        
        ensureInstitutionContext();
        
        TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
        List<Treatment> treatments = treatmentRepository.findByTimeSlotId(timeSlot.getId());
        
        // Sortiere: Zuerst nach Auge (RIGHT, dann LEFT), dann nach Nachname
        List<Treatment> sortedTreatments = treatments.stream()
            .sorted(Comparator
                .comparing((Treatment t) -> {
                    SideOfEye eye = t.getSideOfEye();
                    if (eye == SideOfEye.RIGHT) return 0;
                    if (eye == SideOfEye.LEFT) return 1;
                    return 2;
                })
                .thenComparing((Treatment t) -> {
                    Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                    String lastName = patient != null && patient.getLastName() != null 
                        ? patient.getLastName() 
                        : "";
                    return lastName.toLowerCase();
                }))
            .collect(Collectors.toList());
        
        plannedTreatmentsGrid.setItems(sortedTreatments);
        
        // Section nur sichtbar machen, wenn Behandlungen vorhanden sind
        plannedTreatmentsSection.setVisible(!sortedTreatments.isEmpty());
        
        // Button ist nicht mehr nötig, da PDF-Icon direkt im Grid ist
    }
    
    private void generateCombinedReport() {
        if (selectedTimeSlot == null) {
            return;
        }
        
        try {
            ensureInstitutionContext();
            
            TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
            List<Treatment> treatments = treatmentRepository.findByTimeSlotId(selectedTimeSlot.getId());
            
            if (treatments.isEmpty()) {
                Notification.show("Keine Behandlungen für diesen Zeitslot gefunden", 3000, 
                    Notification.Position.BOTTOM_CENTER);
                return;
            }
            
            TreatmentReportService reportService = applicationContext.getBean(TreatmentReportService.class);
            com.vaadin.flow.spring.security.AuthenticationContext authContext = 
                applicationContext.getBean(com.vaadin.flow.spring.security.AuthenticationContext.class);
            String treatingDoctor = authContext.getPrincipalName().orElse("Unbekannt");
            boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
            
            byte[] pdfBytes = reportService.generatePdfReport(treatments, selectedTimeSlot, treatingDoctor, allApproved);
            
            // Erstelle Dateiname
            String dateStr = selectedTimeSlot.getDate() != null 
                ? selectedTimeSlot.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "unbekannt";
            String prefix = allApproved ? "Sammelbericht" : "Vorläufiger_Sammelbericht";
            String filename = prefix + "_" + dateStr + "_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = allApproved ? "Sammelbericht wird heruntergeladen" : "Vorläufiger Sammelbericht wird heruntergeladen";
            Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Fehler beim Generieren des Sammelberichts: " + e.getMessage(), 5000, 
                Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new java.io.ByteArrayInputStream(pdfBytes);
        });
        streamResource.setContentType("application/pdf");
        
        getUI().ifPresent(ui -> {
            StreamRegistration registration = ui.getSession().getResourceRegistry()
                .registerResource(streamResource);
            String resourceUrl = registration.getResourceUri().toString();
            
            ui.getPage().executeJs(
                "var link = document.createElement('a');" +
                "link.href = $0;" +
                "link.download = $1;" +
                "document.body.appendChild(link);" +
                "link.click();" +
                "document.body.removeChild(link);",
                resourceUrl, filename
            );
        });
    }
    
    /**
     * Erstellt eine Section (wie im IVOM-Planer) statt Accordion.
     */
    private Div createSection(String title) {
        Div section = new Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        section.getStyle().set("display", "flex");
        section.getStyle().set("flex-direction", "column");
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        section.getStyle().set("padding", "var(--lumo-space-m)");
        section.getStyle().set("box-sizing", "border-box");
        section.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        H4 sectionTitle = new H4(title);
        sectionTitle.getStyle().set("margin-top", "0");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
        sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        sectionTitle.getStyle().set("font-weight", "600");
        sectionTitle.getStyle().set("flex-shrink", "0");
        section.add(sectionTitle);
        
        return section;
    }
    
    /**
     * Behandelt das Hinzufügen eines neuen Zeitslots.
     * Erstellt die Slots aus der Konfiguration und fügt sie zum Grid hinzu.
     */
    private void handleAddTimeSlot() {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
        if (!InstitutionContext.hasInstitution()) {
            Notification.show("Fehler: InstitutionContext konnte nicht gesetzt werden. Bitte versuchen Sie es erneut.", 
                5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        List<TimeSlotConfig> configs = timeSlotConfigForm.getTimeSlotConfigList();
        if (configs.isEmpty()) {
            Notification.show("Bitte füllen Sie alle Felder aus und klicken Sie auf '+ hinzufügen'.", 
                3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
        writeBean();
        
        SurgicalCenter surgicalCenter = binder.getBean();
        if (surgicalCenter == null) {
            Notification.show("Bitte speichern Sie zuerst die Stammdaten.", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Wenn das SurgicalCenter noch nicht persistiert ist, speichere es zuerst
        if (surgicalCenter.getId() == null || surgicalCenter.getId() == -1) {
            if (saveListener != null) {
                saveListener.run();
                // Nach dem Speichern: Bean neu laden, um die ID zu erhalten
                surgicalCenter = binder.getBean();
            }
        }
        
        // Erstelle Slots aus der letzten Konfiguration
        TimeSlotConfig lastConfig = configs.get(configs.size() - 1);
        lastConfig.setSurgicalCenter(surgicalCenter);
        List<SurgicalCenterTimeSlot> newSlots = TimeSlotCreator.createTimeSlots(lastConfig);
        
        if (newSlots.isEmpty()) {
            Notification.show("Es konnten keine Zeitslots erstellt werden. Bitte überprüfen Sie die Eingaben.", 
                3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Prüfe auf Duplikate und Überschneidungen
        List<String> warnings = new ArrayList<>();
        List<SurgicalCenterTimeSlot> slotsToAdd = new ArrayList<>();
        
        for (SurgicalCenterTimeSlot newSlot : newSlots) {
            // Prüfe auf Duplikate in der neuen Liste
            boolean isDuplicate = newTimeSlotsList.stream()
                .anyMatch(existing -> isSameSlot(existing, newSlot));
            
            if (isDuplicate) {
                warnings.add("Duplikat: " + formatSlotInfo(newSlot));
                continue;
            }
            
            // Prüfe auf Überschneidungen mit neuen Slots
            SurgicalCenterTimeSlot overlappingNew = newTimeSlotsList.stream()
                .filter(existing -> hasTimeOverlap(existing, newSlot))
                .findFirst()
                .orElse(null);
            
            if (overlappingNew != null) {
                warnings.add("Überschneidung: " + formatSlotInfo(newSlot) + 
                    " überschneidet sich mit " + formatSlotInfo(overlappingNew));
            }
            
            // Prüfe auf Überschneidungen mit persistierten Slots
            if (surgicalCenter.getAvailableTimeSlots() != null) {
                SurgicalCenterTimeSlot overlappingExisting = surgicalCenter.getAvailableTimeSlots().stream()
                    .filter(existing -> hasTimeOverlap(existing, newSlot))
                    .findFirst()
                    .orElse(null);
                
                if (overlappingExisting != null) {
                    warnings.add("Überschneidung: " + formatSlotInfo(newSlot) + 
                        " überschneidet sich mit einem bereits vorhandenen Slot am " + 
                        DateAndTimeUtils.getGermanDateTimeFormatter().format(newSlot.getDate()) + 
                        " um " + (overlappingExisting.getStartTime() != null ? 
                            overlappingExisting.getStartTime().toString() : "") + 
                        " - " + (overlappingExisting.getEndTime() != null ? 
                            overlappingExisting.getEndTime().toString() : ""));
                }
            }
            
            slotsToAdd.add(newSlot);
        }
        
        // Zeige Warnungen an, aber füge Slots trotzdem hinzu
        if (!warnings.isEmpty()) {
            String warningMessage = "Warnung: Es wurden Überschneidungen oder Duplikate erkannt:\n" + 
                String.join("\n", warnings) + 
                "\n\nBitte lösen Sie die Überschneidungen auf, bevor Sie die Slots speichern.";
            Notification notification = Notification.show(warningMessage, 10000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }
        
        // Füge Slots zur Liste hinzu
        newTimeSlotsList.addAll(slotsToAdd);
        refreshNewTimeSlotsGrid();
    }
    
    /**
     * Prüft, ob eine Slot-Konfiguration gültig ist.
     */
    private boolean isValidSlotConfig(TimeSlotConfig config) {
        return config != null && 
               config.getDayOfWeek() != null &&
               config.getStartTime() != null &&
               config.getEndTime() != null &&
               config.getPeriodStartDate() != null &&
               config.getStartTime().isBefore(config.getEndTime());
    }
    
    /**
     * Prüft, ob zwei Slots identisch sind (Duplikat).
     */
    private boolean isSameSlot(SurgicalCenterTimeSlot slot1, SurgicalCenterTimeSlot slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        return slot1.getDate() != null && slot2.getDate() != null &&
               slot1.getDate().equals(slot2.getDate()) &&
               slot1.getStartTime() != null && slot2.getStartTime() != null &&
               slot1.getStartTime().equals(slot2.getStartTime()) &&
               slot1.getEndTime() != null && slot2.getEndTime() != null &&
               slot1.getEndTime().equals(slot2.getEndTime());
    }
    
    /**
     * Prüft, ob zwei Slots zeitlich überschneiden.
     */
    private boolean hasTimeOverlap(SurgicalCenterTimeSlot slot1, SurgicalCenterTimeSlot slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        if (slot1.getDate() == null || slot2.getDate() == null) {
            return false;
        }
        if (!slot1.getDate().equals(slot2.getDate())) {
            return false;
        }
        if (slot1.getStartTime() == null || slot1.getEndTime() == null ||
            slot2.getStartTime() == null || slot2.getEndTime() == null) {
            return false;
        }
        
        // Überschneidung: Start oder Ende von slot2 liegt innerhalb von slot1
        return (slot2.getStartTime().isAfter(slot1.getStartTime()) && 
                slot2.getStartTime().isBefore(slot1.getEndTime())) ||
               (slot2.getEndTime().isAfter(slot1.getStartTime()) && 
                slot2.getEndTime().isBefore(slot1.getEndTime())) ||
               (slot2.getStartTime().equals(slot1.getStartTime()) && 
                slot2.getEndTime().equals(slot1.getEndTime()));
    }
    
    /**
     * Prüft, ob ein Slot Überschneidungen hat.
     */
    private boolean hasOverlap(SurgicalCenterTimeSlot slot) {
        if (slot == null) {
            return false;
        }
        
        // Prüfe Überschneidungen mit anderen neuen Slots
        boolean hasOverlapWithNew = newTimeSlotsList.stream()
            .anyMatch(existing -> existing != slot && hasTimeOverlap(existing, slot));
        
        if (hasOverlapWithNew) {
            return true;
        }
        
        // Prüfe Überschneidungen mit persistierten Slots
        SurgicalCenter surgicalCenter = binder.getBean();
        if (surgicalCenter != null && surgicalCenter.getAvailableTimeSlots() != null) {
            return surgicalCenter.getAvailableTimeSlots().stream()
                .anyMatch(existing -> hasTimeOverlap(existing, slot));
        }
        
        return false;
    }
    
    /**
     * Formatiert Slot-Informationen für Anzeige.
     */
    private String formatSlotInfo(SurgicalCenterTimeSlot slot) {
        if (slot == null) {
            return "unbekannt";
        }
        String date = slot.getDate() != null ? 
            DateAndTimeUtils.getGermanDateTimeFormatter().format(slot.getDate()) : "unbekannt";
        String time = slot.getStartTime() != null && slot.getEndTime() != null ?
            slot.getStartTime().toString() + " - " + slot.getEndTime().toString() : "unbekannt";
        return date + " " + time;
    }
    
    /**
     * Aktualisiert das Grid "Neue Zeitslots".
     */
    private void refreshNewTimeSlotsGrid() {
        newTimeSlotsGrid.setItems(newTimeSlotsList);
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            try {
                String username = adapter.getUsername();
                UserAccountRepository userAccountRepository = applicationContext.getBean(UserAccountRepository.class);
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                }
            } catch (Exception e) {
                // Log error but continue
            }
        }
    }

}
