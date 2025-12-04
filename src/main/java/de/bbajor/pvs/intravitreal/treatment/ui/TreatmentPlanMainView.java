package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.appointment.service.AppointmentService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanListPresenter;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;
import de.bbajor.pvs.taskmanagement.ui.view.TaskReviewDialog;
import jakarta.annotation.security.PermitAll;

@Route("ivom")
@PageTitle("IVOM-Planer")
@Menu(order = 1, icon = "vaadin:calendar-user", title = "IVOM-Planer")
@PermitAll
public class TreatmentPlanMainView extends Main implements TreatmentPlanChangeListener, BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(TreatmentPlanMainView.class);

    private final TreatmentPlanListPresenter ivomListPresenter;
    private final CurrentUser currentUser;
    private final TaskService taskService;
    private final TreatmentRepository treatmentRepository;
    private final AuthenticationContext authenticationContext;
    private final TreatmentReportService reportService;
    private final UserAccountRepository userAccountRepository;
    private final Clock clock;
    private final ApplicationContext applicationContext;
    private final TreatmentPlanPresenter treatmentPlanPresenter;
    private final AppointmentService appointmentService;

    // Treatment Plans Tab Components
    private final TextField searchField = new TextField();
    private final Button createButton = new Button();
    private final Button generateDailyListButton = new Button();
    private final Grid<TreatmentPlan> ivomPlanGrid = new Grid<>(TreatmentPlan.class, false);
    private final VerticalLayout treatmentPlansContent = new VerticalLayout();
    private final Button toggleFinishedButton = new Button();
    private final ProgressBar gridLoadingProgressBar = new ProgressBar();
    private final Div gridContainer = new Div();
    private volatile boolean showFinished = true;
    private volatile boolean isGridInitialized = false;

    // Task Review Tab Components
    private final Grid<Task> taskGrid = new Grid<>();
    private final TextField taskFilterText = new TextField();
    private final Button taskToggleCompleted = new Button("Abgeschlossene ein-/ausblenden");
    private final Button taskRefreshButton = new Button("Aktualisieren");
    private boolean hideCompleted = false;
    private final VerticalLayout taskReviewContent = new VerticalLayout();

    // Tab Components
    private final Tab treatmentPlansTab = new Tab("Übersicht");
    private final Tab taskReviewTab = new Tab("Dokumentation");
    private final Tabs tabs = new Tabs(treatmentPlansTab, taskReviewTab);
    private final VerticalLayout tabContent = new VerticalLayout();

    public TreatmentPlanMainView(
            TreatmentPlanListPresenter ivomListPresenter,
            CurrentUser currentUser,
            TaskService taskService,
            TreatmentRepository treatmentRepository,
            AuthenticationContext authenticationContext,
            TreatmentReportService reportService,
            UserAccountRepository userAccountRepository,
            Clock clock,
            ApplicationContext applicationContext,
            TreatmentPlanPresenter treatmentPlanPresenter,
            AppointmentService appointmentService) {
        this.ivomListPresenter = ivomListPresenter;
        this.currentUser = currentUser;
        this.taskService = taskService;
        this.treatmentRepository = treatmentRepository;
        this.authenticationContext = authenticationContext;
        this.reportService = reportService;
        this.userAccountRepository = userAccountRepository;
        this.clock = clock;
        this.applicationContext = applicationContext;
        this.treatmentPlanPresenter = treatmentPlanPresenter;
        this.appointmentService = appointmentService;

        initializeTreatmentPlansTab();
        initializeTaskReviewTab();

        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            tabContent.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == treatmentPlansTab) {
                tabContent.add(treatmentPlansContent);
            } else if (selected == taskReviewTab) {
                tabContent.add(taskReviewContent);
            }
        });

        tabContent.setSizeFull();

        // Überschrift - fixiert oben
        H1 title = new H1("IVOM-Planer");

        add(title);
        add(tabs);
        add(tabContent);

        // Show treatment plans tab by default
        tabs.setSelectedTab(treatmentPlansTab);
        tabContent.add(treatmentPlansContent);

        setSizeFull();
    }

    private void initializeTreatmentPlansTab() {
        createButton.setText("Erstellen");
        createButton.addClickListener(event -> {
            Optional<TreatmentPlan> ivom = ivomPlanGrid.getSelectionModel().getFirstSelectedItem();
            if (ivom.isPresent()) {
                navigateToDetailView(ivom.get());
            } else {
                TreatmentPlan newTreatmentPlan = new TreatmentPlan();
                newTreatmentPlan.setId(-1L);
                navigateToDetailView(newTreatmentPlan);
            }
        });
        
        // Button nur für berechtigte Rollen aktivieren
        boolean canBook = currentUser.getPrincipal()
                .map(principal -> {
                    return principal.getAuthorities().stream()
                            .anyMatch(auth -> {
                                String authority = auth.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.ADMIN)
                                        || authority.equals("ROLE_" + AppRoles.DOCTOR)
                                        || authority.equals("ROLE_" + AppRoles.TECH_USER);
                            });
                })
                .orElse(false);
        createButton.setEnabled(canBook);
        if (!canBook) {
            createButton.setTooltipText("Sie benötigen die Rolle ADMIN, DOCTOR oder TECH_USER, um Termine zu buchen");
        }

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsjahr, Krankenkasse, Diagnose oder zusätzlichen Informationen");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());

        generateDailyListButton.setText("Wochenliste");
        generateDailyListButton.addClickListener(event -> {
            ensureInstitutionContext();
            openWeekListDialog();
        });
        generateDailyListButton
                .setTooltipText("Erzeugt eine Tagesliste für die tagesaktuellen OP-Slots. "
                        + "Dabei werden die zu behandelnden Patienten, sowie die jeweiligen "
                        + "Einrichtungen aufgelistet, an denen die Behandlung stattfindet.");
        generateDailyListButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateDailyListButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        // Button für abgeschlossene Pläne ein-/ausblenden
        updateToggleFinishedButton();
        toggleFinishedButton.addClickListener(e -> {
            showFinished = !showFinished;
            updateToggleFinishedButton();
            refresh("");
        });

        // Container als Flexbox konfigurieren - WICHTIG: VOR add() aufrufen
        treatmentPlansContent.setSizeFull();

        Div toolbarSection = createToolbarSection();
        toolbarSection.getStyle().set("flex-shrink", "0");
        treatmentPlansContent.add(toolbarSection);

        // Grid-Container mit ProgressBar
        gridContainer.setSizeFull();
        gridContainer.getStyle()
                .set("position", "relative")
                .set("display", "flex")
                .set("flex-direction", "column");
        
        // ProgressBar über dem Grid positionieren
        gridLoadingProgressBar.setIndeterminate(true);
        gridLoadingProgressBar.setWidthFull();
        gridLoadingProgressBar.getStyle()
                .set("position", "absolute")
                .set("top", "0")
                .set("left", "0")
                .set("right", "0")
                .set("z-index", "10")
                .set("margin", "0");
        
        ivomPlanGrid.setSizeFull();
        // Grid initial unsichtbar, bis es geladen ist
        ivomPlanGrid.setVisible(false);
        
        // Nach dem ersten Attach des Grids ProgressBar ausblenden und Grid einblenden
        ivomPlanGrid.addAttachListener(e -> {
            if (!isGridInitialized) {
                // Kurze Verzögerung, damit das Grid Zeit zum Rendern hat
                UI.getCurrent().access(() -> {
                    UI.getCurrent().getPage().executeJs(
                        "setTimeout(function() { $0.$server.hideProgressBarAfterLoad(); }, 200);",
                        getElement()
                    );
                });
            }
        });
        
        gridContainer.add(gridLoadingProgressBar, ivomPlanGrid);
        treatmentPlansContent.add(gridContainer);

        configureGrid();
        configureSearch();
    }

    private void initializeTaskReviewTab() {
        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withZone(clock.getZone())
                .withLocale(getLocale());
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        taskGrid.setItems(query -> {
            ensureInstitutionContext();
            return taskService.list(toSpringPageRequest(query)).stream();
        });
        // Verbesserte Grid-Spalten mit ComponentRenderer
        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            Span s = new Span(task.getDescription());
            if (task.isCompleted()) {
                s.addClassNames(LumoUtility.TextColor.SECONDARY);
                s.getStyle().set("text-decoration", "line-through");
            } else {
                s.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            }
            s.getStyle().set("white-space", "normal");
            s.getStyle().set("word-wrap", "break-word");
            return s;
        })).setHeader("Beschreibung").setResizable(true).setAutoWidth(true);

        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            String dueDate = Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Nie");
            Span dateSpan = new Span(dueDate);
            if (task.getDueDate() != null && task.getDueDate().isBefore(java.time.LocalDate.now()) && !task.isCompleted()) {
                dateSpan.addClassNames(LumoUtility.TextColor.ERROR);
            } else {
                dateSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            }
            dateSpan.getStyle().set("white-space", "normal");
            dateSpan.getStyle().set("word-wrap", "break-word");
            return dateSpan;
        })).setHeader("Fälligkeitsdatum").setResizable(true).setAutoWidth(true);

        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            Span dateSpan = new Span(dateTimeFormatter.format(task.getCreationDate()));
            dateSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            dateSpan.getStyle().set("white-space", "normal");
            dateSpan.getStyle().set("word-wrap", "break-word");
            return dateSpan;
        })).setHeader("Erstellt am").setResizable(true).setAutoWidth(true);

        // Zeilenumbruch in Zellen aktivieren
        taskGrid.getStyle().set("--vaadin-grid-cell-content-overflow", "visible");

        taskGrid.setPartNameGenerator(task -> task.isCompleted() ? "row-completed" : "");
        taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // Nur Width auf 100% setzen, Höhe wird über Flexbox gesteuert
        taskGrid.setWidthFull();
        taskGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        taskGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);
        taskGrid.addItemDoubleClickListener(ev -> {
            Task t = ev.getItem();
            TaskReviewDialog dialog = new TaskReviewDialog(t, this.treatmentRepository, this.taskService,
                    this.authenticationContext, this.reportService, this.userAccountRepository,
                    this.applicationContext, this.treatmentPlanPresenter,
                    applicationContext.getBean(de.bbajor.pvs.taskmanagement.service.StandardRemarkService.class),
                    applicationContext.getBean(de.bbajor.pvs.taskmanagement.service.TreatmentRemarkService.class));
            dialog.open();
        });

        taskRefreshButton.setIcon(VaadinIcon.REFRESH.create());
        taskRefreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        taskRefreshButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        taskRefreshButton.addClickListener(e -> {
            try {
                ensureInstitutionContext();
                taskService.createDailyTaskIfAny();
                refreshTaskGrid();
                Notification.show("Aufgabenliste aktualisiert")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Fehler beim Aktualisieren der Aufgabenliste: " + ex.getMessage(),
                        5000,
                        Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        taskFilterText.setPlaceholder("Filter Beschreibung...");
        taskFilterText.setClearButtonVisible(true);
        taskToggleCompleted.setIcon(VaadinIcon.EYE.create());
        taskToggleCompleted.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        updateTaskToggleCompletedButton();
        taskToggleCompleted.addClickListener(e -> {
            hideCompleted = !hideCompleted;
            updateTaskToggleCompletedButton();
            refreshTaskGrid();
        });

        // Container als Flexbox konfigurieren - WICHTIG: VOR add() aufrufen
        taskReviewContent.setSizeFull();
        taskReviewContent.setPadding(false);
        taskReviewContent.setSpacing(false); // Spacing reduziert, damit Grid mehr Platz hat
        taskReviewContent.getStyle().set("display", "flex");
        taskReviewContent.getStyle().set("flex-direction", "column");
        taskReviewContent.getStyle().set("min-height", "0");

        // Section für Buttons und Suche (ohne Überschrift "Behandlungsprüfung")
        Div taskToolbarSection = createTaskToolbarSection();
        taskToolbarSection.getStyle().set("flex-shrink", "0");
        taskReviewContent.add(taskToolbarSection);

        // Grid - nimmt restlichen Platz ein und scrollt
        taskGrid.setSizeFull();
        taskReviewContent.add(taskGrid);

        refreshTaskGrid();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access treatment plan data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();

        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
        }
    }

    /**
     * Ensures InstitutionContext is set before service calls. This is necessary
     * because Vaadin button clicks don't trigger BeforeEnterEvent, so the
     * context might not be set.
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
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);

                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, userAccount.getInstitution().getInstitutionCode());
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
            }
        }
    }

    private void refreshTaskGrid() {
        // Ensure InstitutionContext is set before service call
        ensureInstitutionContext();

        taskGrid.setItems(query -> {
            // Ensure context is set again for each query (in case it was cleared)
            ensureInstitutionContext();

            Boolean completedFilter = hideCompleted ? Boolean.FALSE : null;
            List<Task> tasks = taskService.listByCompleted(completedFilter, toSpringPageRequest(query));

            // Debug logging and user notification for missing institution context
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                log.warn("No institution context set - cannot load tasks for user: {}",
                        authenticationContext.getPrincipalName().orElse("unknown"));
                // Show notification to user (only once, not on every query)
                if (query.getPage() == 0 && query.getPageSize() > 0) {
                    Notification.show(
                            "Kein Institution-Kontext gefunden. Bitte melden Sie sich erneut an oder kontaktieren Sie den Administrator.",
                            10000,
                            Notification.Position.MIDDLE
                    ).addThemeVariants(NotificationVariant.LUMO_WARNING);
                }
            } else {
                log.debug("Loading tasks for institution ID: {}, found {} tasks", institutionId, tasks.size());
            }

            return tasks.stream();
        });
    }

    private void navigateToDetailView(TreatmentPlan treatmentPlan) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        if (treatmentPlan.getId() != null && treatmentPlan.getId() == -1L) {
            UI.getCurrent().navigate("ivom/-1");
        } else {
            UI.getCurrent().navigate("ivom/" + treatmentPlan.getId());
        }
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

        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setSpacing(true);
        toolbarLayout.setWidthFull();
        toolbarLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);

        toolbarLayout.add(createButton, generateDailyListButton);
        toolbarLayout.add(toggleFinishedButton);
        toolbarLayout.add(searchField);
        toolbarLayout.setFlexGrow(1, searchField);

        section.add(toolbarLayout);
        return section;
    }

    /**
     * Aktualisiert den Text und den Enabled-Status des Toggle-Buttons für
     * abgeschlossene Pläne.
     */
    private void updateToggleFinishedButton() {
        if (showFinished) {
            toggleFinishedButton.setText("Abgeschlossene ausblenden");
        } else {
            toggleFinishedButton.setText("Abgeschlossene einblenden");
        }

        // Button disabled wenn kein Behandlungsplan angezeigt wird
        // Prüfe Item-Count über DataProvider
        int itemCount = ivomPlanGrid.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>());
        toggleFinishedButton.setEnabled(itemCount > 0);
    }

    /**
     * Aktualisiert den Text des Toggle-Buttons für abgeschlossene Dokumentationen.
     */
    private void updateTaskToggleCompletedButton() {
        if (hideCompleted) {
            taskToggleCompleted.setText("Abgeschlossene einblenden");
        } else {
            taskToggleCompleted.setText("Abgeschlossene ausblenden");
        }
    }

    private Div createTaskToolbarSection() {
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

        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setSpacing(true);
        toolbarLayout.setWidthFull();
        toolbarLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);

        toolbarLayout.add(taskRefreshButton, taskToggleCompleted);
        toolbarLayout.add(taskFilterText);
        toolbarLayout.setFlexGrow(1, taskFilterText);

        section.add(toolbarLayout);
        return section;
    }

    private void configureGrid() {
        // Nr. Spalte als erste Spalte
        java.util.concurrent.atomic.AtomicInteger rowCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            // Zeilennummer pro Seite (startet bei 1 auf jeder Seite)
            int rowNumber = rowCounter.incrementAndGet();
            Span span = new Span(String.valueOf(rowNumber));
            span.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            return span;
        })).setHeader("Nr.").setResizable(false).setAutoWidth(true).setFlexGrow(0);

        // Patientendaten-Renderer (eine Spalte) - wie im TaskReviewDialog
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            if (plan.getPatient() != null) {
                de.bbajor.pvs.patient.model.Patient patient = plan.getPatient();
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

        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String insurance = plan.getHealthInsurance() != null ? plan.getHealthInsurance() : "-";
            Span insuranceSpan = new Span(insurance);
            insuranceSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            insuranceSpan.getStyle().set("white-space", "normal");
            insuranceSpan.getStyle().set("word-wrap", "break-word");
            return insuranceSpan;
        })).setHeader("Krankenkasse").setResizable(true).setAutoWidth(false).setWidth("150px");

        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String diagnosis = plan.getDiagnosis() != null ? plan.getDiagnosis().getName() : "-";
            Span diagnosisSpan = new Span(diagnosis);
            diagnosisSpan.getStyle().set("white-space", "normal");
            diagnosisSpan.getStyle().set("word-wrap", "break-word");
            return diagnosisSpan;
        })).setHeader("Grund der Behandlung").setResizable(true).setAutoWidth(false).setWidth("180px");

        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String info = plan.getAdditionalInformation();
            if (info != null && !info.trim().isEmpty()) {
                Span infoSpan = new Span(info);
                infoSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                infoSpan.getStyle().set("white-space", "normal");
                infoSpan.getStyle().set("word-wrap", "break-word");
                return infoSpan;
            }
            return new Span("-");
        })).setHeader("Zusätzliche Informationen").setResizable(true).setAutoWidth(false).setWidth("200px");

        // Status-Spalte als letzte Spalte für Patienten mit zukünftigen Behandlungsterminen
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            if (plan.getPatient() != null) {
                try {
                    // Ensure InstitutionContext is set before checking appointments
                    ensureInstitutionContext();
                    boolean hasFutureAppointment = !appointmentService.findFutureAppointmentsByPatient(plan.getPatient()).isEmpty();
                    if (hasFutureAppointment) {
                        Span statusSpan = new Span();
                        statusSpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);

                        // Icon für zukünftigen Termin
                        com.vaadin.flow.component.icon.Icon icon = VaadinIcon.CALENDAR_CLOCK.create();
                        icon.setColor("var(--lumo-success-color)");
                        icon.getStyle().set("width", "var(--lumo-icon-size-m)");
                        icon.getStyle().set("height", "var(--lumo-icon-size-m)");

                        Span text = new Span("Termin");
                        text.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SUCCESS);

                        statusSpan.add(icon, text);
                        return statusSpan;
                    }
                } catch (Exception e) {
                    // Silently ignore errors (e.g., missing institution context)
                    log.debug("Could not check future appointments for patient: {}", e.getMessage());
                }
            }
            return new Span();
        })).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        
        // Spalte "Nächster Behandlungstermin" mit Button oder View mit geplanten Patienten
        // WICHTIG: Diese Spalte verwendet ComponentRenderer, daher muss das Grid setItemCountUnknown() haben
        Grid.Column<TreatmentPlan> nextAppointmentColumn = ivomPlanGrid.addComponentColumn(plan -> {
            try {
                ensureInstitutionContext();
                if (plan.getId() == null || plan.getId() == -1L) {
                    return new Span();
                }
                
                // Finde nächsten Termin für beide Augen
                java.time.LocalDate now = java.time.LocalDate.now();
                List<de.bbajor.pvs.intravitreal.treatment.model.Treatment> leftTreatments = 
                    treatmentPlanPresenter.getTreatmentDtos(de.bbajor.pvs.base.util.SideOfEye.LEFT, plan.getId());
                List<de.bbajor.pvs.intravitreal.treatment.model.Treatment> rightTreatments = 
                    treatmentPlanPresenter.getTreatmentDtos(de.bbajor.pvs.base.util.SideOfEye.RIGHT, plan.getId());
                
                de.bbajor.pvs.intravitreal.treatment.model.Treatment nextLeft = leftTreatments.stream()
                    .filter(t -> t.getDate() != null && t.getDate().isAfter(now) || t.getDate().isEqual(now))
                    .min((a, b) -> a.getDate().compareTo(b.getDate()))
                    .orElse(null);
                
                de.bbajor.pvs.intravitreal.treatment.model.Treatment nextRight = rightTreatments.stream()
                    .filter(t -> t.getDate() != null && t.getDate().isAfter(now) || t.getDate().isEqual(now))
                    .min((a, b) -> a.getDate().compareTo(b.getDate()))
                    .orElse(null);
                
                // Finde den nächsten Termin insgesamt
                de.bbajor.pvs.intravitreal.treatment.model.Treatment nextTreatment = null;
                if (nextLeft != null && nextRight != null) {
                    nextTreatment = nextLeft.getDate().isBefore(nextRight.getDate()) ? nextLeft : nextRight;
                } else if (nextLeft != null) {
                    nextTreatment = nextLeft;
                } else if (nextRight != null) {
                    nextTreatment = nextRight;
                }
                
                if (nextTreatment != null && nextTreatment.getSurgicalCenterTimeSlot() != null) {
                    // Finale Kopie für Lambda
                    final de.bbajor.pvs.intravitreal.treatment.model.Treatment finalNextTreatment = nextTreatment;
                    final de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot finalTimeSlot = finalNextTreatment.getSurgicalCenterTimeSlot();
                    
                    // Termin vorhanden: Button zum Öffnen der Terminübersicht
                    Button overviewButton = new Button("Terminübersicht", VaadinIcon.LIST.create(), e -> {
                        AppointmentOverviewDialog dialog = new AppointmentOverviewDialog(
                                finalTimeSlot, treatmentRepository);
                        dialog.open();
                    });
                    overviewButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                    overviewButton.setWidth("fit-content");
                    return overviewButton;
                } else {
                    // Kein Termin: Zeige nur "-"
                    return new Span("-");
                }
            } catch (Exception e) {
                log.debug("Fehler beim Laden des nächsten Behandlungstermins", e);
                return new Span();
            }
        });
        nextAppointmentColumn.setHeader("Nächster Behandlungstermin");
        nextAppointmentColumn.setAutoWidth(false);
        nextAppointmentColumn.setWidth("180px");
        nextAppointmentColumn.setFlexGrow(0);
        nextAppointmentColumn.setResizable(false);

        // Graue Hinterlegung für abgeschlossene Pläne
        ivomPlanGrid.setPartNameGenerator(plan -> {
            if (plan.getFinishedDate() != null) {
                return "finished";
            }
            return "";
        });

        // CSS für graue Hinterlegung
        ivomPlanGrid.getElement().executeJs(
                "const style = document.createElement('style');"
                + "style.textContent = '"
                + "vaadin-grid::part(row)::part(finished) { background-color: #e0e0e0 !important; } "
                + "vaadin-grid::part(cell)::part(finished) { background-color: #e0e0e0 !important; }"
                + "';"
                + "document.head.appendChild(style);"
        );
        
        // Zeilenumbruch in Zellen aktivieren und Grid-Breite begrenzen
        ivomPlanGrid.getStyle().set("--vaadin-grid-cell-content-overflow", "visible");
        ivomPlanGrid.setWidthFull();
        ivomPlanGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        ivomPlanGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);
        ivomPlanGrid.asSingleSelect().addValueChangeListener(event -> {
            TreatmentPlan ivomDto = event.getValue();
            if (ivomDto != null) {
                navigateToDetailView(ivomDto);
            }
        });

        // Paging aktivieren
        ivomPlanGrid.setPageSize(20);
        
        ivomPlanGrid.setItems(query -> {
            // Counter beim Start jeder Query zurücksetzen
            rowCounter.set(0);
            ensureInstitutionContext();
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return java.util.stream.Stream.empty();
            }
            String searchTerm = searchField.getValue();

            // Konvertiere zu Spring PageRequest
            org.springframework.data.domain.Pageable pageable = toSpringPageRequest(query);

            // Wenn keine Sortierung in der Query ist, füge Standard-Sortierung nach patient.lastName und patient.firstName hinzu
            if (query.getSortOrders().isEmpty()) {
                org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.asc("patient.lastName"),
                        org.springframework.data.domain.Sort.Order.asc("patient.firstName")
                );
                pageable = org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        sort
                );
            }

            java.util.stream.Stream<TreatmentPlan> stream;
            if (searchTerm == null || searchTerm.isEmpty()) {
                stream = ivomListPresenter.findAll(pageable).getContent().stream();
            } else {
                stream = ivomListPresenter.findAllBy(searchTerm, pageable).getContent().stream();
            }

            // Filtere abgeschlossene Pläne, falls ausgeschaltet
            if (!showFinished) {
                stream = stream.filter(plan -> plan.getFinishedDate() == null);
            }

            return stream;
        });
        
        // Count-Callback: Da der Presenter ein Slice (nicht Page) zurückgibt, können wir keine exakte Anzahl bestimmen.
        // Wir verwenden eine Schätzung: Wenn die aktuelle Seite voll ist (20 Items), schätzen wir, dass es mehr gibt.
        // Dies verhindert den Fehler "Trying to use exact size with a lazy loading component".
        ivomPlanGrid.getLazyDataView().setItemCountCallback(query -> {
            ensureInstitutionContext();
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return 0;
            }
            
            // Verwende die PageSize des Grids statt query.getLimit(), da getLimit() negativ sein kann
            int pageSize = Math.max(1, ivomPlanGrid.getPageSize());
            int offset = Math.max(0, query.getOffset());
            
            String searchTerm = searchField.getValue();
            
            // Lade eine Seite, um zu sehen, ob es mehr Items gibt
            // Verwende PageSize statt query.getLimit() um negative Werte zu vermeiden
            org.springframework.data.domain.Pageable testPageable = org.springframework.data.domain.PageRequest.of(0, pageSize);
            org.springframework.data.domain.Slice<TreatmentPlan> slice;
            if (searchTerm == null || searchTerm.isEmpty()) {
                slice = ivomListPresenter.findAll(testPageable);
            } else {
                slice = ivomListPresenter.findAllBy(searchTerm, testPageable);
            }
            
            // Wenn die Seite voll ist und es eine nächste Seite gibt, schätze eine große Anzahl
            // Ansonsten verwende die tatsächliche Anzahl der Items
            int currentPageSize = slice.getContent().size();
            if (slice.hasNext() && currentPageSize == pageSize) {
                // Schätze: mindestens (aktuelle Seite + 1) * PageSize
                // Verwende eine große Zahl, um sicherzustellen, dass Pagination funktioniert
                int currentPage = offset / pageSize;
                return (currentPage + 2) * pageSize;
            } else {
                // Letzte Seite oder weniger Items: exakte Anzahl
                return offset + currentPageSize;
            }
        });
    }

    private void configureSearch() {
        // Suche bei jeder Änderung im Suchfeld
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
        // Zusätzlich bei KeyUp für sofortige Reaktion während der Eingabe
        searchField.addKeyUpListener(e -> refresh(searchField.getValue()));
    }

    public void refresh(String searchString) {
        // Ensure InstitutionContext is set before service call
        ensureInstitutionContext();

        // Zeige ProgressBar während des Refreshs (nur wenn Grid bereits initialisiert wurde)
        if (isGridInitialized) {
            gridLoadingProgressBar.setVisible(true);
            ivomPlanGrid.setVisible(false);
            
            // Nach kurzer Verzögerung ProgressBar ausblenden und Grid einblenden
            // Dies gibt dem Grid Zeit zum Rendern
            UI.getCurrent().access(() -> {
                UI.getCurrent().setPollInterval(100);
                UI.getCurrent().getPage().executeJs(
                    "setTimeout(function() { $0.$server.hideProgressBarAfterRefresh(); }, 300);",
                    getElement()
                );
            });
        }

        // Verwende refreshAll() statt setItems(), um Paging zu erhalten
        ivomPlanGrid.getDataProvider().refreshAll();

        // Aktualisiere Toggle-Button Status nach Refresh
        updateToggleFinishedButton();
    }

    /**
     * Wird von JavaScript aufgerufen, um die ProgressBar nach dem ersten Load auszublenden.
     */
    @com.vaadin.flow.component.ClientCallable
    private void hideProgressBarAfterLoad() {
        if (!isGridInitialized) {
            gridLoadingProgressBar.setVisible(false);
            ivomPlanGrid.setVisible(true);
            isGridInitialized = true;
        }
    }

    /**
     * Wird von JavaScript aufgerufen, um die ProgressBar nach dem Refresh auszublenden.
     */
    @com.vaadin.flow.component.ClientCallable
    private void hideProgressBarAfterRefresh() {
        gridLoadingProgressBar.setVisible(false);
        ivomPlanGrid.setVisible(true);
    }

    @Override
    public void onTreatmentPlanChanged() {
        refresh("");
    }

    /**
     * Öffnet den Dialog für die Wochenliste mit der aktuellen Woche
     */
    private void openWeekListDialog() {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        LocalDate monday = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = monday.plusDays(6);
        WeekListConfig config = new WeekListConfig(ivomListPresenter.generateWeekList(monday), monday, endOfWeek);
        WeekListDialog weekListDialog = new WeekListDialog(config, applicationContext, ivomListPresenter);
        weekListDialog.open();
    }
    
}
