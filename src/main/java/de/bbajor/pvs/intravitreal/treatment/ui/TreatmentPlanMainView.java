package de.bbajor.pvs.intravitreal.treatment.ui;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
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
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Treatment Plans Tab Components
    private final TextField searchField = new TextField();
    private final Button createButton = new Button(VaadinIcon.PLUS.create());
    private final Button generateDailyListButton = new Button("Wochenliste anzeigen");
    private final Grid<TreatmentPlan> ivomPlanGrid = new Grid<>(TreatmentPlan.class, false);
    private final VerticalLayout treatmentPlansContent = new VerticalLayout();

    // Task Review Tab Components
    private final Grid<Task> taskGrid = new Grid<>();
    private final TextField taskFilterText = new TextField();
    private final Button taskToggleCompleted = new Button("Abgeschlossene ein-/ausblenden");
    private final Button taskRefreshButton = new Button("Aktualisieren");
    private boolean hideCompleted = false;
    private final VerticalLayout taskReviewContent = new VerticalLayout();

    // Tab Components
    private final Tab treatmentPlansTab = new Tab("Behandlungspläne");
    private final Tab taskReviewTab = new Tab("Behandlungsprüfung");
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
            TreatmentPlanPresenter treatmentPlanPresenter) {
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
        tabContent.setPadding(false);
        tabContent.setSpacing(false);

        add(tabs);
        add(tabContent);

        // Show treatment plans tab by default
        tabs.setSelectedTab(treatmentPlansTab);
        tabContent.add(treatmentPlansContent);

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private void initializeTreatmentPlansTab() {
        createButton.setText("Neuer Behandlungsplan");
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
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        
        // Button nur für berechtigte Rollen aktivieren
        boolean canBook = currentUser.getPrincipal()
                .map(principal -> {
                    return principal.getAuthorities().stream()
                            .anyMatch(auth -> {
                                String authority = auth.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.ADMIN) ||
                                        authority.equals("ROLE_" + AppRoles.DOCTOR) ||
                                        authority.equals("ROLE_" + AppRoles.TECH_USER);
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

        generateDailyListButton.addClickListener(event -> {
            LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate endOfWeek = monday.plusDays(6);
            ensureInstitutionContext();
            WeekListConfig config = new WeekListConfig(ivomListPresenter.generateWeekList(monday), monday,
                    endOfWeek);
            WeekListDialog dailyTreatmentsDialog = new WeekListDialog(config, applicationContext);
            dailyTreatmentsDialog.open();
        });
        generateDailyListButton.setIcon(VaadinIcon.FILE_TEXT.create());
        generateDailyListButton
                .setTooltipText("Erzeugt eine Tagesliste für die tagesaktuellen OP-Slots. " +
                        "Dabei werden die zu behandelnden Patienten, sowie die jeweiligen " +
                        "Einrichtungen aufgelistet, an denen die Behandlung stattfindet.");
        generateDailyListButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateDailyListButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        // Professionelle Button-Anordnung - Business-App Style
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        buttonLayout.add(createButton, generateDailyListButton);
        buttonLayout.setFlexGrow(1, new Div()); // Spacer
        
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchLayout.setSpacing(true);
        searchLayout.setWidthFull();
        searchLayout.add(searchField);
        searchLayout.setFlexGrow(1, searchField);
        
        VerticalLayout toolbarContent = new VerticalLayout();
        toolbarContent.setSpacing(true);
        toolbarContent.setPadding(false);
        toolbarContent.add(buttonLayout, searchLayout);
        
        treatmentPlansContent.add(toolbarContent);
        treatmentPlansContent.add(ivomPlanGrid);
        treatmentPlansContent.setSizeFull();
        treatmentPlansContent.setPadding(false);
        treatmentPlansContent.setSpacing(true);

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
            return s;
        })).setHeader("Beschreibung").setAutoWidth(true);
        
        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            String dueDate = Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Nie");
            Span dateSpan = new Span(dueDate);
            if (task.getDueDate() != null && task.getDueDate().isBefore(java.time.LocalDate.now()) && !task.isCompleted()) {
                dateSpan.addClassNames(LumoUtility.TextColor.ERROR);
            } else {
                dateSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            }
            return dateSpan;
        })).setHeader("Fälligkeitsdatum").setAutoWidth(true);
        
        taskGrid.addColumn(new ComponentRenderer<>(task -> {
            Span dateSpan = new Span(dateTimeFormatter.format(task.getCreationDate()));
            dateSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            return dateSpan;
        })).setHeader("Erstellt am").setAutoWidth(true);
        
        taskGrid.setPartNameGenerator(task -> task.isCompleted() ? "row-completed" : "");
        taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        taskGrid.setSizeFull();
        taskGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        taskGrid.getStyle().set("min-height", "10em");
        taskGrid.addItemDoubleClickListener(ev -> {
            Task t = ev.getItem();
            TaskReviewDialog dialog = new TaskReviewDialog(t, this.treatmentRepository, this.taskService,
                    this.authenticationContext, this.reportService, this.userAccountRepository,
                    this.applicationContext, this.treatmentPlanPresenter);
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
        taskToggleCompleted.addClickListener(e -> {
            hideCompleted = !hideCompleted;
            refreshTaskGrid();
        });

        // Moderne Anordnung für Task Review Tab
        HorizontalLayout taskButtonLayout = new HorizontalLayout();
        taskButtonLayout.setSpacing(true);
        taskButtonLayout.setWidthFull();
        taskButtonLayout.add(taskRefreshButton, taskToggleCompleted);
        taskButtonLayout.setFlexGrow(1, new Div()); // Spacer
        
        HorizontalLayout taskSearchLayout = new HorizontalLayout();
        taskSearchLayout.setSpacing(true);
        taskSearchLayout.setWidthFull();
        taskSearchLayout.add(taskFilterText);
        taskSearchLayout.setFlexGrow(1, taskFilterText);
        
        VerticalLayout taskToolbarContent = new VerticalLayout();
        taskToolbarContent.setSpacing(true);
        taskToolbarContent.setPadding(false);
        taskToolbarContent.add(taskButtonLayout, taskSearchLayout);
        
        taskReviewContent.add(new ViewToolbar("Behandlungsprüfung",
                taskToolbarContent));
        taskReviewContent.add(taskGrid);
        taskReviewContent.setSizeFull();
        taskReviewContent.setPadding(false);
        taskReviewContent.setSpacing(false);

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
        UI.getCurrent().navigate("ivom/" + treatmentPlan.getId());
    }

    private void configureGrid() {
        // Verbesserte Grid-Spalten mit ComponentRenderer für bessere Darstellung
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            Span name = new Span(plan.getLastName() != null ? plan.getLastName() : "");
            name.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            return name;
        })).setHeader("Nachname").setAutoWidth(true);
        
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            Span name = new Span(plan.getFirstName() != null ? plan.getFirstName() : "");
            return name;
        })).setHeader("Vorname").setAutoWidth(true);
        
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            if (plan.getBirth() != null) {
                Span date = new Span(DateAndTimeUtils.getGermanDateTimeFormatter().format(plan.getBirth()));
                date.addClassNames(LumoUtility.TextColor.SECONDARY);
                return date;
            }
            return new Span("-");
        })).setHeader("Geburtsdatum").setAutoWidth(true);
        
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String insurance = plan.getHealthInsurance() != null ? plan.getHealthInsurance() : "-";
            Span insuranceSpan = new Span(insurance);
            insuranceSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            return insuranceSpan;
        })).setHeader("Krankenkasse").setAutoWidth(true);
        
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String diagnosis = plan.getDiagnosis() != null ? plan.getDiagnosis().getName() : "-";
            Span diagnosisSpan = new Span(diagnosis);
            return diagnosisSpan;
        })).setHeader("Grund der Behandlung").setAutoWidth(true);
        
        ivomPlanGrid.addColumn(new ComponentRenderer<>(plan -> {
            String info = plan.getAdditionalInformation();
            if (info != null && !info.trim().isEmpty()) {
                Span infoSpan = new Span(info.length() > 50 ? info.substring(0, 50) + "..." : info);
                infoSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                return infoSpan;
            }
            return new Span("-");
        })).setHeader("Zusätzliche Informationen").setAutoWidth(true);
        
        ivomPlanGrid.setSizeFull();
        ivomPlanGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        ivomPlanGrid.asSingleSelect().addValueChangeListener(event -> {
            TreatmentPlan ivomDto = event.getValue();
            if (ivomDto != null) {
                navigateToDetailView(ivomDto);
            }
        });
        
        // Ensure InstitutionContext is set before loading initial data
        ensureInstitutionContext();
        ivomPlanGrid.setItems(ivomListPresenter.findAll());
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
        
        List<TreatmentPlan> ivomList = ivomListPresenter.findAllBy(searchString);
        ivomPlanGrid.setItems(ivomList);
    }

    @Override
    public void onTreatmentPlanChanged() {
        refresh("");
    }
}