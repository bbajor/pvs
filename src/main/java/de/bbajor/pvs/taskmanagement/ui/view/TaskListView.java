package de.bbajor.pvs.taskmanagement.ui.view;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.spring.security.AuthenticationContext;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;
import jakarta.annotation.security.PermitAll;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("aufgabenliste")
@PageTitle("Zurückliegende Behandlungen die noch überprüft werden müssen")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Zu überprüfende Behandlungen")
@PermitAll
public class TaskListView extends Main implements BeforeEnterObserver {

        private static final Logger log = LoggerFactory.getLogger(TaskListView.class);

        private final TaskService taskService;
        private final TreatmentRepository treatmentRepository;
        private final AuthenticationContext authenticationContext;
        private final TreatmentReportService reportService;
        private final UserAccountRepository userAccountRepository;
        private final Button refreshButton;
        final TextField description;
        final DatePicker dueDate;
        final Grid<Task> taskGrid;
        final TextField filterText;
        final Button toggleCompleted;
        private boolean hideCompleted = false;

        public TaskListView(TaskService taskService, TreatmentRepository treatmentRepository, AuthenticationContext authenticationContext, 
                TreatmentReportService reportService, UserAccountRepository userAccountRepository, Clock clock) {
                this.taskService = taskService;
                this.treatmentRepository = treatmentRepository;
                this.authenticationContext = authenticationContext;
                this.reportService = reportService;
                this.userAccountRepository = userAccountRepository;

                description = new TextField();
                description.setPlaceholder("Was möchten Sie erledigen?");
                description.setAriaLabel("Beschreibung der Aufgabe");
                description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
                description.setMinWidth("20em");

                dueDate = new DatePicker();
                dueDate.setPlaceholder("Fälligkeitsdatum");
                dueDate.setAriaLabel("Fälligkeitsdatum der Aufgabe");

                var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                .withZone(clock.getZone())
                                .withLocale(getLocale());
                var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

                taskGrid = new Grid<>();
                taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
                taskGrid.addColumn(new ComponentRenderer<>(task -> {
                                Span s = new Span(task.getDescription());
                                if (task.isCompleted()) {
                                        s.getStyle().set("opacity", "0.6");
                                }
                                return s;
                        })).setHeader("Beschreibung");
                taskGrid.addColumn(task -> Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Nie"))
                                .setHeader("Fälligkeitsdatum");
                taskGrid.addColumn(task -> dateTimeFormatter.format(task.getCreationDate())).setHeader("Erstellt am");
                taskGrid.setPartNameGenerator(task -> task.isCompleted() ? "row-completed" : "");
                taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
                taskGrid.setSizeFull();
                taskGrid.getStyle().set("min-height", "10em");
                taskGrid.addItemDoubleClickListener(ev -> {
                        Task t = ev.getItem();
                        
                        // All authenticated users can open the dialog (ADMIN can view and generate reports)
                        // But only MEDICAL_STAFF, OWNER, DOCTOR can start review
                        TaskReviewDialog dialog = new TaskReviewDialog(t, this.treatmentRepository, this.taskService,
                                        this.authenticationContext, this.reportService, this.userAccountRepository);
                        dialog.open();
                });

                refreshButton = new Button("Aktualisieren");
                refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                refreshButton.addClickListener(e -> {
                        try {
                                // Ensure InstitutionContext is set before service call
                                ensureInstitutionContext();
                                taskService.createDailyTaskIfAny();
                                taskGrid.setItems(query -> {
                                        // Ensure context is set again for each query
                                        ensureInstitutionContext();
                                        return taskService.list(toSpringPageRequest(query)).stream();
                                });
                                Notification.show("Aufgabenliste aktualisiert")
                                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        } catch (Exception ex) {
                                Notification.show("Fehler beim Aktualisieren der Aufgabenliste: " + ex.getMessage(),
                                                5000,
                                                Notification.Position.MIDDLE)
                                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                });

                filterText = new TextField();
                filterText.setPlaceholder("Filter Beschreibung...");
                toggleCompleted = new Button("Abgeschlossene ein-/ausblenden");
                toggleCompleted.addClickListener(e -> {
                        hideCompleted = !hideCompleted;
                        refreshGrid();
                });

                setSizeFull();
                addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

                Component controls = ViewToolbar.group(description, dueDate, filterText, toggleCompleted, refreshButton);
                add(new ViewToolbar("Aufgabenliste", controls));
                add(taskGrid);
                refreshGrid();
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
                // SUPER_ADMIN without institution context should not access task data
                // All other authenticated users (including ADMIN) with institution context can access
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
                boolean hasInstitutionContext = InstitutionContext.hasInstitution();
                Long institutionId = InstitutionContext.getInstitutionId();
                
                // Debug logging
                if (auth != null) {
                        log.debug("TaskListView.beforeEnter - User: {}, isSuperAdmin: {}, hasInstitutionContext: {}, institutionId: {}", 
                                auth.getName(), isSuperAdmin, hasInstitutionContext, institutionId);
                }
                
                // Only redirect SUPER_ADMIN without institution context
                // All other users (including ADMIN with institution context) can proceed
                if (isSuperAdmin && !hasInstitutionContext) {
                        // Redirect SUPER_ADMIN to institution management
                        log.debug("Redirecting SUPER_ADMIN without institution context to admin/institutions");
                        event.forwardTo("admin/institutions");
                }
                // All other cases (including ADMIN with institution context) are allowed
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

        private void refreshGrid() {
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

}
