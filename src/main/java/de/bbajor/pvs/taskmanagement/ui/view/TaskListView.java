package de.bbajor.pvs.taskmanagement.ui.view;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import jakarta.annotation.security.PermitAll;

@Route("aufgabenliste")
@PageTitle("Zurückliegende Behandlungen die noch überprüft werden müssen")
@Menu(order = 0, icon = "vaadin:clipboard-check", title = "Zu überprüfende Behandlungen")
@PermitAll
public class TaskListView extends Main {

    private final TaskService taskService;
    final TextField description;
    final DatePicker dueDate;
    final Grid<Task> taskGrid;

    public TaskListView(TaskService taskService, Clock clock) {
        this.taskService = taskService;

        description = new TextField();
        description.setPlaceholder("Was möchten Sie erledigen?");
        description.setAriaLabel("Beschreibung der Aufgabe");
        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("20em");

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Fälligkeitsdatum");
        dueDate.setAriaLabel("Fälligkeitsdatum der Aufgabe");

        var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(clock.getZone())
                .withLocale(getLocale());
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        taskGrid = new Grid<>();
        taskGrid.setItems(query -> taskService.list(toSpringPageRequest(query)).stream());
        taskGrid.addColumn(Task::getDescription).setHeader("Beschreibung");
        taskGrid.addColumn(task -> Optional.ofNullable(task.getDueDate()).map(dateFormatter::format).orElse("Nie"))
                .setHeader("Fälligkeitsdatum");
        taskGrid.addColumn(task -> dateTimeFormatter.format(task.getCreationDate())).setHeader("Erstellt am");
        taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        taskGrid.setSizeFull();
        taskGrid.getStyle().set("min-height", "10em");

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Aufgabenliste", ViewToolbar.group(description, dueDate)));
        add(taskGrid);
    }

}
