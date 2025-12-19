package de.bbajor.pvs.surgicalcenter.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import com.vaadin.flow.component.grid.Grid;

import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.base.ui.component.WeekNavigationSection;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.PhoneUtils;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
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
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;

public class SurgicalCenterLayout extends HorizontalLayout {

    private final Binder<SurgicalCenter> binder = new Binder<>(SurgicalCenter.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Name Kontaktperson");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontaktperson");
    private final AddressField<Address> addressForm = new AddressField<>("Adresse", new Address());
    private TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    private final Grid<Treatment> plannedTreatmentsGrid = new Grid<>();
    private final Grid<SurgicalCenterTimeSlot> newTimeSlotsGrid = new Grid<>();
    private final ApplicationContext applicationContext;
    private SurgicalCenterTimeSlot selectedTimeSlot;
    private final List<SurgicalCenterTimeSlot> newTimeSlotsList = new ArrayList<>();
    private Div plannedTreatmentsSection;
    private Runnable binderChangeListener; // Listener für Binder-Änderungen
    private boolean hasTreatmentsForSelectedSlot = false;

    // Kalenderansicht für vorhandene Zeitslots
    private LocalDate currentWeekStart;
    private Div weekCalendarContainer;
    private Div legendContainer;
    private WeekNavigationSection weekNavigationSection;
    private List<SurgicalCenterTimeSlot> calendarTimeSlots = new ArrayList<>();
    private final Map<SurgicalCenter, String> surgicalCenterColors = new HashMap<>();
    private final Map<SurgicalCenter, Boolean> surgicalCenterVisibility = new HashMap<>();

    // Dialog zum Hinzufügen neuer Zeitslots
    private Dialog timeSlotsDialog;
    private Button deleteTimeSlotButton;

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

        // Root-Layout ohne Tabs
        VerticalLayout root = new VerticalLayout();
        root.setSizeFull();
        root.setPadding(true);
        root.setSpacing(true);

        // Hauptbereich "Stammdaten"
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

        // Section "Vorhandene Zeitslots" über die volle Breite mit Kalenderansicht
        Div availableSlotsSection = createSection("Vorhandene Zeitslots");
        availableSlotsSection.setHeightFull();

        // Header mit Titel und "+"-Button für neuen Zeitslot-Dialog
        HorizontalLayout slotsHeader = new HorizontalLayout();
        slotsHeader.setWidthFull();
        slotsHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        Div slotsTitle = new Div("Vorhandene Zeitslots");
        slotsTitle.getStyle().set("font-weight", "bold");
        slotsTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");

        deleteTimeSlotButton = new Button("Zeitslot löschen", VaadinIcon.TRASH.create());
        deleteTimeSlotButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        deleteTimeSlotButton.setEnabled(false);
        deleteTimeSlotButton.addClickListener(e -> handleDeleteSelectedTimeSlot());

        Button addSlotsButton = new Button(VaadinIcon.PLUS.create());
        addSlotsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSlotsButton.getElement().setProperty("title", "Neue Zeitslots hinzufügen");

        slotsHeader.add(slotsTitle);
        slotsHeader.setFlexGrow(1, slotsTitle);
        slotsHeader.add(deleteTimeSlotButton);
        slotsHeader.add(addSlotsButton);
        availableSlotsSection.add(slotsHeader);

        // Bereich für Wochennavigation und Legende
        VerticalLayout topCalendarLayout = new VerticalLayout();
        topCalendarLayout.setWidthFull();
        topCalendarLayout.setSpacing(true);
        topCalendarLayout.setPadding(false);

        // Woche-Navigation
        currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        weekNavigationSection = new WeekNavigationSection("Wochenliste", currentWeekStart, weekStart -> {
            currentWeekStart = weekStart;
            refreshWeekCalendar();
        });
        weekNavigationSection.getStyle().set("flex-shrink", "0");
        topCalendarLayout.add(weekNavigationSection);

        // Legende für operative Einrichtungen
        legendContainer = new Div();
        legendContainer.setWidthFull();
        legendContainer.getStyle().set("flex-shrink", "0");
        topCalendarLayout.add(legendContainer);

        availableSlotsSection.add(topCalendarLayout);

        // Kalender-Container
        weekCalendarContainer = new Div();
        weekCalendarContainer.setWidthFull();
        weekCalendarContainer.getStyle()
                .set("min-height", "600px")
                .set("flex-grow", "1")
                .set("overflow", "hidden");
        availableSlotsSection.add(weekCalendarContainer);

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
        plannedTreatmentsGrid.setSelectionMode(Grid.SelectionMode.NONE);
        
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

        // Details-Layout zusammensetzen: Stammdaten + Kalender + geplante Behandlungen
        detailsLayout.add(availableSlotsSection);
        detailsLayout.add(plannedTreatmentsSection);
        detailsLayout.setFlexGrow(1, availableSlotsSection);

        // Grid "Neue Zeitslots" für noch nicht persistierte Slots (im Dialog verwendet)
        Div newTimeSlotsSection = createSection("Neue Zeitslots");

        // Grid konfigurieren
        newTimeSlotsGrid.setSizeFull();
        newTimeSlotsGrid.setMinHeight("400px");
        newTimeSlotsGrid.setSelectionMode(com.vaadin.flow.component.grid.Grid.SelectionMode.MULTI);
        
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

        // Setze Callback für TimeSlotConfigForm
        timeSlotConfigForm.setOnSlotAddedCallback(this::handleAddTimeSlot);

        // Dialog für neue Zeitslots erstellen
        this.timeSlotsDialog = createTimeSlotsDialog(newTimeSlotsSection);
        addSlotsButton.addClickListener(e -> {
            // Beim Öffnen sicherstellen, dass Liste und Grid aktuell sind
            refreshNewTimeSlotsGrid();
            timeSlotsDialog.open();
        });

        root.add(detailsLayout);
        root.setFlexGrow(1, detailsLayout);

        add(root);
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
            reloadCalendarSlots();
            refreshWeekCalendar();
        }
    }
    
    /**
     * Lädt die Zeitslots für den Kalender neu aus dem aktuell gesetzten SurgicalCenter.
     */
    private void reloadCalendarSlots() {
        ensureInstitutionContext();
        SurgicalCenterService service = applicationContext.getBean(SurgicalCenterService.class);

        // Lade alle Slots aller Einrichtungen dieser Institution inkl. Patientenzahl
        LocalDate start = LocalDate.now();
        calendarTimeSlots = service.getAllTimeSlotsForCurrentInstitutionWithTreatmentCount(start,
                TimePeriod.SIX_MONTHS);

        // Sichtbarkeiten/Farben neu initialisieren: nur aktuelle Einrichtung sichtbar, alle anderen zunächst ausgeblendet
        initializeSurgicalCenterColors();
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
    
    /**
     * Erstellt den Dialog zum Hinzufügen neuer Zeitslots.
     */
    private Dialog createTimeSlotsDialog(Div newTimeSlotsSection) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("OP-Slots hinzufügen");
        dialog.setWidth("1200px");
        dialog.setMaxWidth("95vw");
        dialog.setHeight("90vh");
        dialog.setCloseOnOutsideClick(false);

        // X-Icon im Header
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> dialog.close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        dialog.getHeader().add(closeIconButton);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);

        // Section "OP-Slot hinzufügen"
        Div timeSlotCreationSection = createSection("OP-Slot hinzufügen");
        timeSlotCreationSection.getStyle().set("max-height", "300px");
        timeSlotCreationSection.getStyle().set("overflow", "auto");
        timeSlotCreationSection.add(timeSlotConfigForm);

        content.add(timeSlotCreationSection, newTimeSlotsSection);
        content.setFlexGrow(1, newTimeSlotsSection);

        dialog.add(content);

        // Footer-Buttons
        Button cancelButton = new Button("Abbrechen", e -> {
            newTimeSlotsList.clear();
            refreshNewTimeSlotsGrid();
            dialog.close();
        });

        Button saveButton = new Button("Speichern", e -> {
            if (saveNewTimeSlots()) {
                dialog.close();
                reloadCalendarSlots();
                refreshWeekCalendar();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, saveButton);
        return dialog;
    }

    private void loadPlannedTreatments(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null || timeSlot.getId() == null) {
            plannedTreatmentsGrid.setItems(List.of());
            plannedTreatmentsSection.setVisible(false);
            hasTreatmentsForSelectedSlot = false;
            updateDeleteButtonState();
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
        
        hasTreatmentsForSelectedSlot = !sortedTreatments.isEmpty();
        plannedTreatmentsSection.setVisible(hasTreatmentsForSelectedSlot);
        updateDeleteButtonState();
        
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
        List<TimeSlotConfig> configs = timeSlotConfigForm.getTimeSlotConfigList();
        if (configs.isEmpty()) {
            Notification.show("Bitte füllen Sie alle Felder aus und klicken Sie auf '+ hinzufügen'.", 
                3000, Notification.Position.MIDDLE);
            return;
        }
        
        // Erstelle Slots aus der letzten Konfiguration
        TimeSlotConfig lastConfig = configs.get(configs.size() - 1);
        SurgicalCenter surgicalCenter = binder.getBean();
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
     * Persistiert die in newTimeSlotsList enthaltenen Zeitslots für das aktuelle SurgicalCenter.
     * Verhindert zeitliche Überschneidungen mit bestehenden Slots dieser Einrichtung.
     */
    private boolean saveNewTimeSlots() {
        if (newTimeSlotsList.isEmpty()) {
            Notification.show("Es wurden keine neuen Zeitslots erstellt.", 3000, Notification.Position.MIDDLE);
            return false;
        }

        // Harte Validierung: keine Überschneidungen mit bestehenden Slots der Einrichtung
        List<SurgicalCenterTimeSlot> invalid = newTimeSlotsList.stream()
                .filter(this::hasOverlap)
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            Notification notification = Notification.show(
                    "Es gibt zeitliche Überschneidungen mit bestehenden Zeitslots. "
                            + "Bitte korrigieren Sie die Eingaben.",
                    7000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        // Optionale Warnung: Überschneidungen mit Zeitslots anderer Einrichtungen
        checkOverlapsWithOtherCenters(newTimeSlotsList);

        ensureInstitutionContext();
        writeBean();

        SurgicalCenter surgicalCenter = binder.getBean();
        if (surgicalCenter == null) {
            Notification.show("Fehler: Keine operative Einrichtung geladen.", 3000,
                    Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        try {
            SurgicalCenterService service = applicationContext.getBean(SurgicalCenterService.class);
            SurgicalCenter saved = service.saveTimeSlotsAndSurgicalCenter(new ArrayList<>(newTimeSlotsList),
                    surgicalCenter);

            // Bean & UI aktualisieren
            setBean(saved);
            newTimeSlotsList.clear();
            refreshNewTimeSlotsGrid();

            Notification.show("Zeitslots wurden gespeichert.", 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Fehler beim Speichern der Zeitslots: " + e.getMessage(), 7000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
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
     * Prüft auf zeitliche Überschneidungen der neu erstellten Zeitslots mit Slots anderer Einrichtungen
     * und zeigt ggf. eine Warnung an (nicht blockierend).
     */
    private void checkOverlapsWithOtherCenters(List<SurgicalCenterTimeSlot> newSlots) {
        try {
            ensureInstitutionContext();
            SurgicalCenterService service = applicationContext.getBean(SurgicalCenterService.class);

            LocalDate start = newSlots.stream()
                    .map(SurgicalCenterTimeSlot::getDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now());

            // Lade alle verfügbaren Zeitslots aller Einrichtungen der Institution
            var allSlots = service.findAvailableTimeSlotsFilteredBy(start, TimePeriod.SIX_MONTHS, null);

            SurgicalCenter currentCenter = binder.getBean();

            List<String> overlaps = new ArrayList<>();
            for (SurgicalCenterTimeSlot newSlot : newSlots) {
                for (SurgicalCenterTimeSlot other : allSlots) {
                    if (other.getSurgicalCenter() == null) {
                        continue;
                    }
                    if (currentCenter != null
                            && other.getSurgicalCenter().getId().equals(currentCenter.getId())) {
                        // eigene Einrichtung – dafür gibt es die harte hasOverlap-Validierung
                        continue;
                    }
                    if (hasTimeOverlap(other, newSlot)) {
                        overlaps.add(
                                formatSlotInfo(newSlot) + " kollidiert mit "
                                        + formatSlotInfo(other) + " in "
                                        + other.getSurgicalCenter().getName());
                    }
                }
            }

            if (!overlaps.isEmpty()) {
                String msg = "Achtung: Zeitliche Überschneidungen mit anderen operativen Einrichtungen:\n"
                        + String.join("\n", overlaps);
                Notification notification = Notification.show(msg, 10000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        } catch (Exception e) {
            // nicht blockierend – nur loggen, falls gewünscht
        }
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
     * Erstellt eine Legende für operative Einrichtungen mit Checkboxen (Kalenderansicht).
     */
    private Div createLegend() {
        if (calendarTimeSlots.isEmpty()) {
            return null;
        }

        List<SurgicalCenter> centers = calendarTimeSlots.stream()
                .map(SurgicalCenterTimeSlot::getSurgicalCenter)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(SurgicalCenter::getName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        if (centers.isEmpty()) {
            return null;
        }

        Div legend = new Div();
        legend.addClassName("dialog-section");
        legend.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        H4 legendTitle = new H4("Operative Einrichtungen");
        legendTitle.getStyle()
                .set("margin-top", "0")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "600");
        legend.add(legendTitle);

        Div legendContent = new Div();
        legendContent.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "var(--lumo-space-s)")
                .set("width", "100%");

        SurgicalCenter currentCenter = binder.getBean();

        for (SurgicalCenter center : centers) {
            com.vaadin.flow.component.checkbox.Checkbox checkbox = new com.vaadin.flow.component.checkbox.Checkbox();
            checkbox.setLabel(center.getName());
            boolean isCurrent = currentCenter != null && currentCenter.getId() != null
                    && center.getId() != null
                    && currentCenter.getId().equals(center.getId());
            boolean value = surgicalCenterVisibility.getOrDefault(center, Boolean.TRUE);
            if (isCurrent) {
                value = true;
                checkbox.setEnabled(false); // aktuelle Einrichtung ist immer sichtbar
            }
            checkbox.setValue(value);
            if (!isCurrent) {
                checkbox.addValueChangeListener(e -> {
                    surgicalCenterVisibility.put(center, e.getValue());
                    refreshWeekCalendar();
                });
            }

            Div colorBox = new Div();
            String color = surgicalCenterColors.getOrDefault(center, "var(--lumo-contrast-20pct)");
            colorBox.getStyle()
                    .set("width", "16px")
                    .set("height", "16px")
                    .set("border-radius", "2px")
                    .set("background-color", color)
                    .set("border", "1px solid var(--lumo-contrast-30pct)")
                    .set("display", "inline-block")
                    .set("margin-right", "var(--lumo-space-xs)")
                    .set("vertical-align", "middle")
                    .set("flex-shrink", "0");

            HorizontalLayout legendItem = new HorizontalLayout();
            legendItem.setSpacing(true);
            legendItem.setAlignItems(FlexComponent.Alignment.CENTER);
            legendItem.setPadding(false);
            legendItem.getStyle()
                    .set("flex-shrink", "0")
                    .set("margin", "0");
            legendItem.add(colorBox, checkbox);

            legendContent.add(legendItem);
        }

        // Button: Nur aktuelle Einrichtung anzeigen
        if (currentCenter != null && currentCenter.getId() != null) {
            Button onlyCurrentButton = new Button("Nur aktuelle Einrichtung", e -> {
                surgicalCenterVisibility.replaceAll((center, v) -> {
                    if (center != null && center.getId() != null
                            && center.getId().equals(currentCenter.getId())) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                });
                refreshWeekCalendar();
            });
            onlyCurrentButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            onlyCurrentButton.getStyle().set("margin-bottom", "var(--lumo-space-s)");
            legend.add(onlyCurrentButton);
        }

        legend.add(legendContent);
        return legend;
    }

    /**
     * Initialisiert Farben und Sichtbarkeiten für operative Einrichtungen in der Kalenderansicht.
     * Standard: nur die aktuelle Einrichtung ist ausgewählt.
     */
    private void initializeSurgicalCenterColors() {
        surgicalCenterColors.clear();
        surgicalCenterVisibility.clear();

        List<SurgicalCenter> centers = calendarTimeSlots.stream()
                .map(SurgicalCenterTimeSlot::getSurgicalCenter)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        String[] colors = {
                "var(--lumo-primary-color-10pct)",
                "var(--lumo-success-color-10pct)",
                "var(--lumo-warning-color-10pct)",
                "var(--lumo-error-color-10pct)",
                "#E3F2FD",
                "#FFF3E0",
                "#F3E5F5",
                "#E8F5E9"
        };

        SurgicalCenter currentCenter = binder.getBean();

        for (int i = 0; i < centers.size(); i++) {
            SurgicalCenter center = centers.get(i);
            surgicalCenterColors.put(center, colors[i % colors.length]);
            // Nur aktuelle Einrichtung standardmäßig sichtbar, andere ausblenden
            boolean visible = currentCenter != null && currentCenter.getId() != null
                    && center.getId() != null
                    && currentCenter.getId().equals(center.getId());
            surgicalCenterVisibility.put(center, visible);
        }
    }

    /**
     * Aktualisiert die Kalenderansicht für die aktuelle Woche.
     */
    private void refreshWeekCalendar() {
        if (weekCalendarContainer == null) {
            return;
        }

        weekCalendarContainer.removeAll();

        if (calendarTimeSlots.isEmpty()) {
            Span noSlotsMessage = new Span("Keine Zeitslots verfügbar.");
            weekCalendarContainer.add(noSlotsMessage);
            return;
        }

        LocalDate weekStart = currentWeekStart != null
                ? currentWeekStart
                : LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        H3 weekHeader = new H3("Woche "
                + weekStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
                + " - "
                + weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)));
        weekCalendarContainer.add(weekHeader);

        // Slots nach Datum der Woche gruppieren und nach sichtbaren Einrichtungen filtern
        Map<LocalDate, List<SurgicalCenterTimeSlot>> slotsByDate = calendarTimeSlots.stream()
                .filter(slot -> {
                    LocalDate date = slot.getDate();
                    if (date == null || date.isBefore(weekStart) || date.isAfter(weekEnd)) {
                        return false;
                    }
                    SurgicalCenter center = slot.getSurgicalCenter();
                    return center != null && surgicalCenterVisibility.getOrDefault(center, Boolean.FALSE);
                })
                .collect(Collectors.groupingBy(SurgicalCenterTimeSlot::getDate));

        HorizontalLayout weekGrid = createWeekGrid(weekStart, slotsByDate);
        weekGrid.setWidthFull();
        weekGrid.setHeightFull();
        weekCalendarContainer.add(weekGrid);
        weekCalendarContainer.setHeightFull();

        // Legende aktualisieren
        if (legendContainer != null) {
            legendContainer.removeAll();
            Div legend = createLegend();
            if (legend != null) {
                legendContainer.add(legend);
            }
        }

        setupSynchronizedScrolling();
    }

    private String getGermanDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Montag";
            case TUESDAY -> "Dienstag";
            case WEDNESDAY -> "Mittwoch";
            case THURSDAY -> "Donnerstag";
            case FRIDAY -> "Freitag";
            case SATURDAY -> "Samstag";
            case SUNDAY -> "Sonntag";
        };
    }

    private HorizontalLayout createWeekGrid(LocalDate weekStart,
            Map<LocalDate, List<SurgicalCenterTimeSlot>> slotsByDate) {
        HorizontalLayout weekLayout = new HorizontalLayout();
        weekLayout.setWidthFull();
        weekLayout.setSpacing(false);
        weekLayout.setPadding(false);
        weekLayout.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.Gap.SMALL);

        // Zeitbereich bestimmen
        LocalTime earliestTime = calendarTimeSlots.stream()
                .map(SurgicalCenterTimeSlot::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalTime::compareTo)
                .orElse(LocalTime.of(6, 0))
                .minusHours(1);
        LocalTime latestTime = calendarTimeSlots.stream()
                .map(slot -> slot.getEndTime() != null ? slot.getEndTime() : slot.getStartTime().plusHours(1))
                .filter(Objects::nonNull)
                .max(LocalTime::compareTo)
                .orElse(LocalTime.of(20, 0))
                .plusHours(1);

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<SurgicalCenterTimeSlot> daySlots = slotsByDate.getOrDefault(day, new ArrayList<>());
            VerticalLayout dayColumn = createDayColumn(day, earliestTime, latestTime, daySlots);
            weekLayout.add(dayColumn);
            weekLayout.setFlexGrow(1, dayColumn);
        }

        return weekLayout;
    }

    private VerticalLayout createDayColumn(LocalDate day, LocalTime earliestTime, LocalTime latestTime,
            List<SurgicalCenterTimeSlot> daySlots) {
        VerticalLayout column = new VerticalLayout();
        column.setPadding(false);
        column.setSpacing(false);
        column.setWidth(null);
        column.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.Border.ALL,
                com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius.SMALL);
        column.getStyle()
                .set("min-width", "0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%");

        String dayName = getGermanDayName(day.getDayOfWeek());
        H4 dayHeader = new H4(
                dayName + "\n" + day.format(DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMAN)));
        dayHeader.getStyle()
                .set("text-align", "center")
                .set("padding", "var(--lumo-space-s)")
                .set("margin", "0")
                .set("background-color",
                        day.equals(LocalDate.now())
                                ? "var(--lumo-primary-color-10pct)"
                                : "var(--lumo-contrast-5pct)")
                .set("flex-shrink", "0");
        column.add(dayHeader);

        Div timeSlotsContainer = new Div();
        timeSlotsContainer.setWidthFull();
        timeSlotsContainer.getStyle()
                .set("overflow-y", "auto")
                .set("position", "relative")
                .set("min-height", "400px")
                .set("flex-grow", "1")
                .set("flex-shrink", "1");

        String containerId = "day-column-" + day.toString();
        timeSlotsContainer.setId(containerId);

        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, latestTime);
        if (totalMinutes == 0) {
            totalMinutes = 1;
        }

        // Zeit-Labels
        LocalTime currentTime = earliestTime;
        while (currentTime.isBefore(latestTime)) {
            if (currentTime.getMinute() == 0) {
                Span timeLabel = new Span(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                double topPercent = (double) java.time.temporal.ChronoUnit.MINUTES
                        .between(earliestTime, currentTime) / totalMinutes * 100;
                timeLabel.getStyle()
                        .set("font-size", "var(--lumo-font-size-xs)")
                        .set("color", "var(--lumo-contrast-70pct)")
                        .set("position", "absolute")
                        .set("top", topPercent + "%")
                        .set("left", "0")
                        .set("width", "45px")
                        .set("text-align", "right")
                        .set("padding-right", "4px")
                        .set("z-index", "1")
                        .set("pointer-events", "none");
                timeSlotsContainer.add(timeLabel);
            }
            currentTime = currentTime.plusMinutes(15);
        }

        List<List<SurgicalCenterTimeSlot>> slotGroups = groupOverlappingSlots(daySlots);
        for (int groupIndex = 0; groupIndex < slotGroups.size(); groupIndex++) {
            List<SurgicalCenterTimeSlot> slotGroup = slotGroups.get(groupIndex);
            for (SurgicalCenterTimeSlot slot : slotGroup) {
                Div slotBlock = createTimeSlotBlock(slot, earliestTime, totalMinutes,
                        slotGroups.size(), groupIndex);
                timeSlotsContainer.add(slotBlock);
            }
        }

        column.add(timeSlotsContainer);
        column.setFlexGrow(1, timeSlotsContainer);

        return column;
    }

    private List<List<SurgicalCenterTimeSlot>> groupOverlappingSlots(List<SurgicalCenterTimeSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        List<SurgicalCenterTimeSlot> sorted = new ArrayList<>(slots);
        sorted.sort((a, b) -> {
            LocalTime sa = a.getStartTime();
            LocalTime sb = b.getStartTime();
            if (sa == null && sb == null) {
                return 0;
            }
            if (sa == null) {
                return 1;
            }
            if (sb == null) {
                return -1;
            }
            int cmp = sa.compareTo(sb);
            if (cmp != 0) {
                return cmp;
            }
            LocalTime ea = a.getEndTime() != null ? a.getEndTime() : sa.plusHours(1);
            LocalTime eb = b.getEndTime() != null ? b.getEndTime() : sb.plusHours(1);
            return ea.compareTo(eb);
        });

        List<List<SurgicalCenterTimeSlot>> groups = new ArrayList<>();

        for (SurgicalCenterTimeSlot slot : sorted) {
            boolean added = false;
            for (List<SurgicalCenterTimeSlot> group : groups) {
                boolean overlaps = false;
                LocalTime slotStart = slot.getStartTime();
                LocalTime slotEnd = slot.getEndTime() != null ? slot.getEndTime()
                        : slotStart != null ? slotStart.plusHours(1) : null;
                if (slotStart == null || slotEnd == null) {
                    continue;
                }

                for (SurgicalCenterTimeSlot groupSlot : group) {
                    LocalTime groupStart = groupSlot.getStartTime();
                    LocalTime groupEnd = groupSlot.getEndTime() != null ? groupSlot.getEndTime()
                            : groupStart != null ? groupStart.plusHours(1) : null;
                    if (groupStart == null || groupEnd == null) {
                        continue;
                    }

                    if ((slotStart.isBefore(groupEnd) && slotEnd.isAfter(groupStart))
                            || (groupStart.isBefore(slotEnd) && groupEnd.isAfter(slotStart))) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    group.add(slot);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<SurgicalCenterTimeSlot> newGroup = new ArrayList<>();
                newGroup.add(slot);
                groups.add(newGroup);
            }
        }

        return groups;
    }

    private Div createTimeSlotBlock(SurgicalCenterTimeSlot timeSlot, LocalTime earliestTime,
            long totalMinutes, int totalGroups, int groupIndex) {
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime() != null ? timeSlot.getEndTime()
                : startTime != null ? startTime.plusHours(1) : null;
        if (startTime == null || endTime == null) {
            Div invalid = new Div();
            invalid.setText("Ungültiger Slot");
            return invalid;
        }

        long startMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, startTime);
        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);

        double topPercent = (double) startMinutes / totalMinutes * 100;
        double heightPercent = (double) durationMinutes / totalMinutes * 100;

        double leftPercent = 50.0;
        double widthPercent = 50.0;
        if (totalGroups > 1) {
            widthPercent = 50.0 / totalGroups;
            leftPercent = 50.0 + (groupIndex * widthPercent);
        }

        Div slotBlock = new Div();
        slotBlock.getStyle()
                .set("position", "absolute")
                .set("top", topPercent + "%")
                .set("left", leftPercent + "%")
                .set("width", widthPercent + "%")
                .set("height", heightPercent + "%")
                .set("min-height", "40px")
                .set("padding", "var(--lumo-space-xs)")
                .set("box-sizing", "border-box")
                .set("cursor", "pointer");

        Span slotLabel = createTimeSlotLabel(timeSlot);
        slotBlock.add(slotLabel);

        return slotBlock;
    }

    private Span createTimeSlotLabel(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null) {
            return new Span("Ungültiger Termin");
        }

        int patientCount = timeSlot.getPatientCount();
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime() != null ? timeSlot.getEndTime()
                : startTime != null ? startTime.plusHours(1) : null;
        if (startTime == null || endTime == null) {
            return new Span("Ungültiger Termin");
        }

        String centerName = timeSlot.getSurgicalCenter() != null
                ? timeSlot.getSurgicalCenter().getName()
                : "Unbekannt";

        long durationHours = java.time.temporal.ChronoUnit.HOURS.between(startTime, endTime);
        if (durationHours == 0) {
            durationHours = 1;
        }
        double patientsPerHour = (double) patientCount / durationHours;

        String labelText = centerName;
        if (patientCount > 0) {
            labelText += "\n" + patientCount + " Patient" + (patientCount > 1 ? "en" : "");
        } else {
            labelText += "\nFrei";
        }

        boolean isSelected = selectedTimeSlot != null
                && selectedTimeSlot.getId() != null
                && timeSlot.getId() != null
                && selectedTimeSlot.getId().equals(timeSlot.getId());

        String backgroundColor;
        String textColor;
        if (isSelected) {
            backgroundColor = "var(--lumo-primary-color)";
            textColor = "var(--lumo-primary-contrast-color)";
        } else {
            SurgicalCenter center = timeSlot.getSurgicalCenter();
            backgroundColor = surgicalCenterColors.getOrDefault(center, "var(--lumo-contrast-20pct)");

            if (patientCount == 0) {
                textColor = "var(--lumo-success-color)";
            } else if (patientsPerHour > 30) {
                textColor = "var(--lumo-error-color)";
            } else if (patientsPerHour > 20) {
                textColor = "#ffffff";
            } else if (patientsPerHour > 15) {
                textColor = "var(--lumo-warning-color)";
            } else {
                textColor = "var(--lumo-success-color)";
            }
        }

        Span label = new Span(labelText);
        label.getStyle()
                .set("background-color", backgroundColor)
                .set("color", textColor)
                .set("padding", "4px 6px")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("border",
                        isSelected ? "2px solid var(--lumo-primary-color)"
                                : "1px solid var(--lumo-contrast-20pct)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("cursor", "pointer")
                .set("display", "block")
                .set("width", "100%")
                .set("height", "100%")
                .set("box-sizing", "border-box")
                .set("white-space", "normal")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("line-height", "1.3");

        label.addClickListener(e -> {
            boolean sameSlotSelected = selectedTimeSlot != null
                    && selectedTimeSlot.getId() != null
                    && timeSlot.getId() != null
                    && selectedTimeSlot.getId().equals(timeSlot.getId());

            // Immer aktuellen Slot setzen und Behandlungen laden
            selectedTimeSlot = timeSlot;
            loadPlannedTreatments(timeSlot);

            if (sameSlotSelected) {
                // Zweiter Klick auf denselben Slot -> Detaildialog
                openTimeSlotPatientsDialog(timeSlot);
            } else {
                // Erster Klick auf einen Slot -> nur auswählen und Kalender aktualisieren
                refreshWeekCalendar();
            }
        });

        return label;
    }

    /**
     * Aktiviert/Deaktiviert den Lösch-Button je nach ausgewähltem Zeitslot.
     * Der Button ist nur aktiv für zukünftige Slots ohne geplante Behandlungen.
     */
    private void updateDeleteButtonState() {
        if (deleteTimeSlotButton == null) {
            return;
        }
        boolean enabled = false;
        if (selectedTimeSlot != null && selectedTimeSlot.getDate() != null) {
            LocalDate today = LocalDate.now();
            boolean isInFutureOrToday = !selectedTimeSlot.getDate().isBefore(today);
            enabled = isInFutureOrToday && !hasTreatmentsForSelectedSlot;
        }
        deleteTimeSlotButton.setEnabled(enabled);
    }

    /**
     * Versucht, den aktuell ausgewählten Zeitslot zu löschen.
     * Falls Behandlungen vorhanden sind, wird der Slot stattdessen deaktiviert.
     */
    private void handleDeleteSelectedTimeSlot() {
        if (selectedTimeSlot == null || selectedTimeSlot.getId() == null) {
            Notification.show("Kein Zeitslot ausgewählt.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            ensureInstitutionContext();

            TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
            List<Treatment> treatments = treatmentRepository.findByTimeSlotId(selectedTimeSlot.getId());

            SurgicalCenterService surgicalCenterService = applicationContext.getBean(SurgicalCenterService.class);

            if (treatments.isEmpty()) {
                // Keine Behandlungen -> Slot löschen
                surgicalCenterService.deleteTimeSlot(selectedTimeSlot.getId());
                Notification.show("Zeitslot wurde gelöscht.", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // Behandlungen vorhanden -> Slot deaktivieren
                surgicalCenterService.deactivateTimeSlot(selectedTimeSlot.getId());
                Notification.show(
                        "Zeitslot konnte nicht gelöscht werden, wurde aber deaktiviert, da Behandlungen geplant sind.",
                        6000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            }

            // Kalender und Behandlungsübersicht aktualisieren
            selectedTimeSlot = null;
            hasTreatmentsForSelectedSlot = false;
            plannedTreatmentsGrid.setItems(List.of());
            plannedTreatmentsSection.setVisible(false);
            reloadCalendarSlots();
            refreshWeekCalendar();
            updateDeleteButtonState();
        } catch (Exception ex) {
            Notification notification = Notification.show(
                    "Fehler beim Löschen/Deaktivieren des Zeitslots: " + ex.getMessage(),
                    7000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Öffnet einen readonly-Dialog mit allen Patienten, die im übergebenen Zeitslot eingeplant sind.
     */
    private void openTimeSlotPatientsDialog(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null || timeSlot.getId() == null) {
            Notification.show("Für diesen Zeitslot liegen keine Daten vor.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            ensureInstitutionContext();
            TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
            List<Treatment> treatments = treatmentRepository.findByTimeSlotId(timeSlot.getId());

            Dialog dialog = new Dialog();
            dialog.setWidth("900px");
            dialog.setHeight("600px");
            dialog.setCloseOnOutsideClick(false);

            String dateStr = timeSlot.getDate() != null
                    ? timeSlot.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
                    : "Unbekanntes Datum";
            String timeStr;
            if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                timeStr = timeSlot.getStartTime().toString() + " - " + timeSlot.getEndTime().toString() + " Uhr";
            } else {
                timeStr = "Uhrzeit unbekannt";
            }
            String centerName = timeSlot.getSurgicalCenter() != null
                    ? timeSlot.getSurgicalCenter().getName()
                    : "Unbekannte Einrichtung";

            dialog.setHeaderTitle("Geplante Behandlungen - " + dateStr + " (" + timeStr + ", " + centerName + ")");

            Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> dialog.close());
            closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            closeIconButton.getStyle().set("margin-left", "auto");
            dialog.getHeader().add(closeIconButton);

            VerticalLayout content = new VerticalLayout();
            content.setSizeFull();
            content.setPadding(true);
            content.setSpacing(true);

            if (treatments.isEmpty()) {
                content.add(new Span("Für diesen Zeitslot sind keine Behandlungen geplant."));
            } else {
                Grid<Treatment> grid = new Grid<>(Treatment.class, false);
                grid.setSizeFull();

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);

                grid.addColumn(t -> {
                    Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                    if (patient == null) {
                        return "-";
                    }
                    String lastName = patient.getLastName() != null ? patient.getLastName() : "";
                    String firstName = patient.getFirstName() != null ? patient.getFirstName() : "";
                    return (lastName + " " + firstName).trim();
                }).setHeader("Patient").setAutoWidth(true);

                grid.addColumn(t -> {
                    Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                    if (patient != null && patient.getBirth() != null) {
                        return dateFormatter.format(patient.getBirth());
                    }
                    return "-";
                }).setHeader("Geburtsdatum").setAutoWidth(true);

                grid.addColumn(t -> {
                    Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                    if (patient != null && patient.getHealthInsurance() != null
                            && patient.getHealthInsurance().getBillingCarrierName() != null) {
                        return patient.getHealthInsurance().getBillingCarrierName();
                    }
                    return "-";
                }).setHeader("Versicherung").setAutoWidth(true);

                grid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
                        .setHeader("Auge").setAutoWidth(true);

                grid.addColumn(t -> {
                    if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                        return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                    }
                    return "-";
                }).setHeader("Medikament").setAutoWidth(true);

                grid.setItems(treatments);
                content.add(grid);
                content.setFlexGrow(1, grid);
            }

            dialog.add(content);

            Button closeButton = new Button("Schließen", e -> dialog.close());
            dialog.getFooter().add(closeButton);

            dialog.open();
        } catch (Exception ex) {
            Notification notification = Notification.show(
                    "Fehler beim Laden der Behandlungen: " + ex.getMessage(),
                    7000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Richtet synchrones vertikales Scrollen für alle Tages-Spalten ein.
     */
    private void setupSynchronizedScrolling() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "setTimeout(() => {"
                        + "  const containers = document.querySelectorAll('[id^=\"day-column-\"]');"
                        + "  if (containers.length === 0) return;"
                        + "  let isScrolling = false;"
                        + "  containers.forEach(container => {"
                        + "    container.addEventListener('scroll', function(e) {"
                        + "      if (isScrolling) return;"
                        + "      isScrolling = true;"
                        + "      const scrollTop = e.target.scrollTop;"
                        + "      containers.forEach(other => {"
                        + "        if (other !== e.target) {"
                        + "          other.scrollTop = scrollTop;"
                        + "        }"
                        + "      });"
                        + "      setTimeout(() => { isScrolling = false; }, 10);"
                        + "    });"
                        + "  });"
                        + "}, 100);"
        ));
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
