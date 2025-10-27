package de.bbajor.pvs.taskmanagement.ui.view;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import jakarta.annotation.security.PermitAll;

@Route("aufgabenliste")
@PageTitle("Zurückliegende Behandlungen die noch überprüft werden müssen")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Zu überprüfende Behandlungen")
@PermitAll
public class TaskListView extends Main {

        private final TaskService taskService;
        private final TreatmentRepository treatmentRepository;
        private final AuthenticationContext authenticationContext;
        private final Button refreshButton;
        final TextField description;
        final DatePicker dueDate;
        final Grid<Task> taskGrid;
        final TextField filterText;
        final Button toggleCompleted;
        private boolean hideCompleted = false;

        public TaskListView(TaskService taskService, TreatmentRepository treatmentRepository, AuthenticationContext authenticationContext, Clock clock) {
                this.taskService = taskService;
                this.treatmentRepository = treatmentRepository;
                this.authenticationContext = authenticationContext;

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
                taskGrid.addClassNameGenerator(task -> task.isCompleted() ? "row-completed" : "");
                taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
                taskGrid.setSizeFull();
                taskGrid.getStyle().set("min-height", "10em");
                taskGrid.addItemDoubleClickListener(ev -> {
                        Task t = ev.getItem();
                        TaskReviewDialog dialog = new TaskReviewDialog(t, this.treatmentRepository, this.taskService,
                                        this.authenticationContext);
                        dialog.open();
                });

                refreshButton = new Button("Aktualisieren");
                refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                refreshButton.addClickListener(e -> {
                        try {
                                taskService.createDailyTaskIfAny();
                                taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
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

                HorizontalLayout controls = ViewToolbar.group(description, dueDate, filterText, toggleCompleted, refreshButton);
                add(new ViewToolbar("Aufgabenliste", controls,
                                ViewToolbar.group(taskGrid)));
                add(taskGrid);
                refreshGrid();
        }

        private void refreshGrid() {
                taskGrid.setItems(query -> {
                        Boolean completedFilter = hideCompleted ? Boolean.FALSE : null;
                        return taskService.listByCompleted(completedFilter, toSpringPageRequest(query)).stream();
                });
        }

}
