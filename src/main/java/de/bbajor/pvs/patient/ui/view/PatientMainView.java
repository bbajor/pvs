package de.bbajor.pvs.patient.ui.view;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.ai.service.ExtractionClient;
import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.FeatureFlagService;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.presenter.PatientListPresenter;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route("patient-search")
@PageTitle("Patienten")
@Menu(order = 2, icon = "vaadin:male", title = "Patienten")
@PermitAll
public class PatientMainView extends Main implements PatientChangeListener, BeforeEnterObserver {

    private final PatientListPresenter patientListPresenter;
    private final VoiceTranscriptionService transcriptionService;
    private final ExtractionOrchestrator extractionOrchestrator;
    private final de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository;
    private final FeatureFlagService featureFlagService;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final de.bbajor.pvs.base.service.InstitutionPrerequisitesService prerequisitesService;
    private Grid<Patient> patientGrid;
    
    // Cache für TreatmentPlans pro Patient (wird beim Query gefüllt, um N+1 zu vermeiden)
    private java.util.Map<Integer, de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan> treatmentPlanCache = 
            new java.util.HashMap<>();

    public PatientMainView(PatientListPresenter patientListPresenter, VoiceTranscriptionService transcriptionService, 
            ExtractionOrchestrator extractionOrchestrator, de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository,
            FeatureFlagService featureFlagService, TreatmentPlanRepository treatmentPlanRepository,
            de.bbajor.pvs.base.service.InstitutionPrerequisitesService prerequisitesService) {
        this.patientListPresenter = patientListPresenter;
        this.transcriptionService = transcriptionService;
        this.extractionOrchestrator = extractionOrchestrator;
        this.userAccountRepository = userAccountRepository;
        this.featureFlagService = featureFlagService;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.prerequisitesService = prerequisitesService;
        
        // Überschrift
        H1 title = new H1("Übersicht Patienten");
        add(title);

        // Container als Flexbox konfigurieren
        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("min-height", "0");

        // Section für Buttons und Suche
        Div toolbarSection = createToolbarSection();
        toolbarSection.getStyle().set("flex-shrink", "0");
        add(toolbarSection);
        
        // Grid - nimmt restlichen Platz ein und scrollt
        configureGrid();
        patientGrid.setSizeFull();
        patientGrid.getStyle().set("flex-grow", "1");
        patientGrid.getStyle().set("min-height", "0");
        add(patientGrid);
    }

    private Div createToolbarSection() {
        Div section = new Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        section.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("box-sizing", "border-box")
            .set("margin-bottom", "var(--lumo-space-m)");
        
        H4 sectionTitle = new H4("Aktionen");
        sectionTitle.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-s)")
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-size", "var(--lumo-font-size-m)")
            .set("font-weight", "600");
        section.add(sectionTitle);
        
        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setSpacing(true);
        toolbarLayout.setWidthFull();
        toolbarLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        Button newPatientButton = new Button("Patienten anlegen", event -> openPatientDialog(new Patient()));
        newPatientButton.setIcon(VaadinIcon.PLUS.create());
        newPatientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newPatientButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        
        // Prüfe Voraussetzungen für Patientenanlage
        ensureInstitutionContext();
        boolean canCreate = prerequisitesService.canCreatePatient();
        List<String> missingPrerequisites = prerequisitesService.getMissingPrerequisitesForPatientCreation();
        newPatientButton.setEnabled(canCreate);
        
        // Button-Layout mit Hinweis-Icon
        HorizontalLayout newPatientButtonLayout = new HorizontalLayout();
        newPatientButtonLayout.setSpacing(true);
        newPatientButtonLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        newPatientButtonLayout.add(newPatientButton);
        
        if (!canCreate && !missingPrerequisites.isEmpty()) {
            com.vaadin.flow.component.icon.Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
            infoIcon.getStyle().set("color", "var(--lumo-warning-color)");
            String tooltipText = "Folgende Voraussetzungen fehlen:\n" + 
                String.join("\n", missingPrerequisites);
            infoIcon.setTooltipText(tooltipText);
            newPatientButtonLayout.add(infoIcon);
        }
        
        TextField searchField = new TextField();
        searchField.setPlaceholder("Mindestens 3 Zeichen für Suche eingeben");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        
        // Suche mit KeyUpListener für Live-Suche während des Tippens
        // Mindestens 3 Zeichen erforderlich, Debouncing mit Abbrechen alter Suchen
        searchField.addKeyUpListener(event -> {
            String searchTerm = searchField.getValue();
            if (searchTerm == null || searchTerm.trim().length() < 3) {
                // Wenn weniger als 3 Zeichen, zeige alle Ergebnisse (keine Suche)
                scheduleSearch("");
                return;
            }
            
            // Mindestens 3 Zeichen: Suche auslösen mit Debouncing
            scheduleSearch(searchTerm.trim());
        });
        
        // Auch bei ValueChange (z.B. Clear-Button) reagieren
        searchField.addValueChangeListener(e -> {
            String searchTerm = e.getValue();
            if (searchTerm == null || searchTerm.trim().length() < 3) {
                // Wenn weniger als 3 Zeichen, zeige alle Ergebnisse
                scheduleSearch("");
                return;
            }
            
            scheduleSearch(searchTerm.trim());
        });
        
        toolbarLayout.add(newPatientButtonLayout, searchField);
        toolbarLayout.setFlexGrow(1, searchField);
        
        section.add(toolbarLayout);
        return section;
    }

    private void configureGrid() {
        if (patientGrid != null) {
            remove(patientGrid);
        }
        patientGrid = new Grid<>(Patient.class, false);
        patientGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        
        // Nr. Spalte als erste Spalte
        java.util.concurrent.atomic.AtomicInteger rowCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        patientGrid.addColumn(new ComponentRenderer<>(patient -> {
            // Zeilennummer pro Seite (startet bei 1 auf jeder Seite)
            int rowNumber = rowCounter.incrementAndGet();
            Span span = new Span(String.valueOf(rowNumber));
            span.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            return span;
        })).setHeader("Nr.").setResizable(false).setAutoWidth(true).setFlexGrow(0);
        
        // Patientendaten-Renderer (eine Spalte) - wie im TaskReviewDialog
        patientGrid.addColumn(new ComponentRenderer<>(patient -> {
            if (patient != null) {
                VerticalLayout patientLayout = new VerticalLayout();
                patientLayout.setSpacing(false);
                patientLayout.setPadding(false);
                
                String name = (patient.getLastName() != null ? patient.getLastName() : "") + 
                              (patient.getFirstName() != null ? ", " + patient.getFirstName() : "");
                if (name.startsWith(", ")) name = name.substring(2);
                if (name.isEmpty()) name = "-";
                
                Span nameSpan = new Span(name);
                nameSpan.getStyle().set("font-weight", "600");
                patientLayout.add(nameSpan);
                
                if (patient.getBirth() != null) {
                    Span birthSpan = new Span("geb. " + 
                        patient.getBirth().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    birthSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    birthSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    patientLayout.add(birthSpan);
                }
                
                return patientLayout;
            }
            return new Span("-");
        })).setHeader("Patient").setResizable(true).setAutoWidth(true);
        
        Grid.Column<Patient> insuranceColumn = patientGrid.addColumn(
                new ComponentRenderer<>(patient -> {
                    String insurance = patient.getHealthInsurance() != null 
                            ? patient.getHealthInsurance().toString() : "-";
                    Span span = new Span(insurance);
                    span.addClassNames(LumoUtility.TextColor.SECONDARY);
                    span.getStyle().set("white-space", "normal");
                    span.getStyle().set("word-wrap", "break-word");
                    return span;
                })).setHeader("Krankenkasse").setResizable(true).setAutoWidth(true);
        
        // Spalte für Behandlungsplan-Status
        Grid.Column<Patient> treatmentPlanColumn = patientGrid.addColumn(
                new ComponentRenderer<>(patient -> {
                    if (patient.getId() == null) {
                        return new Span();
                    }
                    
                    // Verwende Cache statt einzelne Query (vermeidet N+1 Problem)
                    de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan activePlan = 
                            treatmentPlanCache.get(patient.getId());
                    boolean hasActivePlan = activePlan != null;
                    
                    Button statusButton = new Button();
                    statusButton.setWidthFull();
                    if (hasActivePlan) {
                        // Behandlungsplan vorhanden - Button klickbar, führt zur Detailansicht
                        statusButton.setText("Behandlungsplan öffnen");
                        statusButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                        statusButton.setEnabled(true);
                        statusButton.addClickListener(e -> {
                            // Navigiere zur Detailansicht des Behandlungsplans
                            UI.getCurrent().navigate("ivom/" + activePlan.getId());
                        });
                    } else {
                        // Kein Behandlungsplan - Button zum Erstellen
                        statusButton.setText("Behandlungsplan erstellen");
                        statusButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                        statusButton.getStyle().set("background-color", "#ffeb3b");
                        statusButton.getStyle().set("color", "#000000");
                        
                        // Prüfe Voraussetzungen für Behandlungsplan-Erstellung
                        ensureInstitutionContext();
                        boolean canCreate = prerequisitesService.canCreateTreatmentPlan();
                        List<String> missingPrerequisites = prerequisitesService.getMissingPrerequisitesForTreatmentPlanCreation();
                        statusButton.setEnabled(canCreate);
                        
                        if (!canCreate && !missingPrerequisites.isEmpty()) {
                            String tooltipText = "Folgende Voraussetzungen fehlen:\n" + 
                                String.join("\n", missingPrerequisites);
                            statusButton.setTooltipText(tooltipText);
                        }
                        
                        statusButton.addClickListener(e -> {
                            if (!canCreate) {
                                Notification.show("Bitte erfüllen Sie zuerst alle notwendigen Voraussetzungen", 
                                        5000, Notification.Position.MIDDLE)
                                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                                return;
                            }
                            // Gleiche Logik wie im IVOM-Planer: Erstelle TreatmentPlan mit id=-1L
                            TreatmentPlan newTreatmentPlan = new TreatmentPlan();
                            newTreatmentPlan.setId(-1L);
                            // Navigiere zu ivom/-1 mit patientId als Query-Parameter
                            QueryParameters queryParams = QueryParameters.simple(
                                    java.util.Map.of("patientId", String.valueOf(patient.getId()))
                            );
                            UI.getCurrent().navigate("ivom/-1", queryParams);
                        });
                    }
                    return statusButton;
                })).setHeader("Behandlungsplan").setResizable(false).setAutoWidth(false).setFlexGrow(0).setWidth("200px");

        // Paging aktivieren mit max 17 Einträgen pro Seite
        ensureInstitutionContext();
        patientGrid.setPageSize(17);
        patientGrid.setItems(query -> {
            // Counter beim Start jeder Query zurücksetzen
            rowCounter.set(0);
            ensureInstitutionContext();
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return java.util.stream.Stream.empty();
            }
            String searchTerm = getSearchTerm();
            // Begrenze die Query auf max 17 Einträge
            int limit = Math.min(query.getLimit(), 17);
            com.vaadin.flow.data.provider.Query<Patient, ?> pageQuery = new com.vaadin.flow.data.provider.Query<>(
                    query.getOffset(), 
                    limit,
                    query.getSortOrders(),
                    query.getInMemorySorting(),
                    null
            );
            
            java.util.stream.Stream<Patient> patientStream;
            if (searchTerm == null || searchTerm.isEmpty()) {
                patientStream = patientListPresenter.findAll(toSpringPageRequest(pageQuery)).stream();
            } else {
                patientStream = patientListPresenter.findAllBy(searchTerm, toSpringPageRequest(pageQuery)).stream();
            }
            
            // Lade TreatmentPlans für alle Patienten auf dieser Seite in einem Batch (vermeidet N+1)
            java.util.List<Patient> patients = patientStream.collect(java.util.stream.Collectors.toList());
            if (!patients.isEmpty()) {
                java.util.List<Integer> patientIds = patients.stream()
                        .map(Patient::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
                
                if (!patientIds.isEmpty()) {
                    java.util.List<de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan> activePlans = 
                            treatmentPlanRepository.findActiveTreatmentPlansByPatients(institutionId, patientIds);
                    
                    // Cache füllen: Pro Patient nur den neuesten aktiven Plan (ORDER BY creationDate DESC)
                    treatmentPlanCache.clear();
                    for (de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan plan : activePlans) {
                        Integer patientId = plan.getPatient().getId();
                        // Nur den ersten (neuesten) Plan pro Patient speichern
                        treatmentPlanCache.putIfAbsent(patientId, plan);
                    }
                }
            }
            
            return patients.stream();
        });

        // Zeilenumbruch in Zellen aktivieren
        patientGrid.getStyle().set("--vaadin-grid-cell-content-overflow", "visible");

        // Grid-Styling wie im IVOM-Planer
        patientGrid.setWidthFull();
        patientGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        patientGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);
        patientGrid.asSingleSelect().addValueChangeListener(event -> {
            Patient patientDto = event.getValue();
            if (patientDto != null) {
                openPatientDialog(patientDto);
            }
        });
    }
    
    private String searchTerm = "";
    
    private String getSearchTerm() {
        return searchTerm;
    }
    
    /**
     * Plant eine Suche mit Debouncing ein, um zyklische Abhängigkeiten zu vermeiden.
     * Bricht vorherige Suchen ab, wenn eine neue gestartet wird.
     */
    private void scheduleSearch(String searchString) {
        // Plane neue Suche mit 300ms Verzögerung (Debouncing) über JavaScript
        // JavaScript kümmert sich um das Abbrechen vorheriger Timeouts
        com.vaadin.flow.component.UI currentUI = com.vaadin.flow.component.UI.getCurrent();
        if (currentUI != null) {
            final String finalSearchString = searchString != null ? searchString : "";
            currentUI.getPage().executeJs(
                "if (window.patientSearchTimeout) { clearTimeout(window.patientSearchTimeout); } " +
                "window.patientSearchTimeout = setTimeout(function() { $0.$server.executePatientSearch($1); }, 300);",
                getElement(), finalSearchString
            );
        }
    }
    
    /**
     * Wird von JavaScript aufgerufen, um die Suche tatsächlich auszuführen.
     * Dies verhindert zyklische Abhängigkeiten, da die Suche asynchron erfolgt.
     */
    @com.vaadin.flow.component.ClientCallable
    private void executePatientSearch(String searchString) {
        refreshGrid(searchString);
    }
    
    private void refreshGrid(String searchString) {
        this.searchTerm = searchString != null ? searchString : "";
        patientGrid.getDataProvider().refreshAll();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access patient data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
        }
    }

    private void openPatientDialog(Patient dto) {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
        PatientDialog dialog = new PatientDialog(patientListPresenter.getDialogPresenter(), dto, 
                new ExtractionClient(extractionOrchestrator), transcriptionService, userAccountRepository, featureFlagService);
        dialog.addChangeListener(this);
        dialog.open();
    }
    
    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof de.bbajor.pvs.institution.security.InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof 
                   de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter adapter) {
            // Authentication wurde aus Session deserialisiert
            try {
                String username = adapter.getUsername();
                de.bbajor.pvs.security.domain.UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                }
            } catch (Exception e) {
                // Fehler beim Wiederherstellen des Contexts - ignorieren
            }
        }
    }

    @Override
    public void onPatientChanged(Patient patientDto) {
        patientGrid.getDataProvider().refreshAll();
    }
}