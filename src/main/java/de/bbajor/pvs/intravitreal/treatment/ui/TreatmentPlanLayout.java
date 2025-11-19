package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
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
    private VerticalLayout generalSection;
    private VerticalLayout treatmentHistorySection;
    private VerticalLayout appointmentBookingSection;
    
    // View-Toggle und Container
    private RadioButtonGroup<String> viewToggle;
    private VerticalLayout timelineContainer;
    private VerticalLayout gridContainer;
    private boolean showPastTreatments = true;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter, TreatmentPlan treatmentPlan,
            ApplicationContext context) {
        this.presenter = presenter;
        this.current = treatmentPlan;
        this.context = context;

        setSizeFull();
        // overflow entfernt - erlaube Scrollen wenn nötig
        
        timeLineViewLeftEye = new TimelineView(context);
        timeLineViewRightEye = new TimelineView(context);

        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new TreatmentPlan());
                }
                binder.getBean().setPatient(event.getValue());
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
        initializeGeneralSection();
        initializeAppointmentBookingSection();
        initializeTreatmentHistorySection();
        
        // Layout: Allgemein und Termine buchen nebeneinander
        HorizontalLayout topSectionLayout = new HorizontalLayout();
        topSectionLayout.setSizeFull();
        topSectionLayout.setSpacing(true);
        topSectionLayout.add(generalSection);
        topSectionLayout.add(appointmentBookingSection);
        topSectionLayout.setFlexGrow(1, generalSection, appointmentBookingSection);
        
        add(topSectionLayout);
        add(treatmentHistorySection);
    }

    private void initializeGeneralSection() {
        generalSection = new VerticalLayout();
        generalSection.setSpacing(true);
        generalSection.setPadding(true);
        generalSection.setWidthFull();
        
        // Section-Titel
        com.vaadin.flow.component.html.H3 sectionTitle = new com.vaadin.flow.component.html.H3("Allgemein");
        sectionTitle.getStyle().set("margin-top", "0");
        generalSection.add(sectionTitle);
        
        // Patientendaten-Block (immer sichtbar, wenn Patient vorhanden)
        if (current != null && current.getPatient() != null) {
            generalSection.add(createPatientInfoCard());
        }
        
        // Formular für Behandlungsplan-Details
        FormLayout formLayout = new FormLayout();
        formLayout.add(creationDatePicker);
        formLayout.add(patientSelectComboBox);
        formLayout.add(reasonForTreatmentComboBox, 2);
        formLayout.add(additionalInformation, 2);
        
        AccordionPanel generalDetailsPanel = new AccordionPanel();
        Accordion generalDetails = new Accordion();
        generalDetailsPanel.add(formLayout);
        generalDetailsPanel.setOpened(true);
        generalDetails.add(generalDetailsPanel);
        
        generalSection.add(generalDetails);
        generalSection.expand(generalDetails);
    }
    
    private void initializeTreatmentHistorySection() {
        treatmentHistorySection = new VerticalLayout();
        treatmentHistorySection.setSizeFull();
        treatmentHistorySection.setPadding(false);
        treatmentHistorySection.setSpacing(false);
        treatmentHistorySection.setWidthFull();
        
        // Section-Titel
        com.vaadin.flow.component.html.H3 sectionTitle = new com.vaadin.flow.component.html.H3("Behandlungsverlauf");
        sectionTitle.getStyle().set("margin-top", "0");
        treatmentHistorySection.add(sectionTitle);
        
        // Export-Button für Patienten-Ausdruck
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        
        Button exportButton = new Button("Terminübersicht drucken", new Icon(VaadinIcon.PRINT));
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(e -> generateAppointmentReport());
        buttonLayout.add(exportButton);
        treatmentHistorySection.add(buttonLayout);
        
        // View-Toggle: Timeline oder Grid-Ansicht
        viewToggle = new RadioButtonGroup<>();
        viewToggle.setLabel("Ansicht");
        viewToggle.setItems("Timeline", "Grid");
        viewToggle.setValue("Timeline");
        viewToggle.setItemLabelGenerator(item -> item);
        viewToggle.addValueChangeListener(e -> {
            String view = e.getValue();
            if ("Timeline".equals(view)) {
                showTimelineView();
            } else {
                showGridView();
            }
        });
        treatmentHistorySection.add(viewToggle);
        
        // Container für Timeline-Ansicht
        timelineContainer = new VerticalLayout();
        timelineContainer.setSizeFull();
        timelineContainer.setPadding(false);
        timelineContainer.setSpacing(false);
        timeLineViewLeftEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        timeLineViewRightEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        updateTimelineLayout(timelineContainer, TimelineView.Orientation.HORIZONTAL);
        
        // Container für Grid-Ansicht
        gridContainer = new VerticalLayout();
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.setVisible(false);
        initializeGridView();
        
        treatmentHistorySection.add(timelineContainer);
        treatmentHistorySection.add(gridContainer);
        treatmentHistorySection.expand(timelineContainer);
    }
    
    private void showTimelineView() {
        timelineContainer.setVisible(true);
        gridContainer.setVisible(false);
        treatmentHistorySection.expand(timelineContainer);
    }
    
    private void showGridView() {
        timelineContainer.setVisible(false);
        gridContainer.setVisible(true);
        treatmentHistorySection.expand(gridContainer);
        refreshGrids();
    }
    
    private Grid<Treatment> leftEyeGrid;
    private Grid<Treatment> rightEyeGrid;
    private Button togglePastTreatmentsButton;
    
    private void initializeGridView() {
        gridContainer.removeAll();
        
        // Toggle für vergangene Termine
        HorizontalLayout toggleLayout = new HorizontalLayout();
        toggleLayout.setWidthFull();
        toggleLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.START);
        togglePastTreatmentsButton = new Button("Vergangene Termine ausblenden", e -> {
            showPastTreatments = !showPastTreatments;
            togglePastTreatmentsButton.setText(showPastTreatments ? "Vergangene Termine ausblenden" : "Vergangene Termine einblenden");
            refreshGrids();
        });
        toggleLayout.add(togglePastTreatmentsButton);
        gridContainer.add(toggleLayout);
        
        // Horizontal Layout für beide Grids
        HorizontalLayout gridsLayout = new HorizontalLayout();
        gridsLayout.setSizeFull();
        gridsLayout.setSpacing(true);
        gridsLayout.setPadding(false);
        
        // Linkes Auge (OS)
        VerticalLayout leftEyeLayout = new VerticalLayout();
        leftEyeLayout.setSizeFull();
        leftEyeLayout.setSpacing(false);
        leftEyeLayout.setPadding(false);
        
        com.vaadin.flow.component.html.H4 leftEyeTitle = new com.vaadin.flow.component.html.H4("Linkes Auge (OS)");
        leftEyeLayout.add(leftEyeTitle);
        
        // Übersicht für linkes Auge
        leftEyeLayout.add(createEyeOverview(SideOfEye.LEFT));
        
        leftEyeGrid = createTreatmentGrid(SideOfEye.LEFT);
        leftEyeLayout.add(leftEyeGrid);
        leftEyeLayout.expand(leftEyeGrid);
        
        // Rechtes Auge (OD)
        VerticalLayout rightEyeLayout = new VerticalLayout();
        rightEyeLayout.setSizeFull();
        rightEyeLayout.setSpacing(false);
        rightEyeLayout.setPadding(false);
        
        com.vaadin.flow.component.html.H4 rightEyeTitle = new com.vaadin.flow.component.html.H4("Rechtes Auge (OD)");
        rightEyeLayout.add(rightEyeTitle);
        
        // Übersicht für rechtes Auge
        rightEyeLayout.add(createEyeOverview(SideOfEye.RIGHT));
        
        rightEyeGrid = createTreatmentGrid(SideOfEye.RIGHT);
        rightEyeLayout.add(rightEyeGrid);
        rightEyeLayout.expand(rightEyeGrid);
        
        gridsLayout.add(leftEyeLayout, rightEyeLayout);
        gridsLayout.setFlexGrow(1, leftEyeLayout, rightEyeGrid);
        
        gridContainer.add(gridsLayout);
        gridContainer.expand(gridsLayout);
    }
    
    private com.vaadin.flow.component.html.Div createEyeOverview(SideOfEye side) {
        com.vaadin.flow.component.html.Div overview = new com.vaadin.flow.component.html.Div();
        overview.getStyle().set("padding", "var(--lumo-space-m)");
        overview.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        overview.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        overview.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        
        if (current == null || current.getId() == null || current.getId() == -1) {
            com.vaadin.flow.component.html.Span noData = new com.vaadin.flow.component.html.Span("Keine Daten verfügbar");
            overview.add(noData);
            return overview;
        }
        
        ensureInstitutionContext();
        List<Treatment> treatments = presenter.getTreatmentDtos(side, current.getId());
        LocalDate now = LocalDate.now();
        LocalDate oneYearAgo = now.minusYears(1);
        
        // In Behandlung seit
        LocalDate startDate = current.getCreationDate() != null ? current.getCreationDate() : now;
        String inTreatmentSince = "In Behandlung seit: " + startDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN));
        
        // Anzahl bisheriger Behandlungen
        long treatmentCount = treatments.size();
        String treatmentCountStr = "Anzahl bisheriger Behandlungen: " + treatmentCount;
        
        // Weitere Behandlung geplant
        boolean hasFutureTreatment = treatments.stream()
            .anyMatch(t -> t.getDate() != null && t.getDate().isAfter(now));
        String futureTreatmentStr = "Weitere Behandlung geplant: " + (hasFutureTreatment ? "Ja" : "Nein");
        
        // Meiste Zeitintervalle (letztes Jahr)
        List<Treatment> lastYearTreatments = treatments.stream()
            .filter(t -> t.getDate() != null && !t.getDate().isBefore(oneYearAgo) && !t.getDate().isAfter(now))
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(java.util.stream.Collectors.toList());
        
        String intervalStr = "Meiste Zeitintervalle (letztes Jahr): ";
        if (lastYearTreatments.size() < 2) {
            intervalStr += "Nicht verfügbar";
        } else {
            java.util.Map<Integer, Integer> intervalCounts = new java.util.HashMap<>();
            for (int i = 1; i < lastYearTreatments.size(); i++) {
                LocalDate prevDate = lastYearTreatments.get(i - 1).getDate();
                LocalDate currDate = lastYearTreatments.get(i).getDate();
                if (prevDate != null && currDate != null) {
                    long weeks = java.time.temporal.ChronoUnit.WEEKS.between(prevDate, currDate);
                    if (weeks > 0 && weeks <= 16) {
                        int weeksInt = (int) weeks;
                        intervalCounts.put(weeksInt, intervalCounts.getOrDefault(weeksInt, 0) + 1);
                    }
                }
            }
            Integer mostCommonInterval = intervalCounts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
            if (mostCommonInterval != null) {
                intervalStr += mostCommonInterval + " Wochen";
            } else {
                intervalStr += "Nicht verfügbar";
            }
        }
        
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setSpacing(false);
        infoLayout.setPadding(false);
        
        infoLayout.add(createInfoRow(inTreatmentSince));
        infoLayout.add(createInfoRow(treatmentCountStr));
        infoLayout.add(createInfoRow(futureTreatmentStr));
        infoLayout.add(createInfoRow(intervalStr));
        
        overview.add(infoLayout);
        return overview;
    }
    
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
    
    private Grid<Treatment> createTreatmentGrid(SideOfEye side) {
        Grid<Treatment> grid = new Grid<>(Treatment.class, false);
        grid.setSizeFull();
        grid.setPageSize(20);
        grid.setHeight("600px"); // Mindesthöhe für 20 Zeilen
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN);
        
        grid.addColumn(t -> {
            if (t.getDate() != null) {
                return t.getDate().format(dateFormatter);
            }
            return "-";
        }).setHeader("Datum").setAutoWidth(true).setResizable(true);
        
        grid.addColumn(t -> {
            if (t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getStartTime() != null) {
                String startTime = t.getSurgicalCenterTimeSlot().getStartTime().format(timeFormatter);
                if (t.getSurgicalCenterTimeSlot().getEndTime() != null) {
                    String endTime = t.getSurgicalCenterTimeSlot().getEndTime().format(timeFormatter);
                    return startTime + " - " + endTime;
                }
                return startTime;
            }
            return "-";
        }).setHeader("Uhrzeit").setAutoWidth(true).setResizable(true);
        
        grid.addColumn(t -> {
            if (t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getSurgicalCenter() != null) {
                return t.getSurgicalCenterTimeSlot().getSurgicalCenter().getName();
            }
            return "-";
        }).setHeader("Einrichtung").setAutoWidth(true).setResizable(true);
        
        grid.addColumn(t -> {
            if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament").setAutoWidth(true).setResizable(true);
        
        grid.addColumn(t -> {
            if (t.getApprovalDate() != null) {
                return "Genehmigt";
            }
            return "Offen";
        }).setHeader("Status").setAutoWidth(true).setResizable(true);
        
        // Row-Styling basierend auf Datum
        grid.setClassNameGenerator(treatment -> {
            if (treatment.getDate() == null) {
                return "treatment-past";
            }
            LocalDate now = LocalDate.now();
            if (treatment.getDate().isAfter(now)) {
                // Nächster Termin - grün markieren
                List<Treatment> allTreatments = presenter.getTreatmentDtos(side, current != null ? current.getId() : null);
                Treatment nextTreatment = allTreatments.stream()
                    .filter(t -> t.getDate() != null && t.getDate().isAfter(now))
                    .min((a, b) -> a.getDate().compareTo(b.getDate()))
                    .orElse(null);
                if (nextTreatment != null && nextTreatment.getId() != null && treatment.getId() != null 
                    && nextTreatment.getId().equals(treatment.getId())) {
                    return "treatment-next";
                }
                return "treatment-future";
            } else {
                // Vergangene Termine - grau abgestuft
                long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(treatment.getDate(), now);
                if (daysAgo <= 30) {
                    return "treatment-past-recent";
                } else if (daysAgo <= 90) {
                    return "treatment-past-medium";
                } else {
                    return "treatment-past-old";
                }
            }
        });
        
        // Klick auf Zeile öffnet Detailansicht
        grid.addItemClickListener(e -> {
            if (e.getItem() != null) {
                ensureInstitutionContext();
                TreatmentDetailDialog dialog = new TreatmentDetailDialog(
                    e.getItem(),
                    context.getBean(de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService.class),
                    context.getBean(de.bbajor.pvs.security.service.UserAccountService.class)
                );
                dialog.open();
            }
        });
        
        // CSS-Styles für Farbmarkierungen hinzufügen
        grid.getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = '" +
            ".treatment-next { background-color: #c8e6c9 !important; } " +
            ".treatment-future { background-color: #e8f5e9 !important; } " +
            ".treatment-past-recent { background-color: #f5f5f5 !important; } " +
            ".treatment-past-medium { background-color: #e0e0e0 !important; } " +
            ".treatment-past-old { background-color: #bdbdbd !important; } " +
            ".treatment-past { background-color: #9e9e9e !important; }" +
            "';" +
            "document.head.appendChild(style);"
        );
        
        return grid;
    }
    
    private void refreshGrids() {
        if (leftEyeGrid == null || rightEyeGrid == null || current == null || current.getId() == null) {
            return;
        }
        
        ensureInstitutionContext();
        LocalDate now = LocalDate.now();
        
        List<Treatment> leftTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
        List<Treatment> rightTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
        
        if (!showPastTreatments) {
            leftTreatments = leftTreatments.stream()
                .filter(t -> t.getDate() == null || !t.getDate().isBefore(now))
                .collect(java.util.stream.Collectors.toList());
            rightTreatments = rightTreatments.stream()
                .filter(t -> t.getDate() == null || !t.getDate().isBefore(now))
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Sortiere nach Datum (neueste zuerst)
        leftTreatments.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });
        rightTreatments.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });
        
        leftEyeGrid.setItems(leftTreatments);
        rightEyeGrid.setItems(rightTreatments);
        
        // Aktualisiere Übersichten
        if (gridContainer != null) {
            gridContainer.getChildren()
                .filter(child -> child instanceof HorizontalLayout)
                .findFirst()
                .ifPresent(hl -> {
                    HorizontalLayout gridsLayout = (HorizontalLayout) hl;
                    // Finde und aktualisiere Übersichten
                    gridsLayout.getChildren()
                        .filter(child -> child instanceof VerticalLayout)
                        .forEach(vl -> {
                            VerticalLayout eyeLayout = (VerticalLayout) vl;
                            // Ersetze Übersicht (Index 1, nach Titel)
                            if (eyeLayout.getComponentCount() > 1) {
                                com.vaadin.flow.component.Component oldOverview = eyeLayout.getComponentAt(1);
                                if (oldOverview instanceof com.vaadin.flow.component.html.Div) {
                                    eyeLayout.remove(oldOverview);
                                    SideOfEye side = eyeLayout.getComponentAt(0) instanceof com.vaadin.flow.component.html.H4
                                        && ((com.vaadin.flow.component.html.H4) eyeLayout.getComponentAt(0)).getText().contains("Links")
                                        ? SideOfEye.LEFT : SideOfEye.RIGHT;
                                    eyeLayout.addComponentAtIndex(1, createEyeOverview(side));
                                }
                            }
                        });
                });
        }
    }
    
    private void initializeAppointmentBookingSection() {
        appointmentBookingSection = new VerticalLayout();
        appointmentBookingSection.setSpacing(true);
        appointmentBookingSection.setPadding(true);
        appointmentBookingSection.setWidthFull();
        
        // Section-Titel
        com.vaadin.flow.component.html.H3 sectionTitle = new com.vaadin.flow.component.html.H3("Termine buchen");
        sectionTitle.getStyle().set("margin-top", "0");
        appointmentBookingSection.add(sectionTitle);
        
        // Status-Anzeige und Buttons
        updateAppointmentBookingSection();
    }
    
    private void updateAppointmentBookingSection() {
        if (appointmentBookingSection == null) {
            return; // Section noch nicht initialisiert
        }
        
        // Entferne alte Status-Anzeige und Buttons (falls vorhanden)
        appointmentBookingSection.getChildren()
            .filter(child -> child.getClass().getSimpleName().equals("Div") 
                || child instanceof Button 
                || child instanceof com.vaadin.flow.component.html.Span)
            .filter(child -> appointmentBookingSection.indexOf(child) > 0) // Behalte Titel
            .collect(java.util.stream.Collectors.toList())
            .forEach(appointmentBookingSection::remove);
        
        if (current == null || current.getId() == null || current.getId() == -1) {
            com.vaadin.flow.component.html.Span statusMessage = new com.vaadin.flow.component.html.Span(
                "Bitte speichern Sie zuerst den Behandlungsplan, um Termine zu buchen.");
            statusMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
            appointmentBookingSection.add(statusMessage);
            return;
        }
        
        ensureInstitutionContext();
        
        // Prüfe auf zukünftige Termine für beide Augen
        LocalDate now = LocalDate.now();
        List<Treatment> leftEyeTreatments = presenter.getTreatmentDtos(SideOfEye.LEFT, current.getId());
        List<Treatment> rightEyeTreatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, current.getId());
        
        Treatment nextLeftTreatment = leftEyeTreatments.stream()
            .filter(t -> t.getDate() != null && t.getDate().isAfter(now))
            .min((a, b) -> a.getDate().compareTo(b.getDate()))
            .orElse(null);
        
        Treatment nextRightTreatment = rightEyeTreatments.stream()
            .filter(t -> t.getDate() != null && t.getDate().isAfter(now))
            .min((a, b) -> a.getDate().compareTo(b.getDate()))
            .orElse(null);
        
        boolean hasFutureAppointment = nextLeftTreatment != null || nextRightTreatment != null;
        
        // Status-Anzeige
        com.vaadin.flow.component.html.Div statusDiv = new com.vaadin.flow.component.html.Div();
        statusDiv.getStyle().set("padding", "var(--lumo-space-m)");
        statusDiv.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        statusDiv.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        statusDiv.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        com.vaadin.flow.component.html.Span statusText = new com.vaadin.flow.component.html.Span();
        if (hasFutureAppointment) {
            StringBuilder statusBuilder = new StringBuilder("Folgetermin bereits gebucht:");
            if (nextLeftTreatment != null) {
                statusBuilder.append("\n• Linkes Auge (OS): ")
                    .append(nextLeftTreatment.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)));
            }
            if (nextRightTreatment != null) {
                statusBuilder.append("\n• Rechtes Auge (OD): ")
                    .append(nextRightTreatment.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)));
            }
            statusText.setText(statusBuilder.toString());
            statusText.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            statusText.setText("Kein künftiger Termin ansteht.");
            statusText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        }
        statusText.getStyle().set("white-space", "pre-line");
        statusDiv.add(statusText);
        appointmentBookingSection.add(statusDiv);
        
        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        
        if (!hasFutureAppointment) {
            // Button "Folgetermin buchen" - öffnet Dialog für beide Augen
            Button bookNextButton = new Button("Folgetermin buchen", VaadinIcon.CALENDAR.create());
            bookNextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            bookNextButton.addClickListener(e -> {
                // Zeige Dialog zur Auswahl des Auges
                com.vaadin.flow.component.dialog.Dialog eyeSelectionDialog = new com.vaadin.flow.component.dialog.Dialog();
                eyeSelectionDialog.setHeaderTitle("Auge auswählen");
                
                VerticalLayout dialogContent = new VerticalLayout();
                dialogContent.setSpacing(true);
                dialogContent.setPadding(true);
                
                com.vaadin.flow.component.html.Span question = new com.vaadin.flow.component.html.Span(
                    "Für welches Auge soll der Folgetermin gebucht werden?");
                dialogContent.add(question);
                
                Button leftEyeButton = new Button("Linkes Auge (OS)", e2 -> {
                    eyeSelectionDialog.close();
                    openNextTreatmentBookingDialog(SideOfEye.LEFT);
                });
                leftEyeButton.setWidthFull();
                
                Button rightEyeButton = new Button("Rechtes Auge (OD)", e2 -> {
                    eyeSelectionDialog.close();
                    openNextTreatmentBookingDialog(SideOfEye.RIGHT);
                });
                rightEyeButton.setWidthFull();
                
                dialogContent.add(leftEyeButton, rightEyeButton);
                eyeSelectionDialog.add(dialogContent);
                
                Button cancelButton = new Button("Abbrechen", e2 -> eyeSelectionDialog.close());
                eyeSelectionDialog.getFooter().add(cancelButton);
                
                eyeSelectionDialog.open();
            });
            buttonLayout.add(bookNextButton);
        }
        
        // Button "Terminserie planen" - öffnet Dialog mit Terminplanung
        Button planSeriesButton = new Button("Terminserie planen", VaadinIcon.CALENDAR_CLOCK.create());
        planSeriesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        planSeriesButton.addClickListener(e -> openAppointmentPlanningDialog());
        buttonLayout.add(planSeriesButton);
        
        appointmentBookingSection.add(buttonLayout);
    }
    
    private void openAppointmentPlanningDialog() {
        if (current == null || current.getId() == null || current.getId() == -1) {
            Notification.show("Bitte speichern Sie zuerst den Behandlungsplan.", 3000,
                    Notification.Position.MIDDLE);
            return;
        }
        
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Terminserie planen");
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
                updateAppointmentBookingSection();
                // Aktualisiere Grids falls Grid-Ansicht aktiv
                if (gridContainer != null && gridContainer.isVisible()) {
                    refreshGrids();
                }
                
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
    
    private void updateTimelineLayout(VerticalLayout timeLineLayout, TimelineView.Orientation orientation) {
        // Remove existing timeline accordions and eyesContainer
        // timelineContainer enthält nur die Timelines, keine Buttons oder Toggles
        timeLineLayout.getChildren()
                .filter(child -> {
                    String className = child.getClass().getSimpleName();
                    return className.equals("Accordion") || 
                           (className.equals("VerticalLayout") && 
                            child.getStyle().get("display") != null && 
                            child.getStyle().get("display").equals("flex"));
                })
                .collect(java.util.stream.Collectors.toList())
                .forEach(timeLineLayout::remove);
        
        // Immer Timeline anzeigen, auch wenn noch keine Behandlungen existieren
        // Beide Augen immer anzeigen (auch wenn noch keine Behandlungen existieren)
        
        if (orientation == TimelineView.Orientation.VERTICAL) {
            // Vertical: show eyes side by side
            VerticalLayout eyesContainer = new VerticalLayout();
            eyesContainer.setWidthFull();
            eyesContainer.setHeightFull(); // Volle Höhe nutzen
            eyesContainer.setPadding(false);
            eyesContainer.setSpacing(false);
            eyesContainer.getStyle().set("display", "flex");
            eyesContainer.getStyle().set("flex-direction", "row");
            // overflow entfernt - jeder TimelineView scrollt sich selbst
            
            // Immer beide Augen anzeigen, auch wenn noch keine Behandlungen existieren
            initializeTimeLineRightEye(eyesContainer); // OD (rechts vom Patienten = links in UI)
            initializeTimeLineLeftEye(eyesContainer);  // OS (links vom Patienten = rechts in UI)
            
            timeLineLayout.add(eyesContainer);
            timeLineLayout.expand(eyesContainer); // Container soll verfügbaren Platz nutzen
        } else {
            // Horizontal: show vertically stacked
            // Immer beide Augen anzeigen, auch wenn noch keine Behandlungen existieren
            initializeTimeLineRightEye(timeLineLayout); // OD (rechts vom Patienten = links in UI)
            initializeTimeLineLeftEye(timeLineLayout);  // OS (links vom Patienten = rechts in UI)
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
        timeLineViewRightEye.setTimelineHeight(null); // Keine feste Höhe - nutze verfügbaren Platz
        timeLineViewRightEye.addClassName("right-eye-timeline");
        timeLineViewRightEye.getStyle().set("background-color", "#E3F2FD"); // Blue tint
        Accordion accordionRight = new Accordion();
        accordionRight.setWidthFull();
        accordionRight.setHeightFull(); // Volle Höhe nutzen
        // overflow entfernt - Scrolling soll in der TimelineView passieren
        AccordionPanel accordionPanelRight = accordionRight.add("Behandlungsverlauf rechtes Auge (OD)", timeLineViewRightEye);
        accordionPanelRight.setOpened(true);
        accordionPanelRight.setWidthFull();
        timeLineViewRightEye.setSizeFull(); // TimelineView soll volle Größe nutzen
        // overflow entfernt - Scrolling soll in der TimelineView passieren
        timeLineLayout.add(accordionRight);
        if (timeLineLayout instanceof VerticalLayout) {
            ((VerticalLayout) timeLineLayout).expand(accordionRight); // Accordion soll verfügbaren Platz nutzen
        }
        setRightEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setRightEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> rightEyeTreatments = new ArrayList<>();
        int treatmentCount = 0;
        Integer mostCommonInterval = null;
        
        if (treatmentPlanId != null) {
            List<Treatment> treatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, treatmentPlanId);
            treatmentCount = treatments.size();
            
            // Berechne häufigstes Intervall
            mostCommonInterval = calculateMostCommonInterval(treatments);
            
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                rightEyeTreatments.add(config);
            }
        }
        
        // Startkachel mit Statistiken erstellen
        TimeLineCardConfig firstCard = new TimeLineCardConfig()
                .setFirst(true)
                .setFirstDate(current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now())
                .setTreatmentCount(treatmentCount)
                .setMostCommonInterval(mostCommonInterval);
        rightEyeTreatments.add(0, firstCard);
        
        // Callback für Button setzen
        timeLineViewRightEye.setOnBookNextTreatmentCallback(() -> {
            openNextTreatmentBookingDialog(SideOfEye.RIGHT);
        });
        
        // Auch bei null (neuer Plan) initialisieren - zeigt dann wenigstens Start-Marker
        timeLineViewRightEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewRightEye.setItems(rightEyeTreatments);
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

    private void initializeTimeLineLeftEye(VerticalLayout timeLineLayout) {
        // linkes Auge mit medizinisch korrekter Darstellung (links = rechts vom Patienten)
        timeLineViewLeftEye.setTimelineHeight(null); // Keine feste Höhe - nutze verfügbaren Platz
        timeLineViewLeftEye.addClassName("left-eye-timeline");
        timeLineViewLeftEye.getStyle().set("background-color", "#FFF3E0"); // Orange tint
        Accordion accordionLeft = new Accordion();
        accordionLeft.setWidthFull();
        accordionLeft.setHeightFull(); // Volle Höhe nutzen
        // overflow entfernt - Scrolling soll in der TimelineView passieren
        AccordionPanel accordionPanelLeft = accordionLeft.add("Behandlungsverlauf linkes Auge (OS)", timeLineViewLeftEye);
        accordionPanelLeft.setOpened(true);
        accordionPanelLeft.setWidthFull();
        timeLineViewLeftEye.setSizeFull(); // TimelineView soll volle Größe nutzen
        // overflow entfernt - Scrolling soll in der TimelineView passieren
        timeLineLayout.add(accordionLeft);
        if (timeLineLayout instanceof VerticalLayout) {
            ((VerticalLayout) timeLineLayout).expand(accordionLeft); // Accordion soll verfügbaren Platz nutzen
        }
        setLeftEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setLeftEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> leftEyeTreatments = new ArrayList<>();
        int treatmentCount = 0;
        Integer mostCommonInterval = null;
        
        if (treatmentPlanId != null) {
            List<Treatment> treatments = presenter.getTreatmentDtos(SideOfEye.LEFT, treatmentPlanId);
            treatmentCount = treatments.size();
            
            // Berechne häufigstes Intervall
            mostCommonInterval = calculateMostCommonInterval(treatments);
            
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                leftEyeTreatments.add(config);
            }
        }
        
        // Startkachel mit Statistiken erstellen
        TimeLineCardConfig firstCard = new TimeLineCardConfig()
                .setFirst(true)
                .setFirstDate(current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now())
                .setTreatmentCount(treatmentCount)
                .setMostCommonInterval(mostCommonInterval);
        leftEyeTreatments.add(0, firstCard);
        
        // Callback für Button setzen
        timeLineViewLeftEye.setOnBookNextTreatmentCallback(() -> {
            openNextTreatmentBookingDialog(SideOfEye.LEFT);
        });
        
        // Auch bei null (neuer Plan) initialisieren - zeigt dann wenigstens Start-Marker
        timeLineViewLeftEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewLeftEye.setItems(leftEyeTreatments);
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

    public void setCurrent(TreatmentPlan newCurrent) {
        this.current = newCurrent;
        binder.setBean(newCurrent);
        if (newCurrent.getPatient() != null) {
            patientSelectComboBox.setReadOnly(true);
        }
        setLeftEyeTreatmentHistory(newCurrent.getId());
        setRightEyeTreatmentHistory(newCurrent.getId());
        
        // Aktualisiere Patientendaten-Card in Section "Allgemein"
        updatePatientInfoCard();
        
        // Aktualisiere Section "Termine buchen"
        updateAppointmentBookingSection();
        
        // Aktualisiere Grids falls Grid-Ansicht aktiv
        if (gridContainer != null && gridContainer.isVisible()) {
            refreshGrids();
        }
        
        // Refresh timeline display if orientation toggle exists
        // Note: This will be called after the layout is already built, so we need to update it
        // The accordions will be re-added by updateTimelineLayout if needed
    }
    
    /**
     * Aktualisiert die Patientendaten-Card in der Section "Allgemein".
     */
    private void updatePatientInfoCard() {
        if (generalSection == null) {
            return;
        }
        
        // Entferne alte Patientendaten-Card falls vorhanden
        generalSection.getChildren()
            .filter(c -> c.getClass().getSimpleName().equals("Div") 
                && c.getElement().getClassList().contains("patient-info-card"))
            .findFirst()
            .ifPresent(generalSection::remove);
        
        // Füge neue Patientendaten-Card hinzu, falls Patient vorhanden
        if (current != null && current.getPatient() != null) {
            // Finde den Index nach dem Titel (Index 0)
            generalSection.addComponentAtIndex(1, createPatientInfoCard());
        }
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
                    // Nach erfolgreicher Buchung: Timeline aktualisieren
                    setLeftEyeTreatmentHistory(current.getId());
                    setRightEyeTreatmentHistory(current.getId());
                    // Aktualisiere Section "Termine buchen"
                    updateAppointmentBookingSection();
                    // Aktualisiere Grids falls Grid-Ansicht aktiv
                    if (gridContainer != null && gridContainer.isVisible()) {
                        refreshGrids();
                    }
                });
        dialog.open();
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
