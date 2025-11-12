package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.institution.context.InstitutionContext;
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

    private final TreatmentPlan treatmentPlan;
    private final SideOfEye sideOfEye;
    private final ApplicationContext context;
    private final TreatmentPlanPresenter presenter;
    private final Consumer<Treatment> onTreatmentCreated;

    private RadioButtonGroup<String> intervalTypeGroup;
    private ComboBox<Integer> weeksComboBox;
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
    private HorizontalLayout stepIndicatorContainer;
    private Button previousButton;
    private Button nextButton;
    private Button bookButton;
    
    // Schritt 3: Wochensicht
    private LocalDate currentWeekStart;
    private Div weekCalendarContainer;

    public NextTreatmentBookingDialog(TreatmentPlan treatmentPlan, SideOfEye sideOfEye,
            ApplicationContext context, TreatmentPlanPresenter presenter,
            Consumer<Treatment> onTreatmentCreated) {
        this.treatmentPlan = treatmentPlan;
        this.sideOfEye = sideOfEye;
        this.context = context;
        this.presenter = presenter;
        this.onTreatmentCreated = onTreatmentCreated;

        setHeaderTitle("Nächsten Termin buchen");
        setWidth("1400px");
        setMaxWidth("95vw");
        setHeight("90vh");

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
        
        // Schritt-Anzeige-Container (wird in showStep aktualisiert)
        stepIndicatorContainer = new HorizontalLayout();
        stepIndicatorContainer.setWidthFull();
        stepIndicatorContainer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        stepIndicatorContainer.setSpacing(true);
        content.add(stepIndicatorContainer);
        
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
        
        // Aktualisiere Schritt-Anzeige
        stepIndicatorContainer.removeAll();
        stepIndicatorContainer.add(createStepIndicator());
        
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
     * Validiert Schritt 2: Terminfindungsregeln.
     */
    private boolean validateStep2() {
        if (surgicalCenterComboBox.getValue() == null) {
            showError("Bitte wählen Sie einen Behandlungsort aus.");
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

        UserAccountService userAccountService = context.getBean(UserAccountService.class);
        doctorComboBox = new ComboBox<>("Behandelnder Arzt");
        doctorComboBox.setItems(userAccountService.findUsersByRole(AppRoles.DOCTOR));
        doctorComboBox.setItemLabelGenerator(user -> 
            user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
        doctorComboBox.setPlaceholder("Arzt auswählen (optional)");
        treatmentLayout.add(doctorComboBox, 2);

        medicationComboBox = new ComboBox<>("Medikament");
        ensureInstitutionContext();
        List<MedicationFavourite> medicationFavourites;
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            medicationFavourites = presenter.getDrugsForInstitution(treatmentPlan.getInstitution().getId());
        } else {
            medicationFavourites = presenter.getDrugs();
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
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        com.vaadin.flow.component.html.Div rulesSection = createSection("Terminfindungsregeln");
        rulesSection.setWidthFull();
        
        VerticalLayout rulesContent = new VerticalLayout();
        rulesContent.setSpacing(true);
        rulesContent.setPadding(false);
        rulesContent.setWidthFull();
        
        FormLayout intervalLayout = new FormLayout();
        
        intervalTypeGroup = new RadioButtonGroup<>();
        intervalTypeGroup.setLabel("Intervall");
        List<String> intervalOptions = new ArrayList<>();
        intervalOptions.add("in Wochen");
        
        boolean showNextPossible = hasMissedTreatment();
        if (showNextPossible) {
            intervalOptions.add(0, "nächstmöglich");
        }
        
        intervalTypeGroup.setItems(intervalOptions);
        intervalTypeGroup.setValue(intervalOptions.get(0));
        intervalTypeGroup.addValueChangeListener(e -> updateIntervalFields());
        intervalLayout.add(intervalTypeGroup, 2);

        weeksComboBox = new ComboBox<>("Wochen");
        List<Integer> weeks = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            weeks.add(i);
        }
        weeksComboBox.setItems(weeks);
        weeksComboBox.setValue(4);
        weeksComboBox.setItemLabelGenerator(w -> w + " Wochen");
        weeksComboBox.setVisible(!"nächstmöglich".equals(intervalOptions.get(0)));
        intervalLayout.add(weeksComboBox, 2);
        rulesContent.add(intervalLayout);
        
        surgicalCenterComboBox = new ComboBox<>("Behandlungsort");
        ensureInstitutionContext();
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            surgicalCenterComboBox.setItems(presenter.getSurgicalCentersForInstitution(treatmentPlan.getInstitution().getId()));
        } else {
            surgicalCenterComboBox.setItems(presenter.getSurgicalCenters());
        }
        surgicalCenterComboBox.setItemLabelGenerator(SurgicalCenter::getName);
        surgicalCenterComboBox.setClearButtonVisible(true);
        surgicalCenterComboBox.setPlaceholder("Behandlungsort auswählen");
        surgicalCenterComboBox.setRequired(true);
        surgicalCenterComboBox.setRequiredIndicatorVisible(true);
        surgicalCenterComboBox.setWidthFull();
        rulesContent.add(surgicalCenterComboBox);
        
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
        
        // Woche-Navigation
        HorizontalLayout weekNavigation = createWeekNavigation();
        content.add(weekNavigation);
        
        // Kalender-Container
        weekCalendarContainer = new Div();
        weekCalendarContainer.setWidthFull();
        weekCalendarContainer.getStyle().set("min-height", "500px");
        content.add(weekCalendarContainer);
        content.expand(weekCalendarContainer);
        
        // Initialisiere Woche
        currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        refreshWeekCalendar();
        
        return content;
    }
    
    /**
     * Erstellt die Woche-Navigation.
     */
    private HorizontalLayout createWeekNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setWidthFull();
        navigation.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        navigation.setAlignItems(HorizontalLayout.Alignment.CENTER);
        navigation.setSpacing(true);
        
        Button previousWeek = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            LocalDate newWeek = currentWeekStart.minusWeeks(1);
            if (newWeek.isBefore(LocalDate.now().with(DayOfWeek.MONDAY))) {
                showError("Ein Sprung in die Vergangenheit ist nicht möglich.");
                return;
            }
            currentWeekStart = newWeek;
            refreshWeekCalendar();
        });
        previousWeek.setTooltipText("Vorherige Woche");
        
        Button thisWeek = new Button("Diese Woche", e -> {
            currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            refreshWeekCalendar();
        });
        
        Button nextWeek = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            LocalDate newWeek = currentWeekStart.plusWeeks(1);
            if (!hasMoreTimeSlots(newWeek)) {
                showError("Keine weiteren Termine in der operativen Einrichtung eingeplant. Wenn Sie weitere Termine benötigen, dann planen Sie diese bitte über das Menü \"Operationszentren\" ein.");
                return;
            }
            currentWeekStart = newWeek;
            refreshWeekCalendar();
        });
        nextWeek.setTooltipText("Nächste Woche");
        
        DatePicker weekPicker = new DatePicker("Woche auswählen");
        weekPicker.setValue(currentWeekStart);
        weekPicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                LocalDate selectedDate = event.getValue();
                LocalDate weekStart = selectedDate.with(DayOfWeek.MONDAY);
                if (weekStart.isBefore(LocalDate.now().with(DayOfWeek.MONDAY))) {
                    showError("Ein Sprung in die Vergangenheit ist nicht möglich.");
                    weekPicker.setValue(currentWeekStart);
                    return;
                }
                if (!hasMoreTimeSlots(weekStart)) {
                    showError("Keine weiteren Termine in der operativen Einrichtung eingeplant. Wenn Sie weitere Termine benötigen, dann planen Sie diese bitte über das Menü \"Operationszentren\" ein.");
                    weekPicker.setValue(currentWeekStart);
                    return;
                }
                currentWeekStart = weekStart;
                refreshWeekCalendar();
            }
        });
        
        navigation.add(previousWeek, thisWeek, nextWeek, weekPicker);
        
        return navigation;
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
     */
    private void loadTimeSlotsForWeekView() {
        if (surgicalCenterComboBox.getValue() == null) {
            availableTimeSlots = new ArrayList<>();
            return;
        }

        // Bestimme Startdatum basierend auf Intervall
        LocalDate startDate = LocalDate.now();
        if ("in Wochen".equals(intervalTypeGroup.getValue()) && weeksComboBox.getValue() != null) {
            startDate = LocalDate.now().plusWeeks(weeksComboBox.getValue());
        }

        TimePeriod period = TimePeriod.THREE_MONTHS;
        TimeSlotRepetition repetition = TimeSlotRepetition.EVERY_FOUR_WEEKS;
        
        Integer centerId = surgicalCenterComboBox.getValue().getId();
        ensureInstitutionContext();
        
        var availableSlots = presenter.getAllTimeSlotsFilteredBy(
                startDate, period, repetition, centerId);
        
        availableTimeSlots = new ArrayList<>(availableSlots);
        
        // Für "nächstmöglich": Sortiere nach Datum und begrenze auf 20 Termine
        if (!"in Wochen".equals(intervalTypeGroup.getValue())) {
            availableTimeSlots = availableSlots.stream()
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .limit(20)
                    .toList();
        } else {
            // Für "in Wochen": Filtere nach Wochen-Toleranz
            if (weeksComboBox.getValue() != null) {
                int weeks = weeksComboBox.getValue();
                List<SurgicalCenterTimeSlot> filteredSlots = new ArrayList<>();
                for (SurgicalCenterTimeSlot slot : availableSlots) {
                    long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(LocalDate.now(), slot.getDate());
                    if (Math.abs(weeksBetween - weeks) <= 1) {
                        filteredSlots.add(slot);
                    }
                }
                availableTimeSlots = filteredSlots;
            }
        }
    }
    
    /**
     * Aktualisiert die Wochensicht des Kalenders.
     */
    private void refreshWeekCalendar() {
        weekCalendarContainer.removeAll();
        
        if (availableTimeSlots.isEmpty()) {
            Span noSlotsMessage = new Span("Keine Termine verfügbar für den ausgewählten Behandlungsort im gewählten Zeitraum.");
            weekCalendarContainer.add(noSlotsMessage);
            return;
        }
        
        LocalDate weekEnd = currentWeekStart.plusDays(6);
        
        H3 weekHeader = new H3("Woche " + currentWeekStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                               " - " + weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        weekCalendarContainer.add(weekHeader);
        
        // Gruppiere TimeSlots nach Datum
        Map<LocalDate, List<SurgicalCenterTimeSlot>> slotsByDate = availableTimeSlots.stream()
            .filter(slot -> {
                LocalDate slotDate = slot.getDate();
                return slotDate != null && 
                       !slotDate.isBefore(currentWeekStart) && 
                       !slotDate.isAfter(weekEnd);
            })
            .collect(Collectors.groupingBy(SurgicalCenterTimeSlot::getDate));
        
        // Erstelle Wochen-Grid
        HorizontalLayout weekGrid = createWeekGrid(currentWeekStart, slotsByDate);
        weekCalendarContainer.add(weekGrid);
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
        
        // Erstelle Spalte für jeden Tag (Montag bis Sonntag)
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            VerticalLayout dayColumn = createDayColumn(day, earliestTime, latestTime, 
                slotsByDate.getOrDefault(day, new ArrayList<>()));
            weekLayout.add(dayColumn);
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
        column.setWidth("14%");
        column.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.SMALL);
        column.getStyle().set("min-width", "150px");
        
        // Tages-Header
        H4 dayHeader = new H4(day.getDayOfWeek().toString() + "\n" + day.format(DateTimeFormatter.ofPattern("dd.MM.")));
        dayHeader.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-s)")
            .set("margin", "0")
            .set("background-color", day.equals(LocalDate.now()) 
                ? "var(--lumo-primary-color-10pct)" 
                : "var(--lumo-contrast-5pct)");
        column.add(dayHeader);
        
        // TimeSlots-Container mit relativer Positionierung
        Div timeSlotsContainer = new Div();
        timeSlotsContainer.setWidthFull();
        timeSlotsContainer.getStyle()
            .set("overflow-y", "auto")
            .set("max-height", "600px")
            .set("position", "relative")
            .set("min-height", "400px");
        
        // Berechne Gesamtdauer in Minuten für Skalierung
        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, latestTime);
        if (totalMinutes == 0) {
            totalMinutes = 1; // Vermeide Division durch Null
        }
        
        // Zeit-Labels (jede Stunde)
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
                    .set("left", "4px")
                    .set("z-index", "1");
                timeSlotsContainer.add(timeLabel);
            }
            currentTime = currentTime.plusMinutes(15);
        }
        
        // Erstelle TimeSlot-Blöcke als zusammenhängende Elemente
        for (SurgicalCenterTimeSlot slot : daySlots) {
            Div slotBlock = createTimeSlotBlock(slot, earliestTime, totalMinutes);
            timeSlotsContainer.add(slotBlock);
        }
        
        column.add(timeSlotsContainer);
        column.setFlexGrow(1, timeSlotsContainer);
        
        return column;
    }
    
    /**
     * Erstellt einen zusammenhängenden TimeSlot-Block.
     */
    private Div createTimeSlotBlock(SurgicalCenterTimeSlot timeSlot, LocalTime earliestTime, long totalMinutes) {
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime() != null ? timeSlot.getEndTime() : startTime.plusHours(1);
        
        // Berechne Position und Höhe in Prozent
        long startMinutes = java.time.temporal.ChronoUnit.MINUTES.between(earliestTime, startTime);
        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(startTime, endTime);
        
        double topPercent = (double) startMinutes / totalMinutes * 100;
        double heightPercent = (double) durationMinutes / totalMinutes * 100;
        
        Div slotBlock = new Div();
        slotBlock.getStyle()
            .set("position", "absolute")
            .set("top", topPercent + "%")
            .set("left", "0")
            .set("right", "0")
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
     * Erstellt ein Label für einen TimeSlot mit Patientenzahl.
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
        String timeStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + 
                        " - " + endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        
        // Berechne Patienten pro Stunde
        long durationHours = java.time.temporal.ChronoUnit.HOURS.between(startTime, endTime);
        if (durationHours == 0) {
            durationHours = 1; // Mindestens 1 Stunde
        }
        double patientsPerHour = (double) patientCount / durationHours;
        
        String labelText = timeStr;
        if (patientCount > 0) {
            labelText += "\n" + patientCount + " Patient" + (patientCount > 1 ? "en" : "") + " geplant";
        } else {
            labelText += "\nFrei";
        }
        
        boolean isSelected = selectedTimeSlot != null 
            && selectedTimeSlot.getId() != null 
            && timeSlot.getId() != null
            && selectedTimeSlot.getId().equals(timeSlot.getId());
        
        // Bestimme Farbe basierend auf Patienten pro Stunde
        String backgroundColor;
        String textColor;
        if (isSelected) {
            backgroundColor = "var(--lumo-primary-color)";
            textColor = "var(--lumo-primary-contrast-color)";
        } else if (patientCount == 0) {
            // Freie Termine: hellgrün
            backgroundColor = "var(--lumo-success-color-10pct)";
            textColor = "var(--lumo-success-color)";
        } else if (patientsPerHour > 30) {
            // > 30 Patienten/Stunde: rot
            backgroundColor = "var(--lumo-error-color-20pct)";
            textColor = "var(--lumo-error-color)";
        } else if (patientsPerHour > 20) {
            // > 20 Patienten/Stunde: orange
            backgroundColor = "#ff9800";
            textColor = "#ffffff";
        } else if (patientsPerHour > 15) {
            // > 15 Patienten/Stunde: gelb
            backgroundColor = "var(--lumo-warning-color-20pct)";
            textColor = "var(--lumo-warning-color)";
        } else {
            // <= 15 Patienten/Stunde: hellgrün
            backgroundColor = "var(--lumo-success-color-10pct)";
            textColor = "var(--lumo-success-color)";
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
        
        com.vaadin.flow.component.html.H4 sectionTitle = new com.vaadin.flow.component.html.H4(title);
        sectionTitle.getStyle().set("margin-top", "0");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
        sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        sectionTitle.getStyle().set("font-weight", "600");
        section.add(sectionTitle);
        
        return section;
    }


    /**
     * Aktualisiert die Sichtbarkeit der Wochen-Auswahl basierend auf dem Intervall.
     */
    private void updateIntervalFields() {
        boolean isWeeks = "in Wochen".equals(intervalTypeGroup.getValue());
        weeksComboBox.setVisible(isWeeks);
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
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return; // Bereits gesetzt
        }
        
        // Versuche, Context aus TreatmentPlan zu setzen
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            InstitutionContext.setInstitutionId(treatmentPlan.getInstitution().getId());
            System.out.println("InstitutionContext gesetzt aus TreatmentPlan: " + treatmentPlan.getInstitution().getId());
        } else {
            System.out.println("WARNUNG: InstitutionContext konnte nicht gesetzt werden - TreatmentPlan hat keine Institution!");
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
            presenter.save(treatmentPlan.getId(), treatmentsToSave);

            showSuccess("Termin erfolgreich gebucht.");
            if (onTreatmentCreated != null) {
                onTreatmentCreated.accept(newTreatment);
            }
            close();
        } catch (Exception e) {
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
}

