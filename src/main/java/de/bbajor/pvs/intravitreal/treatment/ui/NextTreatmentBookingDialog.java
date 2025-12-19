package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

import de.bbajor.pvs.base.ui.component.WeekNavigationSection;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.service.UserAccountService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

public class NextTreatmentBookingDialog extends Dialog {

    private static final Logger LOG = LogManager.getLogger(NextTreatmentBookingDialog.class);

    private final TreatmentPlan treatmentPlan;
    private final SideOfEye sideOfEye;
    private final ApplicationContext context;
    private final TreatmentPlanPresenter presenter;
    private InstitutionRepository institutionRepository;
    private final Consumer<Treatment> onTreatmentCreated;

    private RadioButtonGroup<String> intervalTypeGroup;
    private ComboBox<Integer> weeksComboBox;
    private boolean initialIntervalMode = false; // false = nächstmöglich, true = in Wochen
    private Integer initialWeeks = 4;
    private ComboBox<SideOfEye> sideOfEyeComboBox;
    private ComboBox<UserAccount> doctorComboBox;
    private ComboBox<MedicationFavourite> medicationComboBox;
    private ComboBox<SurgicalCenter> surgicalCenterComboBox;
    private com.vaadin.flow.component.textfield.TextArea additionalInfoTextArea;
    private List<SurgicalCenterTimeSlot> availableTimeSlots = new ArrayList<>();
    private SurgicalCenterTimeSlot selectedTimeSlot;
    
    // Wizard-Schritte
    private int currentStep = 1;
    private VerticalLayout stepContainer;
    private Button previousButton;
    private Button nextButton;
    private Button bookButton;
    
    // Schritt 3: Wochensicht
    private LocalDate currentWeekStart;
    private Div weekCalendarContainer;
    private Div legendContainer;
    private WeekNavigationSection weekNavigationSection;
    private Map<SurgicalCenter, String> surgicalCenterColors = new HashMap<>();
    private Map<SurgicalCenter, Boolean> surgicalCenterVisibility = new HashMap<>(); // Filter für Einrichtungen

    public NextTreatmentBookingDialog(TreatmentPlan treatmentPlan, SideOfEye sideOfEye,
            ApplicationContext context, TreatmentPlanPresenter presenter,
            Consumer<Treatment> onTreatmentCreated) {
        this.treatmentPlan = treatmentPlan;
        this.sideOfEye = sideOfEye;
        this.context = context;
        this.presenter = presenter;
        this.institutionRepository = context.getBean(InstitutionRepository.class);
        this.onTreatmentCreated = onTreatmentCreated;

        // InstitutionContext sofort setzen, damit alle Service-Aufrufe funktionieren
        ensureInstitutionContext();

        setHeaderTitle("Nächsten Termin buchen");
        setWidth("1400px");
        setMaxWidth("95vw");
        setHeight("90vh");
        setCloseOnOutsideClick(false);
        
        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        // Footer-Buttons zuerst erstellen, damit sie verfügbar sind
        createWizardFooter();
        
        // Content hinzufügen
        add(createWizardContent());
    }

    private VerticalLayout createWizardContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Container für die Schritte
        stepContainer = new VerticalLayout();
        stepContainer.setSizeFull();
        stepContainer.setPadding(false);
        stepContainer.setSpacing(false);
        content.add(stepContainer);
        content.expand(stepContainer);
        
        // Initialisiere ersten Schritt (Buttons sind bereits erstellt)
        showStep(1);
        
        return content;
    }
    
    /**
     * Erstellt die Schritt-Anzeige (1/3, 2/3, 3/3).
     */
    private HorizontalLayout createStepIndicator() {
        HorizontalLayout indicator = new HorizontalLayout();
        indicator.setWidthFull();
        indicator.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        indicator.setSpacing(true);
        indicator.setAlignItems(FlexComponent.Alignment.CENTER);
        
        for (int i = 1; i <= 3; i++) {
            Span stepNumber = new Span(String.valueOf(i));
            stepNumber.getStyle()
                .set("width", "30px")
                .set("height", "30px")
                .set("border-radius", "50%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-weight", "bold");
            
            if (i == currentStep) {
                stepNumber.getStyle()
                    .set("background-color", "var(--lumo-primary-color)")
                    .set("color", "var(--lumo-primary-contrast-color)");
            } else if (i < currentStep) {
                stepNumber.getStyle()
                    .set("background-color", "var(--lumo-success-color)")
                    .set("color", "var(--lumo-success-contrast-color)");
            } else {
                stepNumber.getStyle()
                    .set("background-color", "var(--lumo-contrast-20pct)")
                    .set("color", "var(--lumo-contrast-70pct)");
            }
            
            indicator.add(stepNumber);
        }
        
        return indicator;
    }
    
    /**
     * Erstellt den Footer mit Navigation-Buttons.
     */
    private void createWizardFooter() {
        previousButton = new Button("Zurück", e -> previousStep());
        previousButton.setEnabled(false);
        
        Button cancelButton = new Button("Abbrechen", e -> close());
        
        nextButton = new Button("Weiter", e -> nextStep());
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        bookButton = new Button("Behandlung buchen", e -> saveTreatment());
        bookButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        bookButton.setVisible(false);
        
        // Buttons zum Dialog-Footer hinzufügen (unten)
        getFooter().add(previousButton, cancelButton, nextButton, bookButton);
        
        // Initialisiere Navigation nach Button-Erstellung
        updateNavigationButtons();
    }
    
    /**
     * Zeigt den angegebenen Schritt an.
     */
    private void showStep(int step) {
        currentStep = step;
        stepContainer.removeAll();
        
        switch (step) {
            case 1 -> stepContainer.add(createStep1TreatmentDetails());
            case 2 -> stepContainer.add(createStep2AppointmentRules());
            case 3 -> stepContainer.add(createStep3WeekCalendar());
        }
        
        updateNavigationButtons();
    }
    
    /**
     * Aktualisiert die Navigation-Buttons basierend auf dem aktuellen Schritt.
     */
    private void updateNavigationButtons() {
        previousButton.setEnabled(currentStep > 1);
        nextButton.setVisible(currentStep < 3);
        bookButton.setVisible(currentStep == 3);
        
        if (currentStep == 3) {
            bookButton.setEnabled(selectedTimeSlot != null);
        }
    }
    
    /**
     * Geht zum nächsten Schritt.
     */
    private void nextStep() {
        if (currentStep == 1) {
            if (!validateStep1()) {
                return;
            }
            showStep(2);
        } else if (currentStep == 2) {
            if (!validateStep2()) {
                return;
            }
            // Lade Termine für Schritt 3
            loadTimeSlotsForWeekView();
            // Springe zur richtigen Woche basierend auf Terminfindungsregeln
            if (weekNavigationSection != null) {
                LocalDate targetWeek = calculateTargetWeekStart();
                weekNavigationSection.setWeekStart(targetWeek);
                currentWeekStart = targetWeek;
            }
            showStep(3);
        }
    }
    
    /**
     * Geht zum vorherigen Schritt.
     */
    private void previousStep() {
        if (currentStep > 1) {
            showStep(currentStep - 1);
        }
    }
    
    /**
     * Validiert Schritt 1: Behandlungsdetails.
     */
    private boolean validateStep1() {
        if (sideOfEyeComboBox.getValue() == null) {
            showError("Bitte wählen Sie eine Augenseite aus.");
            return false;
        }
        if (medicationComboBox.getValue() == null) {
            showError("Bitte wählen Sie ein Medikament aus.");
            return false;
        }
        return true;
    }
    
    /**
     * Validiert Schritt 2: Termin finden.
     */
    private boolean validateStep2() {
        if ("in Wochen".equals(intervalTypeGroup.getValue()) && weeksComboBox.getValue() == null) {
            showError("Bitte wählen Sie die Anzahl der Wochen aus.");
            return false;
        }
        return true;
    }
    
    /**
     * Schritt 1: Behandlungsdetails.
     */
    private VerticalLayout createStep1TreatmentDetails() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        com.vaadin.flow.component.html.Div treatmentSection = createSection("Behandlungsdetails");
        treatmentSection.setWidthFull();
        FormLayout treatmentLayout = new FormLayout();
        
        sideOfEyeComboBox = new ComboBox<>("Auge");
        sideOfEyeComboBox.setItems(SideOfEye.values());
        sideOfEyeComboBox.setValue(sideOfEye);
        sideOfEyeComboBox.setItemLabelGenerator(SideOfEye::toString);
        sideOfEyeComboBox.setRequired(true);
        sideOfEyeComboBox.setRequiredIndicatorVisible(true);
        treatmentLayout.add(sideOfEyeComboBox, 2);

        // InstitutionContext MUSS gesetzt sein, bevor Ärzte und Medikamente geladen werden
        ensureInstitutionContext();
        
        if (!InstitutionContext.hasInstitution()) {
            showError("Fehler: InstitutionContext konnte nicht gesetzt werden. Bitte versuchen Sie es erneut.");
            LOG.error("InstitutionContext konnte nicht gesetzt werden beim Laden der Behandlungsdetails");
            // Zeige leere Comboboxen mit Fehlermeldung
            doctorComboBox = new ComboBox<>("Behandelnder Arzt");
            doctorComboBox.setPlaceholder("Fehler: InstitutionContext nicht gesetzt");
            doctorComboBox.setEnabled(false);
            treatmentLayout.add(doctorComboBox, 2);
            
            medicationComboBox = new ComboBox<>("Medikament");
            medicationComboBox.setPlaceholder("Fehler: InstitutionContext nicht gesetzt");
            medicationComboBox.setEnabled(false);
            treatmentLayout.add(medicationComboBox, 2);
            return content;
        }
        
        UserAccountService userAccountService = context.getBean(UserAccountService.class);
        doctorComboBox = new ComboBox<>("Behandelnder Arzt");
        doctorComboBox.setItems(userAccountService.findUsersByRole(AppRoles.DOCTOR));
        doctorComboBox.setItemLabelGenerator(user -> 
            user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
        doctorComboBox.setPlaceholder("Arzt auswählen (optional)");
        treatmentLayout.add(doctorComboBox, 2);

        medicationComboBox = new ComboBox<>("Medikament");
        List<MedicationFavourite> medicationFavourites;
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            medicationFavourites = presenter.getDrugsForInstitution(treatmentPlan.getInstitution().getId());
        } else {
            // Fallback: Versuche über InstitutionContext
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId != null) {
                medicationFavourites = presenter.getDrugsForInstitution(institutionId);
            } else {
                medicationFavourites = presenter.getDrugs();
            }
        }
        medicationComboBox.setItems(medicationFavourites);
        medicationComboBox.setItemLabelGenerator(MedicationFavourite::getEffectiveDisplayName);
        medicationComboBox.setPlaceholder("Medikamentenfavorit auswählen");
        medicationComboBox.setRequired(true);
        medicationComboBox.setRequiredIndicatorVisible(true);
        medicationComboBox.setClearButtonVisible(true);
        
        MedicationFavourite lastMedication = getLastMedicationForSideOfEye();
        if (lastMedication != null && medicationFavourites.contains(lastMedication)) {
            medicationComboBox.setValue(lastMedication);
        }
        treatmentLayout.add(medicationComboBox, 2);
        
        treatmentSection.add(treatmentLayout);
        
        additionalInfoTextArea = new com.vaadin.flow.component.textfield.TextArea("Zusätzliche Informationen");
        additionalInfoTextArea.setPlaceholder("Zusätzliche Informationen zur Behandlung (optional)");
        additionalInfoTextArea.setWidthFull();
        additionalInfoTextArea.setHeight("200px");
        treatmentSection.add(additionalInfoTextArea);
        
        content.add(treatmentSection);
        content.expand(treatmentSection);
        
        return content;
    }
    
    /**
     * Schritt 2: Terminfindungsregeln (ohne Kalender).
     */
    private VerticalLayout createStep2AppointmentRules() {
        // InstitutionContext muss gesetzt sein, bevor SurgicalCenters geladen werden
        ensureInstitutionContext();
        
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        com.vaadin.flow.component.html.Div rulesSection = createSection("Termin finden");
        rulesSection.setWidthFull();
        
        VerticalLayout rulesContent = new VerticalLayout();
        rulesContent.setSpacing(true);
        rulesContent.setPadding(false);
        rulesContent.setWidthFull();
        
        FormLayout intervalLayout = new FormLayout();
        
        intervalTypeGroup = new RadioButtonGroup<>();
        intervalTypeGroup.setLabel("Intervall");
        List<String> intervalOptions = new ArrayList<>();
        intervalOptions.add("nächstmöglich");
        intervalOptions.add("in Wochen");
        
        intervalTypeGroup.setItems(intervalOptions);
        // Setze initialen Wert basierend auf initialIntervalMode
        if (initialIntervalMode) {
            intervalTypeGroup.setValue(intervalOptions.get(1)); // "in Wochen"
        } else {
            intervalTypeGroup.setValue(intervalOptions.get(0)); // "nächstmöglich"
        }
        intervalTypeGroup.addValueChangeListener(e -> updateIntervalFields());
        intervalLayout.add(intervalTypeGroup, 2);

        weeksComboBox = new ComboBox<>("Wochen");
        List<Integer> weeks = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            weeks.add(i);
        }
        weeksComboBox.setItems(weeks);
        weeksComboBox.setValue(initialWeeks != null ? initialWeeks : 4);
        weeksComboBox.setItemLabelGenerator(w -> w + " Wochen");
        weeksComboBox.setVisible(initialIntervalMode); // Sichtbar wenn initialIntervalMode true ist
        intervalLayout.add(weeksComboBox, 2);
        rulesContent.add(intervalLayout);
        
        rulesSection.add(rulesContent);
        content.add(rulesSection);
        content.expand(rulesSection);
        
        return content;
    }
    
    /**
     * Schritt 3: Wochensicht-Kalender.
     */
    private VerticalLayout createStep3WeekCalendar() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Bestimme Startwoche basierend auf Terminfindungsregeln
        LocalDate targetWeekStart = calculateTargetWeekStart();
        
        // VerticalLayout für Wochenliste und Legende untereinander
        VerticalLayout topLayout = new VerticalLayout();
        topLayout.setWidthFull();
        topLayout.setSpacing(true);
        topLayout.setPadding(false);
        
        // Woche-Navigation in Section mit minimaler Höhe
        weekNavigationSection = new WeekNavigationSection("Wochenliste", targetWeekStart, weekStart -> {
            currentWeekStart = weekStart;
            refreshWeekCalendar();
        });
        weekNavigationSection.getStyle().set("flex-shrink", "0");
        topLayout.add(weekNavigationSection);
        
        // Legende für operative Einrichtungen unter der Wochenliste
        legendContainer = new Div();
        legendContainer.setWidthFull();
        legendContainer.getStyle().set("flex-shrink", "0");
        topLayout.add(legendContainer);
        
        content.add(topLayout);
        content.setFlexGrow(0, topLayout); // Top-Layout soll nicht wachsen
        
        // Kalender-Container
        weekCalendarContainer = new Div();
        weekCalendarContainer.setWidthFull();
        weekCalendarContainer.getStyle()
            .set("min-height", "600px")
            .set("flex-grow", "1")
            .set("overflow", "hidden"); // Keine Scrollbar um den Container
        content.add(weekCalendarContainer);
        content.expand(weekCalendarContainer);
        
        // Initialisiere Woche
        currentWeekStart = targetWeekStart;
        weekNavigationSection.setWeekStart(targetWeekStart);
        
        // Lade TimeSlots und aktualisiere Kalender (inkl. Legende)
        loadTimeSlotsForWeekView();
        refreshWeekCalendar();
        
        return content;
    }
    
    /**
     * Berechnet die Zielwoche basierend auf den Terminfindungsregeln.
     */
    private LocalDate calculateTargetWeekStart() {
        if (availableTimeSlots.isEmpty()) {
            return LocalDate.now().with(DayOfWeek.MONDAY);
        }
        
        if ("nächstmöglich".equals(intervalTypeGroup.getValue())) {
            // Springe zur Woche mit dem ersten verfügbaren Termin
            LocalDate firstSlotDate = availableTimeSlots.stream()
                .map(SurgicalCenterTimeSlot::getDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
            return firstSlotDate.with(DayOfWeek.MONDAY);
        } else if ("in Wochen".equals(intervalTypeGroup.getValue()) && weeksComboBox.getValue() != null) {
            // Springe zur Woche basierend auf Wochenauswahl
            LocalDate targetDate = LocalDate.now().plusWeeks(weeksComboBox.getValue());
            // Finde den nächsten verfügbaren Termin in dieser Woche oder danach
            LocalDate weekStart = targetDate.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            // Prüfe ob es Termine in dieser Woche gibt
            boolean hasSlotsInWeek = availableTimeSlots.stream()
                .anyMatch(slot -> {
                    LocalDate slotDate = slot.getDate();
                    return slotDate != null && !slotDate.isBefore(weekStart) && !slotDate.isAfter(weekEnd);
                });
            
            if (hasSlotsInWeek) {
                return weekStart;
            } else {
                // Finde die nächste Woche mit Terminen
                LocalDate nextSlotDate = availableTimeSlots.stream()
                    .map(SurgicalCenterTimeSlot::getDate)
                    .filter(date -> date.isAfter(weekEnd) || date.isEqual(weekStart))
                    .min(LocalDate::compareTo)
                    .orElse(weekStart);
                return nextSlotDate.with(DayOfWeek.MONDAY);
            }
        }
        
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }
    
    
    /**
     * Prüft, ob es weitere TimeSlots nach der angegebenen Woche gibt.
     */
    private boolean hasMoreTimeSlots(LocalDate weekStart) {
        if (availableTimeSlots.isEmpty()) {
            return false;
        }
        
        LocalDate lastSlotDate = availableTimeSlots.stream()
            .map(SurgicalCenterTimeSlot::getDate)
            .max(LocalDate::compareTo)
            .orElse(null);
        
        return lastSlotDate != null && !weekStart.isAfter(lastSlotDate);
    }
    
    /**
     * Lädt die TimeSlots für die Wochensicht.
     * Lädt alle verfügbaren Zeitslots aller operativen Einrichtungen.
     * Die Filterung nach der angezeigten Woche erfolgt in refreshWeekCalendar().
     */
    private void loadTimeSlotsForWeekView() {
        // InstitutionContext MUSS vor jedem Service-Aufruf gesetzt sein
        // (Vaadin-Button-Clicks laufen in anderen Threads, daher geht ThreadLocal verloren)
        ensureInstitutionContext();
        
        // Lade alle Termine ab heute (keine Vorfilterung nach Intervall)
        // Die Vorauswahl "nächstmöglich" / "in Wochen" beeinflusst nur die anzuzeigende Woche,
        // nicht die geladenen Termine
        LocalDate startDate = LocalDate.now();
        TimePeriod period = TimePeriod.THREE_MONTHS;
        
        // Verwende WEEKLY, um alle Termine zu laden (nicht nur alle 4 Wochen)
        TimeSlotRepetition repetition = TimeSlotRepetition.WEEKLY;
        
        // Lade alle Zeitslots aller Einrichtungen (centerId = null)
        var availableSlots = presenter.getAllTimeSlotsFilteredBy(
                startDate, period, repetition, null);
        
        // Sortiere alle Termine nach Datum - keine weitere Filterung hier
        // Die Filterung nach der angezeigten Woche erfolgt in refreshWeekCalendar()
        availableTimeSlots = availableSlots.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(java.util.stream.Collectors.toList());
        
        // Initialisiere Farben für operative Einrichtungen
        initializeSurgicalCenterColors();
    }
    
    /**
     * Initialisiert Farben für operative Einrichtungen.
     */
    private void initializeSurgicalCenterColors() {
        surgicalCenterColors.clear();
        surgicalCenterVisibility.clear();
        List<SurgicalCenter> centers = availableTimeSlots.stream()
            .map(SurgicalCenterTimeSlot::getSurgicalCenter)
            .filter(center -> center != null)
            .distinct()
            .collect(Collectors.toList());
        
        String[] colors = {
            "var(--lumo-primary-color-10pct)",
            "var(--lumo-success-color-10pct)",
            "var(--lumo-warning-color-10pct)",
            "var(--lumo-error-color-10pct)",
            "#E3F2FD", // Hellblau
            "#FFF3E0", // Hellorange
            "#F3E5F5", // Helllila
            "#E8F5E9"  // Hellgrün
        };
        
        for (int i = 0; i < centers.size(); i++) {
            surgicalCenterColors.put(centers.get(i), colors[i % colors.length]);
            // Nur die erste Einrichtung ist vorausgewählt, alle anderen sind abgewählt
            surgicalCenterVisibility.put(centers.get(i), i == 0);
        }
    }
    
    /**
     * Erstellt eine Legende für die operativen Einrichtungen mit Checkboxen.
     */
    private Div createLegend() {
        if (availableTimeSlots.isEmpty()) {
            return null;
        }
        
        List<SurgicalCenter> centers = availableTimeSlots.stream()
            .map(SurgicalCenterTimeSlot::getSurgicalCenter)
            .filter(center -> center != null)
            .distinct()
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
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
        
        // Horizontal angeordnete Einrichtungen mit Zeilenumbruch
        Div legendContent = new Div();
        legendContent.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "var(--lumo-space-s)")
            .set("width", "100%");
        
        for (SurgicalCenter center : centers) {
            Checkbox checkbox = new Checkbox();
            checkbox.setLabel(center.getName());
            // Alle Checkboxen sind an- und abwählbar
            checkbox.setEnabled(true);
            Boolean currentVisibility = surgicalCenterVisibility.getOrDefault(center, false);
            checkbox.setValue(currentVisibility);
            
            // ValueChangeListener für Checkbox-Änderungen
            checkbox.addValueChangeListener(e -> {
                Boolean newValue = e.getValue();
                if (newValue != null) {
                    surgicalCenterVisibility.put(center, newValue);
                    // Aktualisiere nur den Kalender, nicht die gesamte Legende
                    refreshWeekCalendarContent();
                }
            });
            
            // Farbe als Indikator hinzufügen
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
        
        legend.add(legendContent);
        
        return legend;
    }
    
    /**
     * Aktualisiert die Wochensicht des Kalenders.
     */
    private void refreshWeekCalendar() {
        weekCalendarContainer.removeAll();
        
        // Lade Termine neu, wenn die aktuelle Woche außerhalb des geladenen Bereichs liegt
        // Erweitere den geladenen Bereich, wenn nötig
        if (currentWeekStart != null) {
            LocalDate weekEnd = currentWeekStart.plusDays(6);
            
            // Prüfe, ob die aktuelle Woche innerhalb des geladenen Bereichs liegt
            boolean needsReload = availableTimeSlots.isEmpty();
            if (!needsReload) {
                LocalDate minDate = availableTimeSlots.stream()
                    .map(SurgicalCenterTimeSlot::getDate)
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.MAX);
                LocalDate maxDate = availableTimeSlots.stream()
                    .map(SurgicalCenterTimeSlot::getDate)
                    .max(LocalDate::compareTo)
                    .orElse(LocalDate.MIN);
                
                // Wenn die aktuelle Woche außerhalb des geladenen Bereichs liegt, lade neu
                if (currentWeekStart.isBefore(minDate) || weekEnd.isAfter(maxDate)) {
                    needsReload = true;
                }
            }
            
            if (needsReload) {
                // Lade Termine für einen erweiterten Zeitraum (6 Monate vor und nach der aktuellen Woche)
                ensureInstitutionContext();
                LocalDate extendedStart = currentWeekStart.minusMonths(6);
                TimePeriod extendedPeriod = TimePeriod.SIX_MONTHS;
                // Verwende WEEKLY, um alle Termine zu laden (nicht nur alle 4 Wochen)
                TimeSlotRepetition repetition = TimeSlotRepetition.WEEKLY;
                
                var extendedSlots = presenter.getAllTimeSlotsFilteredBy(
                    extendedStart, extendedPeriod, repetition, null);
                
                // Füge neue Termine hinzu (ohne Duplikate)
                for (SurgicalCenterTimeSlot slot : extendedSlots) {
                    if (!availableTimeSlots.stream().anyMatch(existing -> 
                        existing.getId() != null && slot.getId() != null && 
                        existing.getId().equals(slot.getId()))) {
                        availableTimeSlots.add(slot);
                    }
                }
                
                // Sortiere erneut nach Datum
                availableTimeSlots = availableTimeSlots.stream()
                        .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                        .collect(java.util.stream.Collectors.toList());
                
                // Initialisiere Farben neu, falls neue Einrichtungen hinzugekommen sind
                initializeSurgicalCenterColors();
            }
        }
        
        // Aktualisiere Legende
        if (legendContainer != null) {
            legendContainer.removeAll();
            Div legend = createLegend();
            if (legend != null) {
                legendContainer.add(legend);
            }
        }
        
        refreshWeekCalendarContent();
    }
    
    /**
     * Aktualisiert nur den Kalender-Inhalt (ohne Legende neu zu erstellen).
     * Wird verwendet, wenn sich die Sichtbarkeit der Einrichtungen ändert.
     */
    private void refreshWeekCalendarContent() {
        weekCalendarContainer.removeAll();
        
        if (availableTimeSlots.isEmpty()) {
            Span noSlotsMessage = new Span("Keine Termine verfügbar im gewählten Zeitraum.");
            weekCalendarContainer.add(noSlotsMessage);
            return;
        }
        
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        
        H3 weekHeader = new H3("Woche " + currentWeekStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)) + 
                               " - " + weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)));
        weekCalendarContainer.add(weekHeader);
        
        // Gruppiere TimeSlots nach Datum und filtere nach sichtbaren Einrichtungen
        Map<LocalDate, List<SurgicalCenterTimeSlot>> slotsByDate = availableTimeSlots.stream()
            .filter(slot -> {
                LocalDate slotDate = slot.getDate();
                if (slotDate == null || slotDate.isBefore(currentWeekStart) || slotDate.isAfter(weekEnd)) {
                    return false;
                }
                // Filtere nach sichtbaren Einrichtungen
                SurgicalCenter center = slot.getSurgicalCenter();
                return center != null && surgicalCenterVisibility.getOrDefault(center, true);
            })
            .collect(Collectors.groupingBy(SurgicalCenterTimeSlot::getDate));
        
        // Erstelle Wochen-Grid
        HorizontalLayout weekGrid = createWeekGrid(currentWeekStart, slotsByDate);
        weekGrid.setWidthFull();
        weekGrid.setHeightFull();
        weekCalendarContainer.add(weekGrid);
        weekCalendarContainer.setHeightFull();
        
        // Synchronisiere Scrollen aller Tages-Spalten
        setupSynchronizedScrolling();
    }
    
    /**
     * Gibt den deutschen Namen eines Wochentags zurück.
     */
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
    
    /**
     * Erstellt das Wochen-Grid mit TimeSlots.
     */
    private HorizontalLayout createWeekGrid(LocalDate weekStart, Map<LocalDate, List<SurgicalCenterTimeSlot>> slotsByDate) {
        HorizontalLayout weekLayout = new HorizontalLayout();
        weekLayout.setWidthFull();
        weekLayout.setSpacing(false);
        weekLayout.setPadding(false);
        weekLayout.addClassNames(LumoUtility.Gap.SMALL);
        
        // Bestimme Zeitbereich aus allen TimeSlots
        LocalTime earliestTime = availableTimeSlots.stream()
            .map(SurgicalCenterTimeSlot::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(LocalTime.of(6, 0))
            .minusHours(1);
        LocalTime latestTime = availableTimeSlots.stream()
            .map(slot -> slot.getEndTime() != null ? slot.getEndTime() : slot.getStartTime().plusHours(1))
            .max(LocalTime::compareTo)
            .orElse(LocalTime.of(20, 0))
            .plusHours(1);
        
        // Erstelle Spalte für jeden Tag (immer alle 7 Tage, auch wenn Samstag/Sonntag leer sind)
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<SurgicalCenterTimeSlot> daySlots = slotsByDate.getOrDefault(day, new ArrayList<>());
            
            VerticalLayout dayColumn = createDayColumn(day, earliestTime, latestTime, daySlots);
            weekLayout.add(dayColumn);
            weekLayout.setFlexGrow(1, dayColumn); // Gleiche Breite für alle Tage
        }
        
        return weekLayout;
    }
    
    /**
     * Erstellt eine Tages-Spalte mit TimeSlots.
     */
    private VerticalLayout createDayColumn(LocalDate day, LocalTime earliestTime, LocalTime latestTime, 
                                          List<SurgicalCenterTimeSlot> daySlots) {
        VerticalLayout column = new VerticalLayout();
        column.setPadding(false);
        column.setSpacing(false);
        column.setWidth(null); // Wird durch FlexGrow gesteuert
        column.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.SMALL);
        column.getStyle()
            .set("min-width", "0") // Wichtig für Flexbox
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("height", "100%");
        
        // Tages-Header mit deutschem Wochentag
        String dayName = getGermanDayName(day.getDayOfWeek());
        H4 dayHeader = new H4(dayName + "\n" + day.format(DateTimeFormatter.ofPattern("dd.MM.")));
        dayHeader.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-s)")
            .set("margin", "0")
            .set("background-color", day.equals(LocalDate.now()) 
                ? "var(--lumo-primary-color-10pct)" 
                : "var(--lumo-contrast-5pct)")
            .set("flex-shrink", "0");
        column.add(dayHeader);
        
        // TimeSlots-Container mit relativer Positionierung und synchronem Scrollen
        Div timeSlotsContainer = new Div();
        timeSlotsContainer.setWidthFull();
        timeSlotsContainer.getStyle()
            .set("overflow-y", "auto")
            .set("position", "relative")
            .set("min-height", "400px")
            .set("flex-grow", "1")
            .set("flex-shrink", "1");
        
        // Setze eindeutige ID für synchrones Scrollen
        String containerId = "day-column-" + day.toString();
        timeSlotsContainer.setId(containerId);
        
        // Berechne Gesamtdauer in Minuten für Skalierung
        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, latestTime);
        if (totalMinutes == 0) {
            totalMinutes = 1; // Vermeide Division durch Null
        }
        
        // Zeit-Labels (jede Stunde) - mit mehr Abstand nach links, damit sie nicht mit Zeitslots überlappen
        LocalTime currentTime = earliestTime;
        while (currentTime.isBefore(latestTime)) {
            if (currentTime.getMinute() == 0) {
                Span timeLabel = new Span(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
                double topPercent = (double) java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, currentTime) / totalMinutes * 100;
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
        
        // Gruppiere Zeitslots nach Überschneidungen und erstelle Blöcke
        List<List<SurgicalCenterTimeSlot>> slotGroups = groupOverlappingSlots(daySlots);
        for (int groupIndex = 0; groupIndex < slotGroups.size(); groupIndex++) {
            List<SurgicalCenterTimeSlot> slotGroup = slotGroups.get(groupIndex);
            for (SurgicalCenterTimeSlot slot : slotGroup) {
                Div slotBlock = createTimeSlotBlock(slot, earliestTime, totalMinutes, slotGroups.size(), groupIndex);
                timeSlotsContainer.add(slotBlock);
            }
        }
        
        column.add(timeSlotsContainer);
        column.setFlexGrow(1, timeSlotsContainer);
        
        return column;
    }
    
    /**
     * Gruppiert überlappende Zeitslots, damit sie nebeneinander angeordnet werden können.
     */
    private List<List<SurgicalCenterTimeSlot>> groupOverlappingSlots(List<SurgicalCenterTimeSlot> slots) {
        if (slots.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Sortiere nach Startzeit
        List<SurgicalCenterTimeSlot> sorted = new ArrayList<>(slots);
        sorted.sort((a, b) -> {
            int timeCompare = a.getStartTime().compareTo(b.getStartTime());
            if (timeCompare != 0) {
                return timeCompare;
            }
            // Bei gleicher Startzeit: nach Endzeit sortieren
            LocalTime endA = a.getEndTime() != null ? a.getEndTime() : a.getStartTime().plusHours(1);
            LocalTime endB = b.getEndTime() != null ? b.getEndTime() : b.getStartTime().plusHours(1);
            return endA.compareTo(endB);
        });
        
        List<List<SurgicalCenterTimeSlot>> groups = new ArrayList<>();
        
        for (SurgicalCenterTimeSlot slot : sorted) {
            boolean added = false;
            for (List<SurgicalCenterTimeSlot> group : groups) {
                // Prüfe ob Slot mit einem Slot in der Gruppe überlappt
                boolean overlaps = false;
                LocalTime slotStart = slot.getStartTime();
                LocalTime slotEnd = slot.getEndTime() != null ? slot.getEndTime() : slotStart.plusHours(1);
                
                for (SurgicalCenterTimeSlot groupSlot : group) {
                    LocalTime groupStart = groupSlot.getStartTime();
                    LocalTime groupEnd = groupSlot.getEndTime() != null ? groupSlot.getEndTime() : groupStart.plusHours(1);
                    
                    // Überlappung: Start oder Ende liegt innerhalb des anderen Slots
                    if ((slotStart.isBefore(groupEnd) && slotEnd.isAfter(groupStart)) ||
                        (groupStart.isBefore(slotEnd) && groupEnd.isAfter(slotStart))) {
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
                // Neue Gruppe erstellen
                List<SurgicalCenterTimeSlot> newGroup = new ArrayList<>();
                newGroup.add(slot);
                groups.add(newGroup);
            }
        }
        
        return groups;
    }
    
    /**
     * Erstellt einen zusammenhängenden TimeSlot-Block.
     * @param totalGroups Anzahl der Gruppen (für Breitenberechnung)
     * @param groupIndex Index der Gruppe (für Positionierung)
     */
    private Div createTimeSlotBlock(SurgicalCenterTimeSlot timeSlot, LocalTime earliestTime, long totalMinutes, 
                                    int totalGroups, int groupIndex) {
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime() != null ? timeSlot.getEndTime() : startTime.plusHours(1);
        
        // Berechne Position und Höhe in Prozent
        long startMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, startTime);
        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
        
        double topPercent = (double) startMinutes / totalMinutes * 100;
        double heightPercent = (double) durationMinutes / totalMinutes * 100;
        
        // Berechne Breite und Position für nebeneinander angeordnete Slots
        double leftPercent = 50.0; // Start bei 50% (nach Zeit-Labels)
        double widthPercent = 50.0; // Standardbreite
        
        if (totalGroups > 1) {
            // Wenn mehrere Gruppen: teile den verfügbaren Platz auf
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
        
        // Erstelle Label für den TimeSlot
        Span slotLabel = createTimeSlotLabel(timeSlot);
        slotBlock.add(slotLabel);
        
        return slotBlock;
    }
    
    /**
     * Erstellt ein Label für einen TimeSlot mit Einrichtungsname.
     */
    private Span createTimeSlotLabel(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null) {
            return new Span("Ungültiger Termin");
        }
        int patientCount = timeSlot.getPatientCount();
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime() != null ? timeSlot.getEndTime() : startTime.plusHours(1);
        if (startTime == null) {
            return new Span("Ungültiger Termin");
        }
        
        // Zeige Einrichtungsname statt Zeit
        String centerName = timeSlot.getSurgicalCenter() != null 
            ? timeSlot.getSurgicalCenter().getName() 
            : "Unbekannt";
        
        // Berechne Patienten pro Stunde
        long durationHours = java.time.temporal.ChronoUnit.HOURS.between(startTime, endTime);
        if (durationHours == 0) {
            durationHours = 1; // Mindestens 1 Stunde
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
        
        // Bestimme Farbe basierend auf operativer Einrichtung (aus Legende)
        String backgroundColor;
        String textColor;
        if (isSelected) {
            backgroundColor = "var(--lumo-primary-color)";
            textColor = "var(--lumo-primary-contrast-color)";
        } else {
            // Verwende Farbe aus Legende für die Einrichtung
            SurgicalCenter center = timeSlot.getSurgicalCenter();
            backgroundColor = surgicalCenterColors.getOrDefault(center, "var(--lumo-contrast-20pct)");
            
            // Textfarbe basierend auf Patienten pro Stunde
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
            .set("border", isSelected ? "2px solid var(--lumo-primary-color)" : "1px solid var(--lumo-contrast-20pct)")
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
            selectedTimeSlot = timeSlot;
            bookButton.setEnabled(true);
            refreshWeekCalendar(); // Aktualisiere um Auswahl zu markieren
        });
        
        return label;
    }
    
    
    /**
     * Richtet synchrones Scrollen für alle Tages-Spalten ein.
     */
    private void setupSynchronizedScrolling() {
        // Warte kurz, damit die DOM-Elemente verfügbar sind
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs(
                "setTimeout(() => {" +
                "  const containers = document.querySelectorAll('[id^=\"day-column-\"]');" +
                "  if (containers.length === 0) return;" +
                "  " +
                "  let isScrolling = false;" +
                "  " +
                "  containers.forEach(container => {" +
                "    container.addEventListener('scroll', function(e) {" +
                "      if (isScrolling) return;" +
                "      isScrolling = true;" +
                "      " +
                "      const scrollTop = e.target.scrollTop;" +
                "      containers.forEach(other => {" +
                "        if (other !== e.target) {" +
                "          other.scrollTop = scrollTop;" +
                "        }" +
                "      });" +
                "      " +
                "      setTimeout(() => { isScrolling = false; }, 10);" +
                "    });" +
                "  });" +
                "}, 100);"
            );
        });
    }
    
    /**
     * Erstellt eine optisch getrennte Section mit Titel.
     */
    private com.vaadin.flow.component.html.Div createSection(String title) {
        com.vaadin.flow.component.html.Div section = new com.vaadin.flow.component.html.Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        section.getStyle().set("padding", "var(--lumo-space-m)");
        section.getStyle().set("box-sizing", "border-box");
        
        // Titel-Layout mit Schritt-Anzeige
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setWidthFull();
        titleLayout.setSpacing(true);
        titleLayout.setPadding(false);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        
        com.vaadin.flow.component.html.H4 sectionTitle = new com.vaadin.flow.component.html.H4(title);
        sectionTitle.getStyle().set("margin", "0");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
        sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        sectionTitle.getStyle().set("font-weight", "600");
        titleLayout.add(sectionTitle);
        titleLayout.setFlexGrow(1, sectionTitle);
        
        // Schritt-Anzeige rechts neben dem Titel
        HorizontalLayout stepIndicator = createStepIndicator();
        stepIndicator.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        titleLayout.add(stepIndicator);
        
        section.add(titleLayout);
        
        return section;
    }


    /**
     * Aktualisiert die Sichtbarkeit der Wochen-Auswahl basierend auf dem Intervall.
     */
    private void updateIntervalFields() {
        boolean isWeeks = "in Wochen".equals(intervalTypeGroup.getValue());
        weeksComboBox.setVisible(isWeeks);
        weeksComboBox.setRequired(isWeeks);
        if (isWeeks && weeksComboBox.getValue() == null) {
            weeksComboBox.setValue(4); // Standardwert wenn sichtbar
        }
    }

    private boolean hasMissedTreatment() {
        if (treatmentPlan == null || treatmentPlan.getId() == null) {
            return false;
        }

        // Prüfe, ob es einen vergangenen Termin ohne approvalDate gibt
        List<Treatment> treatments = presenter.getTreatmentDtos(sideOfEye, treatmentPlan.getId());
        LocalDate now = LocalDate.now();
        
        return treatments.stream()
                .anyMatch(t -> {
                    LocalDate treatmentDate = t.getDate();
                    return treatmentDate != null 
                            && treatmentDate.isBefore(now)
                            && t.getApprovalDate() == null;
                });
    }

    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     * Verwendet die Institution aus dem TreatmentPlan, falls der Context nicht gesetzt ist.
     * WICHTIG: Muss vor jedem Service-Aufruf aufgerufen werden, da Vaadin-Button-Clicks
     * in anderen Threads laufen und der ThreadLocal-Context verloren geht.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return; // Bereits gesetzt
        }
        
        // Versuche, Context aus TreatmentPlan zu setzen
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            InstitutionContext.setInstitutionId(treatmentPlan.getInstitution().getId());
            LOG.debug("InstitutionContext gesetzt aus TreatmentPlan: {}", treatmentPlan.getInstitution().getId());
        } else {
            // Fallback: Versuche aus Authentication zu setzen (wie in anderen Dialogen)
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof de.bbajor.pvs.institution.security.InstitutionAuthenticationToken institutionAuth) {
                if (institutionAuth.getInstitutionId() != null) {
                    InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                    LOG.debug("InstitutionContext gesetzt aus InstitutionAuthenticationToken: {}", institutionAuth.getInstitutionId());
                }
            } else if (authentication != null && authentication.getPrincipal() instanceof de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter adapter) {
                // Authentication wurde aus Session deserialisiert
                try {
                    String username = adapter.getUsername();
                    de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository = 
                        context.getBean(de.bbajor.pvs.security.domain.UserAccountRepository.class);
                    de.bbajor.pvs.security.domain.UserAccount userAccount = 
                        userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                        LOG.debug("InstitutionContext wiederhergestellt aus UserAccount.institution: {}", institutionId);
                    } else {
                        LOG.warn("UserAccount hat keine Institution - InstitutionContext konnte nicht gesetzt werden");
                    }
                } catch (Exception e) {
                    LOG.warn("Fehler beim Wiederherstellen des InstitutionContext aus UserAccount: {}", e.getMessage());
                }
            } else {
                LOG.warn("InstitutionContext konnte nicht gesetzt werden - TreatmentPlan hat keine Institution und keine gültige Authentication gefunden!");
            }
        }
    }
    
    /**
     * Ermittelt das Medikament aus der letzten Behandlung für das entsprechende Auge.
     * @return Das MedicationFavourite der letzten Behandlung oder null, falls keine vorhanden ist
     */
    private MedicationFavourite getLastMedicationForSideOfEye() {
        if (treatmentPlan == null || treatmentPlan.getId() == null) {
            return null;
        }

        List<Treatment> treatments = presenter.getTreatmentDtos(sideOfEye, treatmentPlan.getId());
        if (treatments.isEmpty()) {
            return null;
        }

        // Sortiere nach Datum (neueste zuerst) und nimm die erste Behandlung
        return treatments.stream()
                .filter(t -> t.getDate() != null && t.getMedicationFavourite() != null)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // Neueste zuerst
                .map(Treatment::getMedicationFavourite)
                .findFirst()
                .orElse(null);
    }

    private void saveTreatment() {
        if (sideOfEyeComboBox.getValue() == null) {
            showError("Auge muss ausgewählt sein.");
            return;
        }

        if (medicationComboBox.getValue() == null) {
            showError("Bitte wählen Sie ein Medikament aus.");
            return;
        }

        if (selectedTimeSlot == null) {
            showError("Bitte wählen Sie einen Termin aus.");
            return;
        }
        
        // Prüfe auf Zeitsperre zwischen beiden Augen (IVOM-Planer)
        try {
            ensureInstitutionContext();
            if (InstitutionContext.hasInstitution()) {
                Long institutionId = InstitutionContext.getInstitutionId();
                Institution institution = institutionRepository.findById(institutionId).orElse(null);
                
                if (institution != null && institution.getIvomEyeTreatmentLockoutDays() != null 
                        && institution.getIvomEyeTreatmentLockoutDays() > 0) {
                    int lockoutDays = institution.getIvomEyeTreatmentLockoutDays();
                    SideOfEye currentEye = sideOfEyeComboBox.getValue();
                    SideOfEye otherEye = (currentEye == SideOfEye.LEFT) ? SideOfEye.RIGHT : SideOfEye.LEFT;
                    
                    // Hole alle Treatments für das andere Auge
                    List<Treatment> otherEyeTreatments = presenter.getTreatmentDtos(otherEye, treatmentPlan.getId());
                    
                    // Prüfe, ob der ausgewählte Termin innerhalb des Sperrzeitraums liegt
                    LocalDate selectedDate = selectedTimeSlot.getDate();
                    if (selectedDate != null) {
                        for (Treatment otherTreatment : otherEyeTreatments) {
                            if (otherTreatment.getDate() != null) {
                                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                                    otherTreatment.getDate(), selectedDate);
                                
                                // Prüfe, ob der ausgewählte Termin innerhalb des Sperrzeitraums liegt
                                if (Math.abs(daysBetween) <= lockoutDays) {
                                    String eyeLabel = otherEye == SideOfEye.LEFT ? "linken" : "rechten";
                                    String dateStr = otherTreatment.getDate().format(
                                        DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                                    showError(String.format(
                                        "Eine Buchung ist nicht möglich: Das %s Auge wird bereits am %s behandelt. " +
                                        "Die Zeitsperre zwischen beiden Augen beträgt %d Tag%s.",
                                        eyeLabel, dateStr, lockoutDays, lockoutDays == 1 ? "" : "e"));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Fehler bei der Prüfung der Zeitsperre zwischen beiden Augen: {}", e.getMessage(), e);
            // Fehler nicht blockierend - Buchung kann trotzdem fortgesetzt werden
        }
        
        // Prüfe auf Überbuchung (> 40 Patienten pro Stunde)
        LocalTime startTime = selectedTimeSlot.getStartTime();
        LocalTime endTime = selectedTimeSlot.getEndTime() != null 
            ? selectedTimeSlot.getEndTime() 
            : startTime.plusHours(1);
        long durationHours = java.time.temporal.ChronoUnit.HOURS.between(startTime, endTime);
        if (durationHours == 0) {
            durationHours = 1;
        }
        double patientsPerHour = (double) selectedTimeSlot.getPatientCount() / durationHours;
        
        if (patientsPerHour >= 40) {
            // Zeige Bestätigungsdialog
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Termin stark überbucht");
            
            VerticalLayout content = new VerticalLayout();
            content.setSpacing(true);
            content.setPadding(true);
            
            Span message = new Span(
                "Dieser Zeitslot ist bereits stark überbucht (" + 
                selectedTimeSlot.getPatientCount() + " Patienten in " + durationHours + 
                " Stunde" + (durationHours > 1 ? "n" : "") + " = " + 
                String.format("%.1f", patientsPerHour) + " Patienten/Stunde).\n\n" +
                "Möchten Sie diesen Termin trotzdem buchen?"
            );
            message.getStyle().set("white-space", "pre-line");
            content.add(message);
            
            confirmDialog.add(content);
            
            Button cancelButton = new Button("Abbrechen", e -> confirmDialog.close());
            Button confirmButton = new Button("Trotzdem buchen", e -> {
                confirmDialog.close();
                performSaveTreatment();
            });
            confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
            
            confirmDialog.getFooter().add(cancelButton, confirmButton);
            confirmDialog.open();
        } else {
            // Normale Buchung ohne Bestätigung
            performSaveTreatment();
        }
    }
    
    /**
     * Führt die tatsächliche Behandlungsspeicherung durch.
     */
    private void performSaveTreatment() {
        try {
            // InstitutionContext MUSS vor jedem Service-Aufruf gesetzt sein
            // (Vaadin-Button-Clicks laufen in anderen Threads, daher geht ThreadLocal verloren)
            ensureInstitutionContext();
            
            if (!InstitutionContext.hasInstitution()) {
                showError("Fehler: InstitutionContext konnte nicht gesetzt werden. Bitte versuchen Sie es erneut.");
                LOG.error("InstitutionContext konnte nicht gesetzt werden beim Speichern der Behandlung");
                return;
            }
            
            // Validierung
            if (selectedTimeSlot == null || selectedTimeSlot.getId() == null) {
                showError("Fehler: Kein gültiger Termin ausgewählt.");
                LOG.error("selectedTimeSlot ist null oder hat keine ID");
                return;
            }
            
            if (treatmentPlan == null || treatmentPlan.getId() == null) {
                showError("Fehler: Kein gültiger Behandlungsplan vorhanden.");
                LOG.error("treatmentPlan ist null oder hat keine ID");
                return;
            }
            
            if (medicationComboBox.getValue() == null || medicationComboBox.getValue().getId() == null) {
                showError("Fehler: Kein gültiges Medikament ausgewählt.");
                LOG.error("medicationComboBox.getValue() ist null oder hat keine ID");
                return;
            }
            
            LOG.debug("Speichere Behandlung: TreatmentPlanId={}, TimeSlotId={}, MedicationId={}, SideOfEye={}", 
                treatmentPlan.getId(), selectedTimeSlot.getId(), 
                medicationComboBox.getValue().getId(), sideOfEyeComboBox.getValue());
            
            Treatment newTreatment = new Treatment();
            newTreatment.setTreatmentPlan(treatmentPlan);
            newTreatment.setSideOfEye(sideOfEyeComboBox.getValue());
            newTreatment.setSurgicalCenterTimeSlot(selectedTimeSlot);
            newTreatment.setMedicationFavourite(medicationComboBox.getValue());

            // Arzt zuweisen, falls ausgewählt
            if (doctorComboBox.getValue() != null) {
                newTreatment.setTreatingDoctors(Set.of(doctorComboBox.getValue()));
            }
            
            // Zusätzliche Informationen setzen, falls vorhanden
            if (additionalInfoTextArea.getValue() != null && !additionalInfoTextArea.getValue().trim().isEmpty()) {
                newTreatment.setAdditionalInfo(additionalInfoTextArea.getValue().trim());
            }

            // Behandlung speichern
            List<Treatment> treatmentsToSave = List.of(newTreatment);
            TreatmentPlan savedPlan = presenter.save(treatmentPlan.getId(), treatmentsToSave);
            
            LOG.debug("Behandlung erfolgreich gespeichert. TreatmentPlanId={}", savedPlan.getId());

            showSuccess("Termin erfolgreich gebucht.");
            if (onTreatmentCreated != null) {
                // Hole das gespeicherte Treatment aus dem zurückgegebenen Plan
                Treatment savedTreatment = savedPlan.getTreatments().stream()
                    .filter(t -> t.getSurgicalCenterTimeSlot() != null 
                        && t.getSurgicalCenterTimeSlot().getId() != null
                        && t.getSurgicalCenterTimeSlot().getId().equals(selectedTimeSlot.getId())
                        && t.getSideOfEye() == sideOfEyeComboBox.getValue())
                    .findFirst()
                    .orElse(newTreatment);
                onTreatmentCreated.accept(savedTreatment);
            }
            close();
        } catch (Exception e) {
            LOG.error("Fehler beim Buchen des Termins", e);
            showError("Fehler beim Buchen des Termins: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    
    /**
     * Setzt den initialen Intervall-Modus.
     * @param useInterval true = "in Wochen", false = "nächstmöglich"
     */
    public void setInitialIntervalMode(boolean useInterval) {
        this.initialIntervalMode = useInterval;
    }
    
    /**
     * Setzt die initiale Anzahl der Wochen.
     * @param weeks Anzahl der Wochen (1-16)
     */
    public void setInitialWeeks(int weeks) {
        this.initialWeeks = Math.max(1, Math.min(weeks, 16));
    }
}

