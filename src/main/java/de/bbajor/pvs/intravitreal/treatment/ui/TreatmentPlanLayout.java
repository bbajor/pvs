package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;

import de.bbajor.pvs.appointment.service.AppointmentReportService;
import de.bbajor.pvs.base.ui.component.TimeLineCardConfig;
import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class TreatmentPlanLayout extends VerticalLayout {

    private Binder<TreatmentPlan> binder = new Binder<>(TreatmentPlan.class);
    private Runnable binderChangeListener; // Listener für Binder-Änderungen
    private int initialTreatmentCount = 0; // Anzahl der Treatments beim Laden, um Änderungen zu erkennen

    // Allgemeines
    private final DatePicker creationDatePicker = new DatePicker("Erstellt am");
    private final ComboBox<Patient> patientSelectComboBox = new ComboBox<>("Patient");
    private final ComboBox<Diagnosis> reasonForTreatmentComboBox = new ComboBox<>("Behandlungsgrund");

    // Filter
    private final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    private final ComboBox<MedicationFavourite> medicationComboBox = new ComboBox<>("Medikament");
    private final DatePicker startDatePicker = new DatePicker("Neue Termine finden ab");
    private final ComboBox<TimeSlotRepetition> repetitionComboBox = new ComboBox<>("Terminintervall");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>("Termine erstellen für");
    private final ComboBox<SurgicalCenter> surgicalCenterComboBox = new ComboBox<>(
            "Bevorzugten Behandlungsort auswählen");
    private final Button filterTimeSlotsButton = new Button("Verfügbare Termine anzeigen");
    private final Grid<SurgicalCenterTimeSlot> timeSlotGrid = new Grid<>();
    private final TimelineView timeLineViewLeftEye;
    private final TimelineView timeLineViewRightEye;
    private final TextArea additionalInformation = new TextArea("Notizen");
    private final TreatmentPlanPresenter presenter;
    private TreatmentPlan current;
    private final ApplicationContext context;
    
    // Sections
    private VerticalLayout overviewSection; // Neue "Übersicht"-Section für Patientendaten
    private VerticalLayout generalSection;
    private VerticalLayout treatmentHistorySection;
    // appointmentBookingSection und finishTreatmentPlanSection entfernt - werden nicht mehr benötigt
    // terminSectionDiv entfernt - wird nicht mehr benötigt
    private com.vaadin.flow.component.html.Div detailsSectionDiv; // Die Section-Div für "Details" (umbenannt von "Allgemein")
    private com.vaadin.flow.component.html.Div overviewSectionDiv; // Die Section-Div für "Übersicht"
    // finishSectionDiv entfernt - wird nicht mehr benötigt
    
    // View-Toggle und Container
    private VerticalLayout timelineContainer;
    private static final int MIN_INTERVAL_WEEKS = 1;
    private static final int DEFAULT_INTERVAL_WEEKS = 4;
    private static final int MAX_INTERVAL_WEEKS = 16;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter, TreatmentPlan treatmentPlan,
            ApplicationContext context) {
        this.presenter = presenter;
        this.current = treatmentPlan;
        this.context = context;

        setSizeFull();
        // overflow entfernt - erlaube Scrollen wenn nötig
        
        timeLineViewLeftEye = new TimelineView(context);
        timeLineViewLeftEye.setSideOfEye(SideOfEye.LEFT);
        timeLineViewRightEye = new TimelineView(context);
        timeLineViewRightEye.setSideOfEye(SideOfEye.RIGHT);

        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new TreatmentPlan());
                }
                binder.getBean().setPatient(event.getValue());
                // Aktualisiere Übersicht-Section live bei Patientenauswahl
                updateOverviewSection();
            } else {
                // Wenn kein Patient ausgewählt, Übersicht leeren
                updateOverviewSection();
            }
        });
        medicationComboBox.setItems(presenter.getDrugs());
        medicationComboBox.setItemLabelGenerator(MedicationFavourite::getEffectiveDisplayName);
        medicationComboBox.setClearButtonVisible(true);

        sideOfEye.setItems(SideOfEye.values());
        timeSlotGrid.setSizeFull();
        timeSlotGrid.setMinHeight("500px");
        timeSlotGrid.addColumn(SurgicalCenterTimeSlot::getSurgicalCenter).setHeader("Einrichtung");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        timeSlotGrid.addColumn(dto -> dto.getDate().format(formatter))
                .setHeader("Datum");
        timeSlotGrid.setSelectionMode(SelectionMode.MULTI);

        surgicalCenterComboBox.setItems(presenter.getSurgicalCenters());
        surgicalCenterComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDatePicker.setEnabled(false);

        reasonForTreatmentComboBox.setItemLabelGenerator(Diagnosis::getName);
        reasonForTreatmentComboBox.setAllowCustomValue(true);
        reasonForTreatmentComboBox.setClearButtonVisible(true);
        reasonForTreatmentComboBox.addCustomValueSetListener(event -> {
            String newValue = event.getDetail();
            // Optional: trim & prüfen
            if (newValue != null && !newValue.trim().isEmpty()) {
                // Neues entity
                Diagnosis newDiagnosis = new Diagnosis();
                newDiagnosis.setName(newValue.trim());

                // In DB speichern, falls nötig
                Diagnosis saved = presenter.saveDiagnosis(newDiagnosis);

                // ComboBox aktualisieren
                List<Diagnosis> items = new ArrayList<>(
                        reasonForTreatmentComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(saved);
                reasonForTreatmentComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                reasonForTreatmentComboBox.setValue(saved);
            }
        });
        reasonForTreatmentComboBox.setItems(presenter.getResaonsForTreatment());

        initializeBinder(treatmentPlan);
        
        // Sections statt Tabs
        initializeOverviewSection();
        initializeGeneralSection();
        initializeTreatmentHistorySection();
        
        // Layout: Übersicht und Details nebeneinander - gleich hoch
        HorizontalLayout overviewDetailsLayout = new HorizontalLayout();
        overviewDetailsLayout.setSizeFull();
        overviewDetailsLayout.setSpacing(true);
        overviewDetailsLayout.setPadding(false);
        overviewDetailsLayout.setMargin(false);
        overviewDetailsLayout.add(overviewSection);
        overviewDetailsLayout.add(generalSection);
        overviewDetailsLayout.setFlexGrow(1, overviewSection, generalSection);
        // Gleiche Höhe für beide Sections
        overviewSection.setHeightFull();
        generalSection.setHeightFull();
        
        add(overviewDetailsLayout);
        add(treatmentHistorySection);
    }

    private void initializeOverviewSection() {
        overviewSection = new VerticalLayout();
        overviewSection.setSpacing(false);
        overviewSection.setPadding(false);
        overviewSection.setWidthFull();
        overviewSection.setHeightFull(); // Für gleiche Höhe
        
        // Section "Übersicht" - für Patientendaten
        overviewSectionDiv = createSection("Übersicht");
        overviewSectionDiv.setHeightFull(); // Div nutzt verfügbare Höhe
        overviewSection.add(overviewSectionDiv);
        overviewSection.setFlexGrow(1, overviewSectionDiv); // Div wächst
        
        // Initiale Patientendaten anzeigen
        updateOverviewSection();
    }
    
    private void initializeGeneralSection() {
        generalSection = new VerticalLayout();
        generalSection.setSpacing(false);
        generalSection.setPadding(false);
        generalSection.setWidthFull();
        generalSection.setHeightFull(); // Für gleiche Höhe
        
        // Section "Details" - umbenannt von "Allgemein"
        detailsSectionDiv = createSection("Details");
        detailsSectionDiv.setHeightFull(); // Div nutzt verfügbare Höhe
        
        // Formular für Behandlungsplan-Details
        // Erstellt am und Patient entfernt, aber Patient-Combobox bei Neuanlage behalten
        FormLayout formLayout = new FormLayout();
        // Patient-Combobox wird dynamisch angezeigt/versteckt
        formLayout.add(patientSelectComboBox);
        formLayout.add(reasonForTreatmentComboBox, 2);
        formLayout.add(additionalInformation, 2);
        detailsSectionDiv.add(formLayout);
        
        generalSection.add(detailsSectionDiv);
        generalSection.setFlexGrow(1, detailsSectionDiv); // Div wächst
        
        // Initiale Sichtbarkeit setzen
        updateDetailsSection();
    }
    
    /**
     * Aktualisiert die Details-Section: Patient-Combobox nur bei Neuanlage anzeigen
     */
    private void updateDetailsSection() {
        if (patientSelectComboBox == null) {
            return;
        }
        
        // Patient-Combobox nur anzeigen, wenn kein Patient vorhanden (Neuanlage)
        boolean showPatientComboBox = (current == null || current.getId() == null || current.getId() == -1 || current.getPatient() == null);
        patientSelectComboBox.setVisible(showPatientComboBox);
    }
    
    private void initializeTreatmentHistorySection() {
        treatmentHistorySection = new VerticalLayout();
        treatmentHistorySection.setSizeFull();
        treatmentHistorySection.setPadding(false);
        treatmentHistorySection.setSpacing(false);
        treatmentHistorySection.setWidthFull();
        
        // Section "Behandlungsverlauf" - als Section wie die anderen
        com.vaadin.flow.component.html.Div treatmentHistorySectionDiv = createSection("Behandlungsverlauf");
        
        // Container für Timeline-Ansicht (nur Timeline, kein Grid mehr)
        timelineContainer = new VerticalLayout();
        timelineContainer.setSizeFull();
        timelineContainer.setPadding(false);
        timelineContainer.setSpacing(false);
        timeLineViewLeftEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        timeLineViewRightEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        updateTimelineLayout(timelineContainer, TimelineView.Orientation.HORIZONTAL);
        
        treatmentHistorySectionDiv.add(timelineContainer);
        treatmentHistorySection.add(treatmentHistorySectionDiv);
        treatmentHistorySection.expand(treatmentHistorySectionDiv);
    }
    
    // Grid-Ansicht und View-Toggle wurden entfernt - nur Timeline wird angezeigt
    // Alle Grid-Methoden wurden entfernt: createEyeOverview, createTreatmentGrid, refreshGrids, refreshGridsWithTreatments
    
    // initializeAppointmentBookingSection() und updateAppointmentBookingSection() entfernt - wird nicht mehr benötigt
    // Die Logik ist jetzt in updateOverviewSection()
    
    private void openAppointmentPlanningDialog() {
        if (current == null || current.getId() == null || current.getId() == -1) {
            Notification.show("Bitte speichern Sie zuerst den Behandlungsplan.", 3000,
                    Notification.Position.MIDDLE);
            return;
        }
        
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Terminserie buchen");
        dialog.setWidth("90vw");
        dialog.setMaxWidth("1400px");
        dialog.setHeight("90vh");
        
        // Erstelle separate Grid für den Dialog, damit wir die Auswahl nicht mit der Hauptansicht teilen
        Grid<SurgicalCenterTimeSlot> dialogTimeSlotGrid = new Grid<>();
        dialogTimeSlotGrid.setSizeFull();
        dialogTimeSlotGrid.setMinHeight("500px");
        dialogTimeSlotGrid.setSelectionMode(SelectionMode.MULTI);
        
        // Spalte: Einrichtung (breiter, resizable)
        Grid.Column<SurgicalCenterTimeSlot> centerColumn = dialogTimeSlotGrid.addColumn(ts -> 
            ts.getSurgicalCenter() != null ? ts.getSurgicalCenter().getName() : "-")
            .setHeader("Einrichtung")
            .setResizable(true)
            .setAutoWidth(false)
            .setWidth("300px");
        
        // Spalte: Datum
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        Grid.Column<SurgicalCenterTimeSlot> dateColumn = dialogTimeSlotGrid.addColumn(ts -> 
            ts.getDate() != null ? ts.getDate().format(dateFormatter) : "-")
            .setHeader("Datum")
            .setResizable(true)
            .setAutoWidth(false)
            .setWidth("150px");
        
        // Spalte: Uhrzeit
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
        Grid.Column<SurgicalCenterTimeSlot> timeColumn = dialogTimeSlotGrid.addColumn(ts -> {
            if (ts.getStartTime() == null) {
                return "-";
            }
            String startTime = ts.getStartTime().format(timeFormatter);
            if (ts.getEndTime() != null) {
                String endTime = ts.getEndTime().format(timeFormatter);
                return startTime + " - " + endTime;
            }
            return startTime;
        })
        .setHeader("Uhrzeit")
        .setResizable(true)
        .setAutoWidth(false)
        .setWidth("120px");
        
        VerticalLayout appointmentLayout = new VerticalLayout();
        appointmentLayout.setSizeFull();
        
        // Filter-Komponenten für den Dialog
        FormLayout formLayout = new FormLayout();
        formLayout.add(sideOfEye);
        formLayout.add(medicationComboBox);
        formLayout.add(surgicalCenterComboBox);
        startDatePicker.setValue(LocalDate.now());
        formLayout.add(startDatePicker);
        timePeriodComboBox.setItems(TimePeriod.values());
        timePeriodComboBox.setValue(TimePeriod.THREE_MONTHS);
        formLayout.add(timePeriodComboBox);
        repetitionComboBox.setItems(TimeSlotRepetition.values());
        repetitionComboBox.setValue(TimeSlotRepetition.EVERY_FOUR_WEEKS);
        formLayout.add(repetitionComboBox);
        
        Button filterButton = new Button("Verfügbare Termine anzeigen");
        filterButton.addClickListener(click -> {
            SurgicalCenter selectedCenter = surgicalCenterComboBox.getValue();
            Integer id = selectedCenter == null ? null : selectedCenter.getId();
            ensureInstitutionContext();
            Collection<SurgicalCenterTimeSlot> availableAndFilteredSlots = presenter.getAllTimeSlotsFilteredBy(
                    startDatePicker.getValue(), timePeriodComboBox.getValue(), repetitionComboBox.getValue(),
                    id);
            dialogTimeSlotGrid.setItems(availableAndFilteredSlots);
        });
        formLayout.add(filterButton);
        
        appointmentLayout.add(formLayout);
        appointmentLayout.add(dialogTimeSlotGrid);
        appointmentLayout.expand(dialogTimeSlotGrid);
        
        dialog.add(appointmentLayout);
        
        Button saveButton = new Button("Termine speichern", e -> {
            Set<SurgicalCenterTimeSlot> selectedSlots = dialogTimeSlotGrid.getSelectedItems();
            if (selectedSlots.isEmpty()) {
                Notification.show("Bitte wählen Sie mindestens einen Termin aus.", 3000,
                        Notification.Position.MIDDLE);
                return;
            }
            
            if (sideOfEye.getValue() == null) {
                Notification.show("Bitte wählen Sie ein Auge aus.", 3000,
                        Notification.Position.MIDDLE);
                return;
            }
            
            List<Treatment> treatmentsToCreate = new ArrayList<>();
            for (SurgicalCenterTimeSlot timeSlot : selectedSlots) {
                Treatment treatment = new Treatment();
                treatment.setSideOfEye(sideOfEye.getValue());
                treatment.setMedicationFavourite(medicationComboBox.getValue());
                treatment.setSurgicalCenterTimeSlot(timeSlot);
                treatment.setTreatmentPlan(current);
                treatmentsToCreate.add(treatment);
            }
            
            try {
                ensureInstitutionContext();
                presenter.save(current.getId(), treatmentsToCreate);
                Notification.show("Terminserie erfolgreich gespeichert.", 3000,
                        Notification.Position.BOTTOM_CENTER);
                
                // Aktualisiere Timeline und Section
                setLeftEyeTreatmentHistory(current.getId());
                setRightEyeTreatmentHistory(current.getId());
                updateOverviewSection();
                
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Fehler beim Speichern: " + ex.getMessage(), 5000,
                        Notification.Position.MIDDLE);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button closeButton = new Button("Abbrechen", e -> dialog.close());
        dialog.getFooter().add(closeButton, saveButton);
        
        // Lade verfügbare Termine
        filterButton.click();
        
        dialog.open();
    }
    
    private void generateAppointmentReport() {
        if (current == null || current.getPatient() == null) {
            Notification.show("Kein Patient ausgewählt", 3000, 
                    Notification.Position.BOTTOM_CENTER);
            return;
        }
        
        if (current.getId() == null || current.getId() == -1) {
            Notification.show("Bitte speichern Sie zuerst den Behandlungsplan.", 3000,
                    Notification.Position.MIDDLE);
            return;
        }
        
        try {
            ensureInstitutionContext();
            
            // Hole alle zukünftigen Treatments für den Patienten
            LocalDate now = LocalDate.now();
            List<Treatment> leftTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
            List<Treatment> rightTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
            
            // Kombiniere alle Treatments und filtere zukünftige
            List<Treatment> allTreatments = new ArrayList<>();
            allTreatments.addAll(leftTreatments);
            allTreatments.addAll(rightTreatments);
            
            List<Treatment> futureTreatments = allTreatments.stream()
                .filter(t -> t.getDate() != null && !t.getDate().isBefore(now))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(java.util.stream.Collectors.toList());
            
            // Verwende Treatments direkt für den Report
            AppointmentReportService reportService = context.getBean(AppointmentReportService.class);
            Patient patient = current.getPatient();
            
            byte[] pdfBytes;
            if (futureTreatments.isEmpty()) {
                Notification.show("Keine zukünftigen Termine gefunden.", 3000,
                        Notification.Position.MIDDLE);
                return;
            }
            
            // Verwende Treatments direkt
            pdfBytes = reportService.generatePatientTreatmentReport(futureTreatments, patient);
            
            // Erstelle Download-Link
            String patientName = patient.getLastName() + "_" + patient.getFirstName();
            String filename = "Terminuebersicht_" + patientName + "_" + 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            Notification.show("Terminübersicht wird heruntergeladen", 3000, 
                    Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            Notification notification = Notification.show(
                    "Fehler beim Generieren des Termin-Ausdrucks: " + e.getMessage(), 5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
        }
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set.
     * Tries multiple sources in order:
     * 1. Already set InstitutionContext
     * 2. TreatmentPlan's institution (if available)
     * 3. Authentication token
     * 4. UserAccount's institution
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        // Try to get institution from TreatmentPlan first (most reliable for this context)
        if (current != null && current.getInstitution() != null && current.getInstitution().getId() != null) {
            InstitutionContext.setInstitutionId(current.getInstitution().getId());
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                return;
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccountRepository userAccountRepository = context.getBean(UserAccountRepository.class);
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    return;
                }
            } catch (Exception e) {
                // Log but don't fail - will be caught by service layer
            }
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        // Create StreamResource for download
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new java.io.ByteArrayInputStream(pdfBytes);
        });
        streamResource.setContentType("application/pdf");
        
        // Register the resource and get the URL
        getUI().ifPresent(ui -> {
            StreamRegistration registration = ui.getSession().getResourceRegistry()
                    .registerResource(streamResource);
            String resourceUrl = registration.getResourceUri().toString();
            
            // Create download link and trigger download via JavaScript
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
    
    // Maps für Info-Kacheln und Terminbuchung pro Auge
    private final java.util.Map<SideOfEye, com.vaadin.flow.component.html.Div> infoCardMap = new java.util.HashMap<>();
    private final java.util.Map<SideOfEye, com.vaadin.flow.component.html.Div> bookingCardMap = new java.util.HashMap<>();
    
    private void updateTimelineLayout(VerticalLayout timeLineLayout, TimelineView.Orientation orientation) {
        // Remove existing timeline accordions and eyesContainer
        timeLineLayout.removeAll();
        
        // Immer Timeline anzeigen, auch wenn noch keine Behandlungen existieren
        // Beide Augen immer anzeigen (auch wenn noch keine Behandlungen existieren)
        
        if (orientation == TimelineView.Orientation.VERTICAL) {
            // Vertical: show eyes side by side
            VerticalLayout eyesContainer = new VerticalLayout();
            eyesContainer.setWidthFull();
            eyesContainer.setHeightFull();
            eyesContainer.setPadding(false);
            eyesContainer.setSpacing(false);
            eyesContainer.getStyle().set("display", "flex");
            eyesContainer.getStyle().set("flex-direction", "row");
            
            // Immer beide Augen anzeigen, auch wenn noch keine Behandlungen existieren
            initializeTimeLineRightEye(eyesContainer); // OD (rechts vom Patienten = links in UI)
            initializeTimeLineLeftEye(eyesContainer);  // OS (links vom Patienten = rechts in UI)
            
            timeLineLayout.add(eyesContainer);
            timeLineLayout.expand(eyesContainer);
        } else {
            // Horizontal: show vertically stacked mit Info-Kachel links und Terminbuchung rechts
            // Rechtes Auge - mit Rahmen um gesamte Section
            com.vaadin.flow.component.html.Div rightEyeSection = new com.vaadin.flow.component.html.Div();
            rightEyeSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin", "0")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-sizing", "border-box");
            rightEyeSection.setWidthFull();
            
            HorizontalLayout rightEyeLayout = new HorizontalLayout();
            rightEyeLayout.setWidthFull();
            rightEyeLayout.setSpacing(true);
            rightEyeLayout.setPadding(false);
            
            // Info-Kachel links
            com.vaadin.flow.component.html.Div rightInfoCard = createInfoCard(SideOfEye.RIGHT);
            infoCardMap.put(SideOfEye.RIGHT, rightInfoCard);
            rightInfoCard.getStyle().set("margin-right", "var(--lumo-space-s)");
            rightEyeLayout.add(rightInfoCard);
            
            // Timeline in der Mitte - horizontal scrollbar
            VerticalLayout rightTimelineWrapper = new VerticalLayout();
            rightTimelineWrapper.setPadding(false);
            rightTimelineWrapper.setSpacing(false);
            rightTimelineWrapper.setWidthFull();
            rightTimelineWrapper.setHeightFull();
            rightTimelineWrapper.getStyle().set("min-height", "300px"); // Feste Mindesthöhe
            // Timeline soll horizontal scrollbar sein, aber nicht über die Terminbuchung hinaus wachsen
            rightTimelineWrapper.getStyle().set("overflow-x", "auto");
            rightTimelineWrapper.getStyle().set("overflow-y", "hidden");
            rightTimelineWrapper.getStyle().set("min-width", "0"); // Wichtig: erlaube Schrumpfen
            initializeTimeLineRightEye(rightTimelineWrapper);
            rightEyeLayout.add(rightTimelineWrapper);
            rightEyeLayout.setFlexGrow(1, rightTimelineWrapper);
            
            // Terminbuchung rechts
            com.vaadin.flow.component.html.Div rightBookingCard = createBookingCard(SideOfEye.RIGHT);
            bookingCardMap.put(SideOfEye.RIGHT, rightBookingCard);
            rightBookingCard.getStyle().set("margin-left", "var(--lumo-space-s)");
            rightEyeLayout.add(rightBookingCard);
            // Terminbuchung soll nicht schrumpfen
            rightEyeLayout.setFlexGrow(0, rightBookingCard);
            
            rightEyeSection.add(rightEyeLayout);
            timeLineLayout.add(rightEyeSection);
            
            // Linkes Auge - mit Rahmen um gesamte Section
            com.vaadin.flow.component.html.Div leftEyeSection = new com.vaadin.flow.component.html.Div();
            leftEyeSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("margin", "0")
                .set("margin-bottom", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-sizing", "border-box");
            leftEyeSection.setWidthFull();
            
            HorizontalLayout leftEyeLayout = new HorizontalLayout();
            leftEyeLayout.setWidthFull();
            leftEyeLayout.setSpacing(true);
            leftEyeLayout.setPadding(false);
            
            // Info-Kachel links
            com.vaadin.flow.component.html.Div leftInfoCard = createInfoCard(SideOfEye.LEFT);
            infoCardMap.put(SideOfEye.LEFT, leftInfoCard);
            leftInfoCard.getStyle().set("margin-right", "var(--lumo-space-s)");
            leftEyeLayout.add(leftInfoCard);
            
            // Timeline in der Mitte - horizontal scrollbar
            VerticalLayout leftTimelineWrapper = new VerticalLayout();
            leftTimelineWrapper.setPadding(false);
            leftTimelineWrapper.setSpacing(false);
            leftTimelineWrapper.setWidthFull();
            leftTimelineWrapper.setHeightFull();
            leftTimelineWrapper.getStyle().set("min-height", "300px"); // Feste Mindesthöhe
            // Timeline soll horizontal scrollbar sein, aber nicht über die Terminbuchung hinaus wachsen
            leftTimelineWrapper.getStyle().set("overflow-x", "auto");
            leftTimelineWrapper.getStyle().set("overflow-y", "hidden");
            leftTimelineWrapper.getStyle().set("min-width", "0"); // Wichtig: erlaube Schrumpfen
            initializeTimeLineLeftEye(leftTimelineWrapper);
            leftEyeLayout.add(leftTimelineWrapper);
            leftEyeLayout.setFlexGrow(1, leftTimelineWrapper);
            
            // Terminbuchung rechts
            com.vaadin.flow.component.html.Div leftBookingCard = createBookingCard(SideOfEye.LEFT);
            bookingCardMap.put(SideOfEye.LEFT, leftBookingCard);
            leftBookingCard.getStyle().set("margin-left", "var(--lumo-space-s)");
            leftEyeLayout.add(leftBookingCard);
            // Terminbuchung soll nicht schrumpfen
            leftEyeLayout.setFlexGrow(0, leftBookingCard);
            
            leftEyeSection.add(leftEyeLayout);
            timeLineLayout.add(leftEyeSection);
        }
    }
    
    /**
     * Erstellt eine Info-Kachel mit Behandlungsplan-Informationen.
     */
    private com.vaadin.flow.component.html.Div createInfoCard(SideOfEye side) {
        com.vaadin.flow.component.html.Div card = new com.vaadin.flow.component.html.Div();
        card.addClassName("timeline-info-card");
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("min-width", "250px")
            .set("max-width", "300px")
            .set("flex-shrink", "0");
        
        updateInfoCard(card, side);
        return card;
    }
    
    /**
     * Aktualisiert die Info-Kachel mit aktuellen Daten.
     */
    private void updateInfoCard(com.vaadin.flow.component.html.Div card, SideOfEye side) {
        card.removeAll();
        
        if (current == null || current.getId() == null || current.getId() == -1) {
            com.vaadin.flow.component.html.Span noData = new com.vaadin.flow.component.html.Span("Keine Daten verfügbar");
            card.add(noData);
            return;
        }
        
        ensureInstitutionContext();
        List<Treatment> treatments = presenter.getTreatmentDtos(side, current.getId());
        LocalDate now = LocalDate.now();
        
        // Titel
        com.vaadin.flow.component.html.H4 title = new com.vaadin.flow.component.html.H4(
            side == SideOfEye.LEFT ? "Linkes Auge (OS)" : "Rechtes Auge (OD)");
        title.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-s)")
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-weight", "600");
        card.add(title);
        
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setSpacing(false);
        infoLayout.setPadding(false);
        
        // Behandlungsplan Start
        LocalDate startDate = current.getCreationDate() != null ? current.getCreationDate() : now;
        String startText = "Behandlungsplan Start: " + 
            startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN));
        infoLayout.add(createInfoRow(startText));
        
        // Anzahl Behandlungen
        int treatmentCount = treatments.size();
        String countText = "Anzahl Behandlungen: " + treatmentCount;
        infoLayout.add(createInfoRow(countText));
        
        // Häufigstes Intervall
        Integer mostCommonInterval = calculateMostCommonInterval(treatments);
        if (mostCommonInterval != null) {
            String intervalText = "Häufigstes Intervall: " + mostCommonInterval + " Wochen";
            infoLayout.add(createInfoRow(intervalText));
        }
        
        // Nächster Termin
        Treatment nextTreatment = treatments.stream()
            .filter(t -> t.getDate() != null && (t.getDate().isAfter(now) || t.getDate().isEqual(now)))
            .min((a, b) -> a.getDate().compareTo(b.getDate()))
            .orElse(null);
        
        if (nextTreatment != null && nextTreatment.getDate() != null) {
            String nextText = "Nächster Termin: " + 
                nextTreatment.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN));
            if (nextTreatment.getSurgicalCenterTimeSlot() != null && 
                nextTreatment.getSurgicalCenterTimeSlot().getStartTime() != null) {
                nextText += " " + 
                    nextTreatment.getSurgicalCenterTimeSlot().getStartTime()
                        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN));
            }
            infoLayout.add(createInfoRow(nextText));
        } else {
            infoLayout.add(createInfoRow("Nächster Termin: Kein Termin geplant"));
        }
        
        card.add(infoLayout);
    }
    
    /**
     * Erstellt eine Terminbuchungs-Kachel.
     */
    private com.vaadin.flow.component.html.Div createBookingCard(SideOfEye side) {
        com.vaadin.flow.component.html.Div card = new com.vaadin.flow.component.html.Div();
        card.addClassName("timeline-booking-card");
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("min-width", "250px")
            .set("max-width", "300px")
            .set("flex-shrink", "0");
        
        updateBookingCard(card, side);
        return card;
    }
    
    /**
     * Aktualisiert die Terminbuchungs-Kachel.
     */
    private void updateBookingCard(com.vaadin.flow.component.html.Div card, SideOfEye side) {
        card.removeAll();
        
        if (current == null || current.getId() == null || current.getId() == -1) {
            com.vaadin.flow.component.html.Span noData = new com.vaadin.flow.component.html.Span("Bitte speichern Sie zuerst den Behandlungsplan.");
            card.add(noData);
            return;
        }
        
        ensureInstitutionContext();
        List<Treatment> treatments = presenter.getTreatmentDtos(side, current.getId());
        int treatmentCount = treatments != null ? treatments.size() : 0;
        
        // Titel
        com.vaadin.flow.component.html.H4 title = new com.vaadin.flow.component.html.H4("Termin buchen");
        title.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-s)")
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-weight", "600");
        card.add(title);
        
        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(false);
        buttonLayout.setWidthFull();
        
        if (treatmentCount < 2) {
            // Weniger als 2 Behandlungen: Einfacher Button
            Button bookButton = new Button("Folgetermin buchen", VaadinIcon.CALENDAR.create());
            bookButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            bookButton.setWidthFull();
            bookButton.addClickListener(e -> openNextTreatmentBookingDialog(side));
            buttonLayout.add(bookButton);
        } else {
            // Ab 2 Behandlungen: Intervall-Buttons
            // Berechne vorheriges Intervall für Stepper-Initialisierung
            List<Treatment> sortedTreatments = new ArrayList<>(treatments);
            sortedTreatments.sort(this::compareTreatmentsByDate);
            Treatment lastTreatment = sortedTreatments.isEmpty() ? null : sortedTreatments.get(sortedTreatments.size() - 1);
            Treatment previousTreatment = sortedTreatments.size() > 1 ? sortedTreatments.get(sortedTreatments.size() - 2) : null;
            int previousIntervalWeeks = calculatePreviousIntervalWeeks(previousTreatment, lastTreatment);
            
            // Verkürztes Intervall mit Stepper
            HorizontalLayout shorterLayout = new HorizontalLayout();
            shorterLayout.setSpacing(true);
            shorterLayout.setWidthFull();
            shorterLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            
            IntegerField shorterWeeksField = new IntegerField();
            shorterWeeksField.setValue(1);
            shorterWeeksField.setMin(1);
            shorterWeeksField.setMax(MAX_INTERVAL_WEEKS);
            shorterWeeksField.setStep(1);
            shorterWeeksField.setWidth("80px");
            shorterWeeksField.setSuffixComponent(new com.vaadin.flow.component.html.Span("Woche"));
            shorterWeeksField.getStyle().set("flex-shrink", "0");
            
            Button shorterButton = new Button("Verkürzen", VaadinIcon.ARROW_LEFT.create());
            shorterButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            shorterButton.setWidthFull();
            shorterButton.addClickListener(e -> {
                Integer weeksToShorten = shorterWeeksField.getValue();
                if (weeksToShorten == null || weeksToShorten < 1) {
                    weeksToShorten = 1;
                }
                int targetWeeks = Math.max(MIN_INTERVAL_WEEKS, previousIntervalWeeks - weeksToShorten);
                handleQuickBooking(new TimelineView.QuickBookingRequest(side, TimelineView.QuickBookingAction.CUSTOM_INTERVAL, targetWeeks));
            });
            
            shorterLayout.add(shorterWeeksField);
            shorterLayout.add(shorterButton);
            shorterLayout.setFlexGrow(1, shorterButton);
            buttonLayout.add(shorterLayout);
            
            Button sameButton = new Button("Gleiches Intervall", VaadinIcon.ARROW_CIRCLE_RIGHT.create());
            sameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            sameButton.setWidthFull();
            sameButton.addClickListener(e -> handleQuickBooking(
                new TimelineView.QuickBookingRequest(side, TimelineView.QuickBookingAction.SAME_INTERVAL, null)));
            buttonLayout.add(sameButton);
            
            // Verlängertes Intervall mit Stepper
            HorizontalLayout longerLayout = new HorizontalLayout();
            longerLayout.setSpacing(true);
            longerLayout.setWidthFull();
            longerLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            
            IntegerField longerWeeksField = new IntegerField();
            longerWeeksField.setValue(1);
            longerWeeksField.setMin(1);
            longerWeeksField.setMax(MAX_INTERVAL_WEEKS);
            longerWeeksField.setStep(1);
            longerWeeksField.setWidth("80px");
            longerWeeksField.setSuffixComponent(new com.vaadin.flow.component.html.Span("Woche"));
            longerWeeksField.getStyle().set("flex-shrink", "0");
            
            Button longerButton = new Button("Verlängern", VaadinIcon.ARROW_RIGHT.create());
            longerButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            longerButton.setWidthFull();
            longerButton.addClickListener(e -> {
                Integer weeksToExtend = longerWeeksField.getValue();
                if (weeksToExtend == null || weeksToExtend < 1) {
                    weeksToExtend = 1;
                }
                int targetWeeks = Math.min(MAX_INTERVAL_WEEKS, previousIntervalWeeks + weeksToExtend);
                handleQuickBooking(new TimelineView.QuickBookingRequest(side, TimelineView.QuickBookingAction.CUSTOM_INTERVAL, targetWeeks));
            });
            
            longerLayout.add(longerWeeksField);
            longerLayout.add(longerButton);
            longerLayout.setFlexGrow(1, longerButton);
            buttonLayout.add(longerLayout);
            
            // Nächster Anschlusstermin mit Info-Icon
            HorizontalLayout nextAvailableLayout = new HorizontalLayout();
            nextAvailableLayout.setSpacing(true);
            nextAvailableLayout.setWidthFull();
            nextAvailableLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            
            Button nextAvailableButton = new Button("Anschlusstermin", VaadinIcon.CALENDAR_CLOCK.create());
            nextAvailableButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            nextAvailableButton.setWidthFull();
            nextAvailableButton.addClickListener(e -> handleQuickBooking(
                new TimelineView.QuickBookingRequest(side, TimelineView.QuickBookingAction.NEXT_AVAILABLE, null)));
            
            Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
            infoIcon.setSize("16px");
            infoIcon.setColor("var(--lumo-secondary-text-color)");
            infoIcon.setTooltipText("Bucht den nächstmöglichen Anschlusstermin, der auf den letzten Termin folgt");
            
            nextAvailableLayout.add(nextAvailableButton);
            nextAvailableLayout.add(infoIcon);
            nextAvailableLayout.setFlexGrow(1, nextAvailableButton);
            nextAvailableLayout.setFlexGrow(0, infoIcon);
            buttonLayout.add(nextAvailableLayout);
        }
        
        card.add(buttonLayout);
    }
    
    /**
     * Aktualisiert Info-Kachel und Terminbuchung für ein Auge.
     */
    private void updateTimelineInfoAndBooking(SideOfEye side) {
        com.vaadin.flow.component.html.Div infoCard = infoCardMap.get(side);
        if (infoCard != null) {
            updateInfoCard(infoCard, side);
        }
        
        com.vaadin.flow.component.html.Div bookingCard = bookingCardMap.get(side);
        if (bookingCard != null) {
            updateBookingCard(bookingCard, side);
        }
    }
    
    private boolean hasTreatmentsForEye(SideOfEye side) {
        if (current == null || current.getId() == null) {
            return false;
        }
        List<Treatment> treatments = presenter.getTreatmentDtos(side, current.getId());
        return treatments != null && !treatments.isEmpty();
    }


    private void initializeBinder(TreatmentPlan dto) {
        binder.bind(creationDatePicker, TreatmentPlan::getCreationDate,
                TreatmentPlan::setCreationDate);
        binder.bind(additionalInformation, TreatmentPlan::getAdditionalInformation,
                TreatmentPlan::setAdditionalInformation);
        binder.bind(patientSelectComboBox, TreatmentPlan::getPatient, TreatmentPlan::setPatient);
        binder.bind(reasonForTreatmentComboBox, TreatmentPlan::getDiagnosis,
                TreatmentPlan::setDiagnosis);
        
        // Binder-Änderungen überwachen für Button-Status
        binder.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });
        
        // Auch ComboBox-Änderungen explizit überwachen
        reasonForTreatmentComboBox.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });
        patientSelectComboBox.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });
        creationDatePicker.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });
        additionalInformation.addValueChangeListener(e -> {
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        });
        
        binder.setBean(dto == null ? new TreatmentPlan() : dto);
    }

    private void initializeTimeSlotFilter(VerticalLayout verticalLayout) {
        FormLayout formLayout = new FormLayout();
        formLayout.add(sideOfEye);
        formLayout.add(medicationComboBox);
        formLayout.add(surgicalCenterComboBox);
        startDatePicker.setValue(LocalDate.now());
        formLayout.add(startDatePicker);
        timePeriodComboBox.setItems(TimePeriod.values());
        timePeriodComboBox.setValue(TimePeriod.THREE_MONTHS);
        formLayout.add(timePeriodComboBox);
        repetitionComboBox.setItems(TimeSlotRepetition.values());
        repetitionComboBox.setValue(TimeSlotRepetition.EVERY_FOUR_WEEKS);
        formLayout.add(repetitionComboBox);
        formLayout.add(filterTimeSlotsButton);

        filterTimeSlotsButton.addClickListener(click -> {
            SurgicalCenter selectedCenter = surgicalCenterComboBox.getValue();
            Integer id = selectedCenter == null ? null : selectedCenter.getId();
            Collection<SurgicalCenterTimeSlot> availableAndFilteredSlots = presenter.getAllTimeSlotsFilteredBy(
                    startDatePicker.getValue(), timePeriodComboBox.getValue(), repetitionComboBox.getValue(),
                    id);
            timeSlotGrid.setItems(availableAndFilteredSlots);
            timeSlotGrid.setItems(availableAndFilteredSlots);
        });

        verticalLayout.add(formLayout);
    }

    private void initializeTimeLineRightEye(VerticalLayout timeLineLayout) {
        // rechtes Auge mit medizinisch korrekter Darstellung (rechts = links vom Patienten)
        timeLineViewRightEye.setTimelineHeight("300px"); // Feste Höhe für gleichmäßige Darstellung
        timeLineViewRightEye.addClassName("right-eye-timeline");
        timeLineViewRightEye.getStyle().set("background-color", "#E3F2FD"); // Blue tint
        timeLineViewRightEye.setWidthFull();
        timeLineViewRightEye.setHeightFull();
        timeLineLayout.add(timeLineViewRightEye);
        if (timeLineLayout instanceof VerticalLayout) {
            ((VerticalLayout) timeLineLayout).expand(timeLineViewRightEye);
        }
        setRightEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setRightEyeTreatmentHistory(Long treatmentPlanId) {
        setRightEyeTreatmentHistory(treatmentPlanId, null);
    }
    
    private void setRightEyeTreatmentHistory(Long treatmentPlanId, List<Treatment> allTreatments) {
        List<TimeLineCardConfig> rightEyeTreatments = new ArrayList<>();
        
        if (treatmentPlanId != null) {
            List<Treatment> treatments;
            if (allTreatments != null) {
                // Verwende bereits geladene Treatments statt neue Query
                treatments = allTreatments.stream()
                    .filter(t -> SideOfEye.RIGHT.equals(t.getSideOfEye()))
                    .collect(java.util.stream.Collectors.toList());
            } else {
                // Fallback: Lade Treatments wenn nicht vorhanden
                treatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, treatmentPlanId);
            }
            
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                rightEyeTreatments.add(config);
            }
        }
        
        // Keine Startkachel mehr - wird jetzt außerhalb der Timeline angezeigt
        
        // Callback nach dem Löschen: Timeline neu laden
        timeLineViewRightEye.setOnTreatmentDeletedCallback(() -> {
            if (current != null && current.getId() != null) {
                setRightEyeTreatmentHistory(current.getId());
            }
        });
        
        // Auch bei null (neuer Plan) initialisieren
        timeLineViewRightEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewRightEye.setItems(rightEyeTreatments);
        
        // Aktualisiere Info-Kachel und Terminbuchung
        updateTimelineInfoAndBooking(SideOfEye.RIGHT);
    }

    
    /**
     * Erstellt eine Card mit den wichtigsten Patientendaten.
     * Diese ist immer sichtbar, damit der Arzt schnell die wichtigsten Infos sieht.
     */
    private com.vaadin.flow.component.html.Div createPatientInfoCard() {
        Patient patient = current.getPatient();
        com.vaadin.flow.component.html.Div card = new com.vaadin.flow.component.html.Div();
        card.addClassName("patient-info-card");
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("padding", "var(--lumo-space-m)");
        card.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        // Titel
        com.vaadin.flow.component.html.H3 title = new com.vaadin.flow.component.html.H3("Patientendaten");
        title.getStyle().set("margin-top", "0");
        title.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        card.add(title);
        
        // Patientendaten in strukturierter Form
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setSpacing(false);
        infoLayout.setPadding(false);
        
        // Name
        if (patient.getFirstName() != null || patient.getLastName() != null) {
            String name = (patient.getLastName() != null ? patient.getLastName() : "") 
                    + (patient.getFirstName() != null ? ", " + patient.getFirstName() : "");
            if (!name.isEmpty()) {
                infoLayout.add(createInfoRow("Name", name));
            }
        }
        
        // Geburtsdatum
        if (patient.getBirth() != null) {
            String birthDate = patient.getBirth().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", java.util.Locale.GERMAN));
            infoLayout.add(createInfoRow("Geburtsdatum", birthDate));
        }
        
        // Adresse
        if (patient.getAddress() != null) {
            String address = patient.getAddress().toString();
            if (address != null && !address.trim().isEmpty()) {
                infoLayout.add(createInfoRow("Adresse", address));
            }
        }
        
        // Krankenkasse
        if (patient.getHealthInsurance() != null) {
            String insuranceName = patient.getHealthInsurance().getBillingCarrierName();
            if (insuranceName != null && !insuranceName.trim().isEmpty()) {
                infoLayout.add(createInfoRow("Krankenkasse", insuranceName));
            }
        }
        
        card.add(infoLayout);
        return card;
    }
    
    private HorizontalLayout createInfoRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();
        row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        
        com.vaadin.flow.component.html.Span labelSpan = new com.vaadin.flow.component.html.Span(label + ":");
        labelSpan.getStyle().set("font-weight", "600");
        labelSpan.getStyle().set("min-width", "120px");
        labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        com.vaadin.flow.component.html.Span valueSpan = new com.vaadin.flow.component.html.Span(value);
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");
        
        row.add(labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }
    
    /**
     * Überladung für createInfoRow mit nur einem Parameter (für Info-Kachel).
     */
    private HorizontalLayout createInfoRow(String text) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setWidthFull();
        row.setPadding(false);
        
        com.vaadin.flow.component.html.Span span = new com.vaadin.flow.component.html.Span(text);
        span.getStyle().set("font-size", "var(--lumo-font-size-s)");
        row.add(span);
        
        return row;
    }

    private void initializeTimeLineLeftEye(VerticalLayout timeLineLayout) {
        // linkes Auge mit medizinisch korrekter Darstellung (links = rechts vom Patienten)
        timeLineViewLeftEye.setTimelineHeight("300px"); // Feste Höhe für gleichmäßige Darstellung
        timeLineViewLeftEye.addClassName("left-eye-timeline");
        timeLineViewLeftEye.getStyle().set("background-color", "#FFF3E0"); // Orange tint
        timeLineViewLeftEye.setWidthFull();
        timeLineViewLeftEye.setHeightFull();
        timeLineLayout.add(timeLineViewLeftEye);
        if (timeLineLayout instanceof VerticalLayout) {
            ((VerticalLayout) timeLineLayout).expand(timeLineViewLeftEye);
        }
        setLeftEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setLeftEyeTreatmentHistory(Long treatmentPlanId) {
        setLeftEyeTreatmentHistory(treatmentPlanId, null);
    }
    
    private void setLeftEyeTreatmentHistory(Long treatmentPlanId, List<Treatment> allTreatments) {
        List<TimeLineCardConfig> leftEyeTreatments = new ArrayList<>();
        
        if (treatmentPlanId != null) {
            List<Treatment> treatments;
            if (allTreatments != null) {
                // Verwende bereits geladene Treatments statt neue Query
                treatments = allTreatments.stream()
                    .filter(t -> SideOfEye.LEFT.equals(t.getSideOfEye()))
                    .collect(java.util.stream.Collectors.toList());
            } else {
                // Fallback: Lade Treatments wenn nicht vorhanden
                treatments = presenter.getTreatmentDtos(SideOfEye.LEFT, treatmentPlanId);
            }
            
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                leftEyeTreatments.add(config);
            }
        }
        
        // Keine Startkachel mehr - wird jetzt außerhalb der Timeline angezeigt
        
        // Callback nach dem Löschen: Timeline neu laden
        timeLineViewLeftEye.setOnTreatmentDeletedCallback(() -> {
            if (current != null && current.getId() != null) {
                setLeftEyeTreatmentHistory(current.getId());
            }
        });
        
        // Auch bei null (neuer Plan) initialisieren
        timeLineViewLeftEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewLeftEye.setItems(leftEyeTreatments);
        
        // Aktualisiere Info-Kachel und Terminbuchung
        updateTimelineInfoAndBooking(SideOfEye.LEFT);
    }

    public TreatmentPlan getCurrent() {
        TreatmentPlan dto = binder.getBean();
        dto.getTreatments().addAll(getTimeSlotsToCreate());
        dto.setPatient(patientSelectComboBox.getValue());
        return dto;
    }

    public List<Treatment> getTimeSlotsToCreate() {
        List<Treatment> timeSlotsToCreate = new ArrayList<>();
        Set<SurgicalCenterTimeSlot> selectedSlots = timeSlotGrid.getSelectedItems();
        for (SurgicalCenterTimeSlot timeSlot : selectedSlots) {
            Treatment timeSlotToCreate = new Treatment();
            timeSlotToCreate.setSideOfEye(sideOfEye.getValue());
            timeSlotToCreate.setMedicationFavourite(medicationComboBox.getValue());
            timeSlotToCreate.setSurgicalCenterTimeSlot(timeSlot);
            timeSlotToCreate.setTreatmentPlan(current);
            timeSlotsToCreate.add(timeSlotToCreate);
        }
        return timeSlotsToCreate;
    }

    /**
     * Erstellt eine Section (wie im PatientDialog) statt Accordion.
     */
    private com.vaadin.flow.component.html.Div createSection(String title) {
        com.vaadin.flow.component.html.Div section = new com.vaadin.flow.component.html.Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        // Flexbox für Höhenanpassung
        section.getStyle().set("display", "flex");
        section.getStyle().set("flex-direction", "column");
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        section.getStyle().set("padding", "var(--lumo-space-m)");
        section.getStyle().set("box-sizing", "border-box");
        section.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        com.vaadin.flow.component.html.H4 sectionTitle = new com.vaadin.flow.component.html.H4(title);
        sectionTitle.getStyle().set("margin-top", "0");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
        sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        sectionTitle.getStyle().set("font-weight", "600");
        sectionTitle.getStyle().set("flex-shrink", "0"); // Titel soll nicht schrumpfen
        section.add(sectionTitle);
        
        return section;
    }
    
    /**
     * Prüft, ob Änderungen am Behandlungsplan vorgenommen wurden.
     * Berücksichtigt sowohl Binder-Änderungen als auch neue Treatments (z.B. gebuchte Folgetermine).
     */
    public boolean hasChanges() {
        if (binder == null || current == null) {
            return false;
        }
        
        // Prüfe ob Binder Änderungen hat
        boolean binderHasChanges = binder.hasChanges();
        
        // Prüfe ob neue Treatments gebucht wurden (z.B. Folgetermine)
        int currentTreatmentCount = 0;
        if (current.getId() != null) {
            try {
                // Lade aktuelle Treatments aus der DB
                List<Treatment> leftTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
                List<Treatment> rightTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
                currentTreatmentCount = leftTreatments.size() + rightTreatments.size();
            } catch (Exception e) {
                // Bei Fehler: Binder-Änderungen als Indikator verwenden
                return binderHasChanges;
            }
        }
        
        boolean treatmentsChanged = currentTreatmentCount != initialTreatmentCount;
        
        return binderHasChanges || treatmentsChanged;
    }

    public void setCurrent(TreatmentPlan newCurrent) {
        this.current = newCurrent;
        binder.setBean(newCurrent);
        if (newCurrent.getPatient() != null) {
            patientSelectComboBox.setReadOnly(true);
        }
        
        // Speichere initiale Anzahl der Treatments - verwende bereits geladene Treatments aus TreatmentPlan
        if (newCurrent.getId() != null && newCurrent.getTreatments() != null) {
            // Verwende bereits geladene Treatments statt neue Query
            List<Treatment> allTreatments = newCurrent.getTreatments();
            initialTreatmentCount = allTreatments.size();
        } else {
            initialTreatmentCount = 0;
        }
        
        // Verwende bereits geladene Treatments statt neue Queries
        setLeftEyeTreatmentHistory(newCurrent.getId(), newCurrent.getTreatments());
        setRightEyeTreatmentHistory(newCurrent.getId(), newCurrent.getTreatments());
        
        // Aktualisiere Übersicht-Section mit Patientendaten
        updateOverviewSection();
        
        // Aktualisiere Details-Section (Patient-Combobox Sichtbarkeit)
        updateDetailsSection();
        
        // Grid-Ansicht wurde entfernt
        
        // Refresh timeline display if orientation toggle exists
        // Note: This will be called after the layout is already built, so we need to update it
        // The accordions will be re-added by updateTimelineLayout if needed
    }
    
    /**
     * Aktualisiert die Übersicht-Section mit Patientendaten.
     * Wird live bei Patientenauswahl aufgerufen.
     */
    private void updateOverviewSection() {
        if (overviewSectionDiv == null) {
            return;
        }
        
        // Entferne alle Kinder außer dem Titel (Index 0 = H4 Titel)
        List<com.vaadin.flow.component.Component> childrenToRemove = new ArrayList<>();
        for (int i = 1; i < overviewSectionDiv.getComponentCount(); i++) {
            childrenToRemove.add(overviewSectionDiv.getComponentAt(i));
        }
        childrenToRemove.forEach(overviewSectionDiv::remove);
        
        // Füge Patientendaten-Card hinzu, falls Patient vorhanden
        Patient patient = null;
        if (current != null && current.getPatient() != null) {
            patient = current.getPatient();
        } else if (binder.getBean() != null && binder.getBean().getPatient() != null) {
            patient = binder.getBean().getPatient();
        } else if (patientSelectComboBox.getValue() != null) {
            patient = patientSelectComboBox.getValue();
        }
        
        if (patient != null) {
            // Temporär current setzen für createPatientInfoCard
            Patient originalPatient = current != null ? current.getPatient() : null;
            if (current == null) {
                current = new TreatmentPlan();
            }
            current.setPatient(patient);
            
            com.vaadin.flow.component.html.Div patientCard = createPatientInfoCard();
            // Entferne den Titel aus der Card, da er bereits in der Section ist
            patientCard.getChildren()
                .filter(c -> c instanceof com.vaadin.flow.component.html.H3)
                .findFirst()
                .ifPresent(patientCard::remove);
            
            overviewSectionDiv.add(patientCard);
            
            // Original wiederherstellen
            if (originalPatient == null && current.getId() == null) {
                current = null;
            } else if (current != null) {
                current.setPatient(originalPatient);
            }
        }
        
        // Nächste Termine werden jetzt in der TimelineView-Übersicht angezeigt, nicht mehr hier
        
        // Button "Terminübersicht drucken" hinzufügen
        if (current != null && current.getId() != null && current.getId() != -1) {
            Button exportButton = new Button("Terminübersicht drucken", VaadinIcon.PRINT.create());
            exportButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            exportButton.setWidthFull();
            exportButton.addClickListener(e -> generateAppointmentReport());
            exportButton.getStyle().set("margin-top", "var(--lumo-space-m)");
            overviewSectionDiv.add(exportButton);
        }
        
        // Button "Behandlungsplan abschließen" hinzufügen
        if (current != null && current.getId() != null && current.getId() != -1) {
            // Prüfe, ob bereits abgeschlossen
            if (current.getFinishedDate() != null) {
                com.vaadin.flow.component.html.Span statusText = new com.vaadin.flow.component.html.Span(
                    "Dieser Behandlungsplan wurde am " + 
                    current.getFinishedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)) + 
                    " abgeschlossen.");
                statusText.getStyle().set("color", "var(--lumo-secondary-text-color)");
                statusText.getStyle().set("margin-top", "var(--lumo-space-m)");
                overviewSectionDiv.add(statusText);
            } else {
                // Prüfe, ob noch zukünftige Termine anstehen
                ensureInstitutionContext();
                LocalDate now = LocalDate.now();
                List<Treatment> leftEyeTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
                List<Treatment> rightEyeTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
                
                boolean hasFutureTreatments = leftEyeTreatments.stream()
                        .anyMatch(t -> t.getDate() != null && t.getDate().isAfter(now)) ||
                        rightEyeTreatments.stream()
                        .anyMatch(t -> t.getDate() != null && t.getDate().isAfter(now));
                
                if (hasFutureTreatments) {
                    com.vaadin.flow.component.html.Span infoText = new com.vaadin.flow.component.html.Span(
                        "Der Behandlungsplan kann erst abgeschlossen werden, wenn keine zukünftigen Termine mehr anstehen.");
                    infoText.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    infoText.getStyle().set("margin-top", "var(--lumo-space-m)");
                    overviewSectionDiv.add(infoText);
                } else {
                    // Button "Behandlungsplan abschließen" hinzufügen
                    Button finishButton = new Button("Behandlungsplan abschließen", VaadinIcon.CHECK_CIRCLE.create());
                    finishButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    finishButton.setWidthFull();
                    finishButton.getStyle().set("margin-top", "var(--lumo-space-m)");
                    finishButton.addClickListener(e -> {
                        com.vaadin.flow.component.dialog.Dialog confirmDialog = new com.vaadin.flow.component.dialog.Dialog();
                        confirmDialog.setHeaderTitle("Behandlungsplan abschließen");
                        
                        com.vaadin.flow.component.html.Span message = new com.vaadin.flow.component.html.Span(
                            "Möchten Sie diesen Behandlungsplan wirklich abschließen? " +
                            "Dies kann nicht rückgängig gemacht werden.");
                        confirmDialog.add(message);
                        
                        Button confirmButton = new Button("Abschließen", VaadinIcon.CHECK.create());
                        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
                        confirmButton.addClickListener(e2 -> {
                            try {
                                ensureInstitutionContext();
                                presenter.finishTreatmentPlan(current.getId());
                                Notification.show("Behandlungsplan wurde erfolgreich abgeschlossen.", 3000,
                                        Notification.Position.BOTTOM_CENTER);
                                confirmDialog.close();
                                // Lade Behandlungsplan neu, um finishedDate zu aktualisieren
                                current = presenter.getByIdWithFullDetails(current.getId());
                                setCurrent(current);
                                updateOverviewSection();
                            } catch (Exception ex) {
                                Notification.show("Fehler beim Abschließen: " + ex.getMessage(), 5000,
                                        Notification.Position.MIDDLE);
                            }
                        });
                        
                        Button cancelButton = new Button("Abbrechen", e3 -> confirmDialog.close());
                        confirmDialog.getFooter().add(cancelButton, confirmButton);
                        confirmDialog.open();
                    });
                    
                    overviewSectionDiv.add(finishButton);
                }
            }
        }
    }
    
    /**
     * Aktualisiert die Patientendaten-Card in der Section "Allgemein".
     * @deprecated Wird nicht mehr verwendet, da Patientendaten jetzt in "Übersicht" sind
     */
    @Deprecated
    private void updatePatientInfoCard() {
        // Nicht mehr verwendet - Patientendaten sind jetzt in "Übersicht"
    }

    private void openNextTreatmentBookingDialog(SideOfEye sideOfEye) {
        if (current == null || current.getId() == null) {
            com.vaadin.flow.component.notification.Notification.show(
                    "Bitte speichern Sie zuerst den Behandlungsplan.", 3000,
                    com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
            return;
        }

        NextTreatmentBookingDialog dialog = new NextTreatmentBookingDialog(
                current, sideOfEye, context, presenter, treatment -> {
                    // UI-Updates müssen im UI-Thread passieren
                    com.vaadin.flow.component.UI.getCurrent().access(() -> {
                        // Nach erfolgreicher Buchung: Timeline aktualisieren
                        setLeftEyeTreatmentHistory(current.getId());
                        setRightEyeTreatmentHistory(current.getId());
                        // Aktualisiere Section "Termine buchen"
                        updateOverviewSection();
                        // Benachrichtige Binder-Change-Listener, damit Speichern-Button enabled wird
                        if (binderChangeListener != null) {
                            binderChangeListener.run();
                        }
                    });
                });
        dialog.open();
    }

    private void handleQuickBooking(TimelineView.QuickBookingRequest request) {
        if (request == null) {
            return;
        }
        if (current == null || current.getId() == null) {
            Notification notification = Notification.show(
                    "Bitte speichern Sie den Behandlungsplan, bevor Sie Folgetermine buchen.",
                    4000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        ensureInstitutionContext();
        try {
            executeQuickBooking(request);
            String eyeLabel = request.sideOfEye() == SideOfEye.LEFT ? "linkes Auge" : "rechtes Auge";
            Notification success = Notification.show(
                    "Folgetermin für das " + eyeLabel + " wurde gebucht.",
                    3000,
                    Notification.Position.BOTTOM_END);
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            if (current.getId() != null) {
                setLeftEyeTreatmentHistory(current.getId());
                setRightEyeTreatmentHistory(current.getId());
            }
            updateOverviewSection();
            if (binderChangeListener != null) {
                binderChangeListener.run();
            }
        } catch (IllegalStateException ex) {
            Notification notification = Notification.show(
                    ex.getMessage(),
                    4000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification notification = Notification.show(
                    "Folgetermin konnte nicht gebucht werden: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void executeQuickBooking(TimelineView.QuickBookingRequest request) {
        if (request.sideOfEye() == null) {
            throw new IllegalStateException("Kein Auge für die Schnellbuchung ausgewählt.");
        }

        List<Treatment> treatments = presenter.getTreatmentDtos(request.sideOfEye(), current.getId());
        treatments.sort(this::compareTreatmentsByDate);
        
        Treatment lastTreatment = treatments.isEmpty() ? null : treatments.get(treatments.size() - 1);
        Treatment previousTreatment = treatments.size() > 1 ? treatments.get(treatments.size() - 2) : null;

        int previousIntervalWeeks = calculatePreviousIntervalWeeks(previousTreatment, lastTreatment);
        int targetIntervalWeeks = determineTargetIntervalWeeks(request, previousIntervalWeeks);

        SurgicalCenterTimeSlot selectedSlot = findQuickBookingSlot(lastTreatment, previousIntervalWeeks, request, targetIntervalWeeks);
        if (selectedSlot == null) {
            throw new IllegalStateException("Kein verfügbarer Termin gefunden.");
        }
        Treatment newTreatment = cloneTreatmentForQuickBooking(lastTreatment, selectedSlot, request.sideOfEye());

        presenter.save(current.getId(), List.of(newTreatment));
    }

    private int compareTreatmentsByDate(Treatment a, Treatment b) {
        LocalDate dateA = a != null ? a.getDate() : null;
        LocalDate dateB = b != null ? b.getDate() : null;
        if (dateA == null && dateB == null) {
            return 0;
        }
        if (dateA == null) {
            return 1;
        }
        if (dateB == null) {
            return -1;
        }
        return dateA.compareTo(dateB);
    }

    private int calculatePreviousIntervalWeeks(Treatment previous, Treatment last) {
        if (previous == null || previous.getDate() == null || last == null || last.getDate() == null) {
            return DEFAULT_INTERVAL_WEEKS;
        }
        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(previous.getDate(), last.getDate());
        if (weeks <= 0) {
            weeks = DEFAULT_INTERVAL_WEEKS;
        }
        return clampInterval((int) weeks);
    }

    private int determineTargetIntervalWeeks(TimelineView.QuickBookingRequest request, int previousIntervalWeeks) {
        return switch (request.action()) {
            case SHORTER_INTERVAL -> clampInterval(previousIntervalWeeks - 1);
            case SAME_INTERVAL -> clampInterval(previousIntervalWeeks);
            case LONGER_INTERVAL -> clampInterval(previousIntervalWeeks + 1);
            case CUSTOM_INTERVAL -> clampInterval(
                    request.intervalWeeks() != null ? request.intervalWeeks() : previousIntervalWeeks);
            case NEXT_AVAILABLE -> clampInterval(previousIntervalWeeks);
        };
    }

    private int clampInterval(int interval) {
        return Math.max(MIN_INTERVAL_WEEKS, Math.min(MAX_INTERVAL_WEEKS, interval));
    }

    private SurgicalCenterTimeSlot findQuickBookingSlot(Treatment lastTreatment,
            int previousIntervalWeeks,
            TimelineView.QuickBookingRequest request,
            int targetIntervalWeeks) {
        Integer surgicalCenterId = null;
        LocalDate lastDate = null;
        
        if (lastTreatment != null && lastTreatment.getSurgicalCenterTimeSlot() != null
                && lastTreatment.getSurgicalCenterTimeSlot().getSurgicalCenter() != null
                && lastTreatment.getSurgicalCenterTimeSlot().getSurgicalCenter().getId() != null) {
            surgicalCenterId = lastTreatment.getSurgicalCenterTimeSlot().getSurgicalCenter().getId();
            lastDate = lastTreatment.getDate();
        }
        
        // Wenn kein Behandlungsort vorhanden, verwende den ersten verfügbaren
        if (surgicalCenterId == null) {
            // Lade alle verfügbaren Behandlungsorte und nimm den ersten
            List<de.bbajor.pvs.surgicalcenter.model.SurgicalCenter> centers = presenter.getSurgicalCenters();
            if (centers.isEmpty()) {
                throw new IllegalStateException("Kein Behandlungsort verfügbar.");
            }
            surgicalCenterId = centers.get(0).getId();
        }

        LocalDate earliestSearchDate = LocalDate.now();
        if (lastDate != null && lastDate.plusDays(1).isAfter(earliestSearchDate)) {
            earliestSearchDate = lastDate.plusDays(1);
        }

        Collection<SurgicalCenterTimeSlot> slotCollection = presenter.getAllTimeSlotsFilteredBy(
                earliestSearchDate,
                TimePeriod.ONE_YEAR,
                TimeSlotRepetition.WEEKLY,
                surgicalCenterId);

        List<SurgicalCenterTimeSlot> slots = slotCollection.stream()
                .filter(slot -> slot.getDate() != null)
                .sorted(this::compareTimeSlots)
                .collect(java.util.stream.Collectors.toList());

        if (slots.isEmpty()) {
            throw new IllegalStateException("Keine freien Termine im ausgewählten Behandlungsort gefunden.");
        }

        return switch (request.action()) {
            case NEXT_AVAILABLE -> slots.get(0);
            case SHORTER_INTERVAL -> selectSlotWithPreference(slots, lastDate, targetIntervalWeeks, previousIntervalWeeks, true);
            case LONGER_INTERVAL -> selectSlotWithPreference(slots, lastDate, targetIntervalWeeks, previousIntervalWeeks, false);
            case SAME_INTERVAL, CUSTOM_INTERVAL -> selectClosestSlot(slots, lastDate, targetIntervalWeeks);
        };
    }

    private int compareTimeSlots(SurgicalCenterTimeSlot a, SurgicalCenterTimeSlot b) {
        LocalDate dateA = a.getDate();
        LocalDate dateB = b.getDate();
        if (dateA == null && dateB == null) {
            return 0;
        }
        if (dateA == null) {
            return 1;
        }
        if (dateB == null) {
            return -1;
        }
        int dateCompare = dateA.compareTo(dateB);
        if (dateCompare != 0) {
            return dateCompare;
        }
        if (a.getStartTime() != null && b.getStartTime() != null) {
            return a.getStartTime().compareTo(b.getStartTime());
        }
        if (a.getStartTime() == null && b.getStartTime() != null) {
            return 1;
        }
        if (a.getStartTime() != null) {
            return -1;
        }
        return 0;
    }

    private SurgicalCenterTimeSlot selectSlotWithPreference(List<SurgicalCenterTimeSlot> slots,
            LocalDate lastDate,
            int targetIntervalWeeks,
            int previousIntervalWeeks,
            boolean preferShorter) {
        if (lastDate == null) {
            return slots.get(0);
        }
        long previousDays = (long) previousIntervalWeeks * 7;
        List<SurgicalCenterTimeSlot> filtered = slots.stream()
                .filter(slot -> !slot.getDate().isBefore(lastDate.plusDays(1)))
                .filter(slot -> {
                    long deltaDays = java.time.temporal.ChronoUnit.DAYS.between(lastDate, slot.getDate());
                    return preferShorter ? deltaDays < previousDays : deltaDays > previousDays;
                })
                .collect(java.util.stream.Collectors.toList());

        if (filtered.isEmpty()) {
            return selectClosestSlot(slots, lastDate, targetIntervalWeeks);
        }
        return selectClosestSlot(filtered, lastDate, targetIntervalWeeks);
    }

    private SurgicalCenterTimeSlot selectClosestSlot(List<SurgicalCenterTimeSlot> slots,
            LocalDate lastDate,
            int targetIntervalWeeks) {
        if (slots.isEmpty()) {
            return null;
        }
        if (lastDate == null) {
            return slots.get(0);
        }
        long targetDays = (long) targetIntervalWeeks * 7;
        return slots.stream()
                .filter(slot -> !slot.getDate().isBefore(lastDate.plusDays(1)))
                .min(Comparator.<SurgicalCenterTimeSlot>comparingLong(slot -> Math.abs(
                                java.time.temporal.ChronoUnit.DAYS.between(lastDate, slot.getDate()) - targetDays))
                        .thenComparing(SurgicalCenterTimeSlot::getDate)
                        .thenComparing(slot -> slot.getStartTime()))
                .orElse(slots.get(0));
    }

    private Treatment cloneTreatmentForQuickBooking(Treatment source,
            SurgicalCenterTimeSlot selectedSlot,
            SideOfEye sideOfEye) {
        if (selectedSlot == null) {
            throw new IllegalStateException("Kein verfügbarer Termin gefunden.");
        }
        Treatment newTreatment = new Treatment();
        newTreatment.setTreatmentPlan(current);
        newTreatment.setSideOfEye(sideOfEye);
        newTreatment.setSurgicalCenterTimeSlot(selectedSlot);
        
        // Wenn source vorhanden ist, kopiere die Werte, sonst bleiben sie null/leer
        if (source != null) {
            newTreatment.setMedicationFavourite(source.getMedicationFavourite());
            newTreatment.setFrequency(source.getFrequency());
            newTreatment.setDosage(source.getDosage());
            newTreatment.setAdditionalInfo(source.getAdditionalInfo());
            if (source.getTreatingDoctors() != null && !source.getTreatingDoctors().isEmpty()) {
                newTreatment.setTreatingDoctors(new java.util.HashSet<>(source.getTreatingDoctors()));
            }
        }
        return newTreatment;
    }
    
    /**
     * Setzt den Binder zurück, damit hasChanges() false wird.
     * Aktualisiert auch die initiale Treatment-Anzahl.
     */
    public void resetBinder() {
        if (binder != null && current != null) {
            binder.readBean(current);
            // Aktualisiere initiale Treatment-Anzahl nach dem Speichern
            if (current.getId() != null) {
                try {
                    List<Treatment> leftTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
                    List<Treatment> rightTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
                    initialTreatmentCount = leftTreatments.size() + rightTreatments.size();
                } catch (Exception e) {
                    // Bei Fehler: initialTreatmentCount bleibt unverändert
                }
            }
        }
    }
    
    /**
     * Setzt einen Listener, der aufgerufen wird, wenn sich der Binder ändert.
     */
    public void setBinderChangeListener(Runnable listener) {
        this.binderChangeListener = listener;
    }

    private Integer calculateMostCommonInterval(List<Treatment> treatments) {
        if (treatments == null || treatments.size() < 2) {
            return null;
        }

        // Sortiere Behandlungen nach Datum
        List<Treatment> sorted = new ArrayList<>(treatments);
        sorted.sort((a, b) -> {
            LocalDate dateA = a.getDate();
            LocalDate dateB = b.getDate();
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateA.compareTo(dateB);
        });

        // Berechne Intervalle zwischen aufeinanderfolgenden Behandlungen
        java.util.Map<Integer, Integer> intervalCounts = new java.util.HashMap<>();
        for (int i = 1; i < sorted.size(); i++) {
            LocalDate prevDate = sorted.get(i - 1).getDate();
            LocalDate currDate = sorted.get(i).getDate();
            if (prevDate != null && currDate != null) {
                long weeks = java.time.temporal.ChronoUnit.WEEKS.between(prevDate, currDate);
                if (weeks > 0 && weeks <= 16) {
                    int weeksInt = (int) weeks;
                    intervalCounts.put(weeksInt, intervalCounts.getOrDefault(weeksInt, 0) + 1);
                }
            }
        }

        // Finde das häufigste Intervall
        return intervalCounts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }
}
