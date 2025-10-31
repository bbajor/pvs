package de.bbajor.pvs.appointment.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.appointment.service.AppointmentService;
import de.bbajor.pvs.appointment.service.OfficeHoursService;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import jakarta.annotation.security.PermitAll;

/**
 * Calendar view for displaying and managing appointments.
 * Shows a day view with time slots and appointments.
 */
@Route("appointment-calendar")
@PageTitle("Terminkalender")
@Menu(order = 2, icon = "vaadin:calendar", title = "Terminkalender")
@PermitAll
public class AppointmentCalendarView extends Main {

    private final AppointmentSchedulerService schedulerService;
    private final AppointmentService appointmentService;
    private final OfficeHoursService officeHoursService;

    private AppointmentScheduler currentScheduler;
    private LocalDate currentDate;
    private Div calendarContainer;
    private Span schedulerNameLabel;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public AppointmentCalendarView(
            AppointmentSchedulerService schedulerService,
            AppointmentService appointmentService,
            OfficeHoursService officeHoursService) {
        this.schedulerService = schedulerService;
        this.appointmentService = appointmentService;
        this.officeHoursService = officeHoursService;
        this.currentDate = LocalDate.now();

        addClassNames(
            LumoUtility.BoxSizing.BORDER, 
            LumoUtility.Display.FLEX, 
            LumoUtility.FlexDirection.COLUMN,
            LumoUtility.Padding.MEDIUM, 
            LumoUtility.Gap.SMALL
        );
        setSizeFull();

        // Initialize with first scheduler if available
        List<AppointmentScheduler> schedulers = schedulerService.findAll();
        if (!schedulers.isEmpty()) {
            currentScheduler = schedulers.get(0);
        }

        initializeView();
    }

    private void initializeView() {
        Button newAppointmentButton = new Button("Neuer Termin", event -> openAppointmentDialog());
        newAppointmentButton.setIcon(VaadinIcon.CALENDAR.create());
        newAppointmentButton.getElement().setAttribute("theme", "primary");

        schedulerNameLabel = new Span(currentScheduler != null ? currentScheduler.getName() : "Kein Terminplaner");
        schedulerNameLabel.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "1.2em")
            .set("color", "var(--lumo-primary-color)");

        add(new ViewToolbar(
            "Terminkalender", 
            ViewToolbar.group(schedulerNameLabel, newAppointmentButton)
        ));

        add(createDateNavigation());

        calendarContainer = new Div();
        calendarContainer.setSizeFull();
        add(calendarContainer);

        refreshCalendar();
    }

    private Component createDateNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setWidthFull();
        navigation.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        navigation.setAlignItems(HorizontalLayout.Alignment.CENTER);

        Button previousDay = new Button(VaadinIcon.ANGLE_LEFT.create(), event -> {
            currentDate = currentDate.minusDays(1);
            refreshCalendar();
        });

        Button today = new Button("Heute", event -> {
            currentDate = LocalDate.now();
            refreshCalendar();
        });

        Button nextDay = new Button(VaadinIcon.ANGLE_RIGHT.create(), event -> {
            currentDate = currentDate.plusDays(1);
            refreshCalendar();
        });

        DatePicker datePicker = new DatePicker("Datum auswählen");
        datePicker.setValue(currentDate);
        datePicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentDate = event.getValue();
                refreshCalendar();
            }
        });

        navigation.add(previousDay, today, nextDay, datePicker);
        return navigation;
    }

    private void refreshCalendar() {
        calendarContainer.removeAll();

        if (currentScheduler == null) {
            calendarContainer.add(new Span("Kein Terminplaner ausgewählt"));
            return;
        }

        H3 dateHeader = new H3(currentDate.format(DATE_FORMATTER) + " - " + currentDate.getDayOfWeek());
        calendarContainer.add(dateHeader);

        // Get office hours for the day
        List<OfficeHours> officeHours = officeHoursService.findBySchedulerAndDate(
            currentScheduler, 
            currentDate
        );

        if (officeHours.isEmpty()) {
            calendarContainer.add(new Span("Keine Sprechzeiten für diesen Tag"));
            return;
        }

        // Get appointments for the day
        LocalDateTime startOfDay = currentDate.atStartOfDay();
        LocalDateTime endOfDay = currentDate.atTime(LocalTime.MAX);
        List<Appointment> appointments = appointmentService.findBySchedulerAndDateRange(
            currentScheduler, 
            startOfDay, 
            endOfDay
        );

        // Create time grid
        VerticalLayout timeGrid = createTimeGrid(officeHours, appointments);
        calendarContainer.add(timeGrid);
    }

    private VerticalLayout createTimeGrid(List<OfficeHours> officeHours, List<Appointment> appointments) {
        VerticalLayout grid = new VerticalLayout();
        grid.setPadding(false);
        grid.setSpacing(false);
        grid.setWidthFull();

        // Find earliest and latest times (with +/- 4 hours buffer)
        LocalTime earliestTime = officeHours.stream()
            .map(OfficeHours::getStartTime)
            .min(LocalTime::compareTo)
            .orElse(LocalTime.of(8, 0))
            .minusHours(4);

        LocalTime latestTime = officeHours.stream()
            .map(OfficeHours::getEndTime)
            .max(LocalTime::compareTo)
            .orElse(LocalTime.of(18, 0))
            .plusHours(4);

        // Ensure times are within 0-24 range
        if (earliestTime.isBefore(LocalTime.of(0, 0))) {
            earliestTime = LocalTime.of(0, 0);
        }
        if (latestTime.isAfter(LocalTime.of(23, 59))) {
            latestTime = LocalTime.of(23, 59);
        }

        // Create time slots (15 minute intervals)
        LocalTime currentTime = earliestTime;
        while (currentTime.isBefore(latestTime)) {
            Div timeSlot = createTimeSlot(currentTime, officeHours, appointments);
            grid.add(timeSlot);
            currentTime = currentTime.plusMinutes(15);
        }

        return grid;
    }

    private Div createTimeSlot(LocalTime time, List<OfficeHours> officeHours, List<Appointment> appointments) {
        Div slot = new Div();
        slot.getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("padding", "var(--lumo-space-xs)")
            .set("min-height", "40px")
            .set("display", "flex")
            .set("align-items", "center");

        // Check if within office hours
        boolean withinOfficeHours = officeHours.stream()
            .anyMatch(oh -> !time.isBefore(oh.getStartTime()) && time.isBefore(oh.getEndTime()));

        if (withinOfficeHours) {
            slot.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        }

        Span timeLabel = new Span(time.format(TIME_FORMATTER));
        timeLabel.getStyle().set("font-weight", "bold").set("margin-right", "var(--lumo-space-m)");
        slot.add(timeLabel);

        // Check for appointments at this time
        LocalDateTime slotDateTime = LocalDateTime.of(currentDate, time);
        appointments.stream()
            .filter(apt -> !apt.getStartTime().isAfter(slotDateTime) 
                        && apt.getEndTime().isAfter(slotDateTime))
            .forEach(apt -> {
                Span appointmentLabel = new Span(
                    apt.getPatient().getLastName() + ", " + 
                    apt.getPatient().getFirstName() + " - " + 
                    apt.getReason()
                );
                appointmentLabel.getStyle()
                    .set("background-color", "var(--lumo-primary-color)")
                    .set("color", "var(--lumo-primary-contrast-color)")
                    .set("padding", "var(--lumo-space-xs)")
                    .set("border-radius", "var(--lumo-border-radius)")
                    .set("cursor", "pointer");
                appointmentLabel.addClickListener(e -> openAppointmentDialog(apt));
                slot.add(appointmentLabel);
            });

        return slot;
    }

    private void openAppointmentDialog() {
        // TODO: Open dialog for creating new appointment
        System.out.println("Open new appointment dialog");
    }

    private void openAppointmentDialog(Appointment appointment) {
        // TODO: Open dialog for editing appointment
        System.out.println("Open appointment dialog for: " + appointment);
    }

    public void setCurrentScheduler(AppointmentScheduler scheduler) {
        this.currentScheduler = scheduler;
        if (schedulerNameLabel != null) {
            schedulerNameLabel.setText(scheduler != null ? scheduler.getName() : "Kein Terminplaner");
        }
        refreshCalendar();
    }
}
