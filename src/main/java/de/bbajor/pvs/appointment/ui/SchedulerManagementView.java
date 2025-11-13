package de.bbajor.pvs.appointment.ui;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.model.SchedulerAssignment;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.appointment.service.OfficeHoursService;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.annotation.security.RolesAllowed;

/**
 * View for managing appointment schedulers, office hours, and assignments.
 * Admin/Owner only view.
 */
@Route("scheduler-management")
@PageTitle("Terminplaner-Verwaltung")
// Menu entry removed - now available in Settings > Terminplaner
@RolesAllowed({"ADMIN", "OWNER"})
public class SchedulerManagementView extends Main {

    private final AppointmentSchedulerService schedulerService;
    private final OfficeHoursService officeHoursService;
    private final LocationService locationService;
    private final UserAccountRepository userAccountRepository;

    private Grid<AppointmentScheduler> schedulerGrid;
    private Grid<OfficeHours> officeHoursGrid;
    private Grid<SchedulerAssignment> assignmentGrid;

    private VerticalLayout contentLayout;
    private AppointmentScheduler selectedScheduler;

    public SchedulerManagementView(
            AppointmentSchedulerService schedulerService,
            OfficeHoursService officeHoursService,
            LocationService locationService,
            UserAccountRepository userAccountRepository) {
        this.schedulerService = schedulerService;
        this.officeHoursService = officeHoursService;
        this.locationService = locationService;
        this.userAccountRepository = userAccountRepository;

        addClassNames(
            LumoUtility.BoxSizing.BORDER, 
            LumoUtility.Display.FLEX, 
            LumoUtility.FlexDirection.COLUMN,
            LumoUtility.Padding.MEDIUM, 
            LumoUtility.Gap.SMALL
        );
        setSizeFull();

        initializeView();
    }

    private void initializeView() {
        Button newSchedulerButton = new Button("Neuer Terminplaner", event -> openSchedulerDialog());
        newSchedulerButton.setIcon(VaadinIcon.PLUS.create());
        newSchedulerButton.getElement().setAttribute("theme", "primary");

        add(new ViewToolbar("Terminplaner-Verwaltung", ViewToolbar.group(newSchedulerButton)));

        Tabs tabs = createTabs();
        add(tabs);

        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(false);
        add(contentLayout);

        showSchedulersTab();
    }

    private Tabs createTabs() {
        Tab schedulersTab = new Tab("Terminplaner");
        Tab officeHoursTab = new Tab("Sprechzeiten");
        Tab assignmentsTab = new Tab("Zuordnungen");

        Tabs tabs = new Tabs(schedulersTab, officeHoursTab, assignmentsTab);
        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab == schedulersTab) {
                showSchedulersTab();
            } else if (selectedTab == officeHoursTab) {
                showOfficeHoursTab();
            } else if (selectedTab == assignmentsTab) {
                showAssignmentsTab();
            }
        });

        return tabs;
    }

    private void showSchedulersTab() {
        contentLayout.removeAll();

        schedulerGrid = new Grid<>(AppointmentScheduler.class, false);
        schedulerGrid.addColumn(AppointmentScheduler::getName).setHeader("Name");
        schedulerGrid.addColumn(AppointmentScheduler::getDescription).setHeader("Beschreibung");
        schedulerGrid.addColumn(AppointmentScheduler::getType).setHeader("Typ");
        schedulerGrid.addColumn(scheduler -> 
            scheduler.getLocation() != null ? scheduler.getLocation().getLocationName() : "Kein Standort")
            .setHeader("Standort");
        schedulerGrid.addColumn(scheduler -> scheduler.isActive() ? "Aktiv" : "Inaktiv")
            .setHeader("Status");

        schedulerGrid.setItems(schedulerService.findAll());
        schedulerGrid.setSizeFull();

        schedulerGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedScheduler = event.getValue();
            if (selectedScheduler != null) {
                openSchedulerDialog(selectedScheduler);
            }
        });

        contentLayout.add(schedulerGrid);
    }

    private void showOfficeHoursTab() {
        contentLayout.removeAll();

        if (selectedScheduler == null) {
            List<AppointmentScheduler> schedulers = schedulerService.findAll();
            if (!schedulers.isEmpty()) {
                selectedScheduler = schedulers.get(0);
            }
        }

        if (selectedScheduler == null) {
            contentLayout.add(new Button("Bitte legen Sie zuerst einen Terminplaner an", 
                event -> showSchedulersTab()));
            return;
        }

        Button newOfficeHoursButton = new Button("Neue Sprechzeit", event -> openOfficeHoursDialog());
        newOfficeHoursButton.setIcon(VaadinIcon.CLOCK.create());
        newOfficeHoursButton.getElement().setAttribute("theme", "primary");

        officeHoursGrid = new Grid<>(OfficeHours.class, false);
        officeHoursGrid.addColumn(oh -> oh.getDayOfWeek().name()).setHeader("Wochentag");
        officeHoursGrid.addColumn(OfficeHours::getStartTime).setHeader("Von");
        officeHoursGrid.addColumn(OfficeHours::getEndTime).setHeader("Bis");
        officeHoursGrid.addColumn(OfficeHours::getSlotDurationMinutes)
            .setHeader("Slot-Dauer (Min)");
        officeHoursGrid.addColumn(oh -> oh.isActive() ? "Aktiv" : "Inaktiv")
            .setHeader("Status");

        officeHoursGrid.setItems(officeHoursService.findByScheduler(selectedScheduler));
        officeHoursGrid.setSizeFull();

        officeHoursGrid.asSingleSelect().addValueChangeListener(event -> {
            OfficeHours selected = event.getValue();
            if (selected != null) {
                openOfficeHoursDialog(selected);
            }
        });

        contentLayout.add(newOfficeHoursButton, officeHoursGrid);
    }

    private void showAssignmentsTab() {
        contentLayout.removeAll();

        if (selectedScheduler == null) {
            List<AppointmentScheduler> schedulers = schedulerService.findAll();
            if (!schedulers.isEmpty()) {
                selectedScheduler = schedulers.get(0);
            }
        }

        if (selectedScheduler == null) {
            contentLayout.add(new Button("Bitte legen Sie zuerst einen Terminplaner an", 
                event -> showSchedulersTab()));
            return;
        }

        Button newAssignmentButton = new Button("Neue Zuordnung", event -> openAssignmentDialog());
        newAssignmentButton.setIcon(VaadinIcon.USER.create());
        newAssignmentButton.getElement().setAttribute("theme", "primary");

        assignmentGrid = new Grid<>(SchedulerAssignment.class, false);
        assignmentGrid.addColumn(assignment -> 
            assignment.getUserAccount() != null ? 
                assignment.getUserAccount().getFullName() : "Rolle: " + assignment.getRole()
        ).setHeader("Zugeordnet zu");

        assignmentGrid.setItems(schedulerService.getAssignments(selectedScheduler));
        assignmentGrid.setSizeFull();

        contentLayout.add(newAssignmentButton, assignmentGrid);
    }

    private void openSchedulerDialog() {
        SchedulerDialog dialog = new SchedulerDialog(
            schedulerService, 
            locationService, 
            null,
            userAccountRepository
        );
        dialog.setOnSaveCallback(this::showSchedulersTab);
        dialog.open();
    }

    private void openSchedulerDialog(AppointmentScheduler scheduler) {
        SchedulerDialog dialog = new SchedulerDialog(
            schedulerService, 
            locationService, 
            scheduler,
            userAccountRepository
        );
        dialog.setOnSaveCallback(this::showSchedulersTab);
        dialog.open();
    }

    private void openOfficeHoursDialog() {
        OfficeHoursDialog dialog = new OfficeHoursDialog(
            officeHoursService, 
            selectedScheduler, 
            null
        );
        dialog.setOnSaveCallback(this::showOfficeHoursTab);
        dialog.open();
    }

    private void openOfficeHoursDialog(OfficeHours officeHours) {
        OfficeHoursDialog dialog = new OfficeHoursDialog(
            officeHoursService, 
            selectedScheduler, 
            officeHours
        );
        dialog.setOnSaveCallback(this::showOfficeHoursTab);
        dialog.open();
    }

    private void openAssignmentDialog() {
        // TODO: Implement assignment dialog
        showNotification("Dialog zum Zuordnen von Benutzern/Rollen", NotificationVariant.LUMO_PRIMARY);
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(variant);
    }
}
