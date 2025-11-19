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
import com.vaadin.flow.component.tabs.TabSheet;
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
    private final TabSheet tabSheet = new TabSheet();
    private final TreatmentPlanPresenter presenter;
    private TreatmentPlan current;
    private final ApplicationContext context;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter, TreatmentPlan treatmentPlan,
            ApplicationContext context) {
        this.presenter = presenter;
        this.current = treatmentPlan;
        this.context = context;

        setSizeFull();
        // overflow entfernt - erlaube Scrollen wenn nötig
        
        add(tabSheet);
        expand(tabSheet);
        tabSheet.setSizeFull(); // TabSheet soll volle Größe nutzen

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

        initializeGeneralDetailsTab();
        initializeTreatmentAppointmentOverviewTab();
        initializeAppointmentTab();

        initializeBinder(treatmentPlan);
    }

    private void initializeTreatmentAppointmentOverviewTab() {
        VerticalLayout timeLineLayout = new VerticalLayout();
        timeLineLayout.setSizeFull();
        timeLineLayout.setPadding(false);
        timeLineLayout.setSpacing(false);
        // Kein overflow hidden - erlaube Scrollen wenn nötig
        
        // Export-Button für Patienten-Ausdruck
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        
        Button exportButton = new Button("Terminübersicht drucken", new Icon(VaadinIcon.PRINT));
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.addClickListener(e -> generateAppointmentReport());
        buttonLayout.add(exportButton);
        timeLineLayout.add(buttonLayout);
        
        // Orientation toggle controls both timelines
        RadioButtonGroup<TimelineView.Orientation> orientationToggle = new RadioButtonGroup<>();
        orientationToggle.setLabel("Ausrichtung");
        orientationToggle.setItems(TimelineView.Orientation.HORIZONTAL, TimelineView.Orientation.VERTICAL);
        orientationToggle.setValue(TimelineView.Orientation.HORIZONTAL);
        orientationToggle.setItemLabelGenerator(item ->
                item == TimelineView.Orientation.HORIZONTAL ? "Horizontal" : "Vertikal");
        orientationToggle.addValueChangeListener(e -> {
            TimelineView.Orientation o = e.getValue();
            timeLineViewLeftEye.setOrientation(o);
            timeLineViewRightEye.setOrientation(o);
            
            // Update layout based on orientation
            updateTimelineLayout(timeLineLayout, o);
        });
        timeLineLayout.add(orientationToggle);
        timeLineViewLeftEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        timeLineViewRightEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        
        // Initial setup
        updateTimelineLayout(timeLineLayout, TimelineView.Orientation.HORIZONTAL);
        
        tabSheet.add("Behandlungsübersicht", timeLineLayout);
    }
    
    private void generateAppointmentReport() {
        if (current == null || current.getPatient() == null) {
            Notification.show("Kein Patient ausgewählt", 3000, 
                    Notification.Position.BOTTOM_CENTER);
            return;
        }
        
        try {
            ensureInstitutionContext();
            AppointmentReportService reportService = context.getBean(AppointmentReportService.class);
            Patient patient = current.getPatient();
            byte[] pdfBytes = reportService.generatePatientAppointmentReport(patient);
            
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
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
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
        timeLineLayout.getChildren()
                .filter(child -> {
                    String className = child.getClass().getSimpleName();
                    return className.equals("Accordion") || className.equals("VerticalLayout");
                })
                .filter(child -> {
                    // Behalte nur den orientationToggle - er ist das erste Child nach dem Toggle
                    return timeLineLayout.indexOf(child) > 0;
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

    private void initializeAppointmentTab() {
        VerticalLayout appointmentLayout = new VerticalLayout();
        appointmentLayout.setSizeFull();
        initializeTimeSlotFilter(appointmentLayout);
        appointmentLayout.add(filterTimeSlotsButton);
        timeSlotGrid.setSizeFull();
        appointmentLayout.add(timeSlotGrid);
        tabSheet.add("Behandlungen planen", appointmentLayout);
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

    private void initializeGeneralDetailsTab() {
        VerticalLayout tabContent = new VerticalLayout();
        tabContent.setSpacing(true);
        tabContent.setPadding(true);
        
        // Patientendaten-Block (immer sichtbar, wenn Patient vorhanden)
        if (current != null && current.getPatient() != null) {
            tabContent.add(createPatientInfoCard());
        }
        
        // Formular für Behandlungsplan-Details
        FormLayout formLayout = new FormLayout();
        formLayout.add(creationDatePicker);
        formLayout.add(patientSelectComboBox);
        formLayout.add(reasonForTreatmentComboBox);
        formLayout.add(additionalInformation, 2);
        
        AccordionPanel generalDetailsPanel = new AccordionPanel();
        Accordion generalDetails = new Accordion();
        generalDetailsPanel.add(formLayout);
        generalDetailsPanel.setOpened(true);
        generalDetails.add(generalDetailsPanel);
        
        tabContent.add(generalDetails);
        tabContent.expand(generalDetails);
        
        tabSheet.add("Allgemeine Informationen", tabContent);
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
        
        // Aktualisiere Patientendaten-Card im Tab "Allgemeine Informationen"
        updatePatientInfoCard();
        
        // Refresh timeline display if orientation toggle exists
        // Note: This will be called after the layout is already built, so we need to update it
        // The accordions will be re-added by updateTimelineLayout if needed
    }
    
    /**
     * Aktualisiert die Patientendaten-Card im Tab "Allgemeine Informationen".
     */
    private void updatePatientInfoCard() {
        // Finde den Tab "Allgemeine Informationen"
        tabSheet.getChildren()
            .filter(child -> {
                if (child instanceof com.vaadin.flow.component.tabs.Tab) {
                    com.vaadin.flow.component.tabs.Tab tab = (com.vaadin.flow.component.tabs.Tab) child;
                    return "Allgemeine Informationen".equals(tab.getLabel());
                }
                return false;
            })
            .findFirst()
            .ifPresent(tab -> {
                // Finde den Content des Tabs
                com.vaadin.flow.component.Component tabContent = tabSheet.getChildren()
                    .filter(c -> c instanceof com.vaadin.flow.component.tabs.Tab 
                        && "Allgemeine Informationen".equals(((com.vaadin.flow.component.tabs.Tab) c).getLabel()))
                    .findFirst()
                    .map(t -> {
                        int index = tabSheet.getChildren()
                            .collect(java.util.stream.Collectors.toList())
                            .indexOf(t);
                        return tabSheet.getChildren()
                            .skip(index + 1)
                            .findFirst()
                            .orElse(null);
                    })
                    .orElse(null);
                
                if (tabContent instanceof VerticalLayout) {
                    VerticalLayout content = (VerticalLayout) tabContent;
                    // Entferne alte Patientendaten-Card falls vorhanden
                    content.getChildren()
                        .filter(c -> c.getClass().getSimpleName().equals("Div") 
                            && c.getElement().getClassList().contains("patient-info-card"))
                        .findFirst()
                        .ifPresent(content::remove);
                    
                    // Füge neue Patientendaten-Card hinzu, falls Patient vorhanden
                    if (current != null && current.getPatient() != null) {
                        content.addComponentAtIndex(0, createPatientInfoCard());
                    }
                }
            });
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
