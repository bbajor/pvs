package de.bbajor.pvs.appointment.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.service.AppointmentSchedulerService;
import de.bbajor.pvs.appointment.service.AppointmentService;
import de.bbajor.pvs.appointment.service.GlobalAppointmentService;
import de.bbajor.pvs.appointment.service.OfficeHoursService;
import de.bbajor.pvs.patient.service.PatientService;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.AppRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

/**
 * Calendar view for displaying and managing appointments.
 * Shows a day view or week view with time slots and appointments.
 */
@Route("appointment-calendar")
@PageTitle("Terminkalender")
@Menu(order = 3, icon = "vaadin:calendar", title = "Terminkalender")
@PermitAll
public class AppointmentCalendarView extends Main implements BeforeEnterObserver {

    private enum ViewMode {
        DAY, WEEK
    }

    private final AppointmentSchedulerService schedulerService;
    private final AppointmentService appointmentService;
    private final OfficeHoursService officeHoursService;
    private final PatientService patientService;
    private final GlobalAppointmentService globalAppointmentService;

    private AppointmentScheduler currentScheduler;
    private LocalDate currentDate;
    private ViewMode currentViewMode = ViewMode.DAY;
    private Div calendarContainer;
    private Component dateNavigation;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public AppointmentCalendarView(
            AppointmentSchedulerService schedulerService,
            AppointmentService appointmentService,
            OfficeHoursService officeHoursService,
            PatientService patientService,
            GlobalAppointmentService globalAppointmentService) {
        this.schedulerService = schedulerService;
        this.appointmentService = appointmentService;
        this.officeHoursService = officeHoursService;
        this.patientService = patientService;
        this.globalAppointmentService = globalAppointmentService;
        this.currentDate = LocalDate.now();

        addClassNames(
            LumoUtility.BoxSizing.BORDER, 
            LumoUtility.Display.FLEX, 
            LumoUtility.FlexDirection.COLUMN,
            "view-content", 
            LumoUtility.Gap.MEDIUM
        );
        setSizeFull();

        // Initialize with first scheduler if available
        List<AppointmentScheduler> schedulers = schedulerService.findAll();
        if (!schedulers.isEmpty()) {
            currentScheduler = schedulers.get(0);
        }

        initializeView();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access appointment data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
        }
    }

    private void initializeView() {
        Button newAppointmentButton = new Button("Neuer Termin", event -> openAppointmentDialog());
        newAppointmentButton.setIcon(VaadinIcon.PLUS.create());
        newAppointmentButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        newAppointmentButton.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);

        // Global next available slot button
        Button globalNextSlotButton = new Button("Nächster freier Termin", 
            event -> findGlobalNextSlot());
        globalNextSlotButton.setIcon(VaadinIcon.SEARCH.create());
        globalNextSlotButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);
        globalNextSlotButton.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);

        // Add SchedulerSwitcher for quick switching between calendars
        SchedulerSwitcher schedulerSwitcher = new SchedulerSwitcher(schedulerService);
        schedulerSwitcher.setOnSchedulerChange(scheduler -> {
            setCurrentScheduler(scheduler);
        });
        if (currentScheduler != null) {
            schedulerSwitcher.setCurrentScheduler(currentScheduler);
        }

        // Überschrift
        H1 title = new H1("Terminkalender");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        add(title);

        // Button-Layout
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.START);
        buttonLayout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        buttonLayout.add(newAppointmentButton, globalNextSlotButton);
        add(buttonLayout);

        add(schedulerSwitcher);
        add(createViewModeTabs());
        
        dateNavigation = createDateNavigation();
        add(dateNavigation);

        calendarContainer = new Div();
        calendarContainer.setSizeFull();
        add(calendarContainer);

        refreshCalendar();
    }

    private Component createViewModeTabs() {
        Tab dayTab = new Tab("Tagesansicht");
        Tab weekTab = new Tab("Wochenansicht");
        Tabs tabs = new Tabs(dayTab, weekTab);
        
        if (currentViewMode == ViewMode.DAY) {
            tabs.setSelectedTab(dayTab);
        } else {
            tabs.setSelectedTab(weekTab);
        }
        
        tabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            if (selected == dayTab) {
                currentViewMode = ViewMode.DAY;
            } else {
                currentViewMode = ViewMode.WEEK;
            }
            remove(dateNavigation);
            dateNavigation = createDateNavigation();
            add(dateNavigation);
            refreshCalendar();
        });
        
        return tabs;
    }

    private Component createDateNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setWidthFull();
        navigation.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        navigation.setAlignItems(HorizontalLayout.Alignment.CENTER);

        if (currentViewMode == ViewMode.DAY) {
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
        } else {
            // Week navigation
            Button previousWeek = new Button(VaadinIcon.ANGLE_LEFT.create(), event -> {
                currentDate = currentDate.minusWeeks(1);
                refreshCalendar();
            });

            Button thisWeek = new Button("Diese Woche", event -> {
                currentDate = LocalDate.now();
                refreshCalendar();
            });

            Button nextWeek = new Button(VaadinIcon.ANGLE_RIGHT.create(), event -> {
                currentDate = currentDate.plusWeeks(1);
                refreshCalendar();
            });

            DatePicker datePicker = new DatePicker("Woche auswählen");
            datePicker.setValue(currentDate);
            datePicker.addValueChangeListener(event -> {
                if (event.getValue() != null) {
                    currentDate = event.getValue();
                    refreshCalendar();
                }
            });

            navigation.add(previousWeek, thisWeek, nextWeek, datePicker);
        }

        return navigation;
    }

    private void refreshCalendar() {
        calendarContainer.removeAll();

        if (currentScheduler == null) {
            calendarContainer.add(new Span("Kein Terminplaner ausgewählt"));
            return;
        }

        if (currentViewMode == ViewMode.DAY) {
            refreshDayView();
        } else {
            refreshWeekView();
        }
    }

    private void refreshDayView() {
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

    private void refreshWeekView() {
        // Calculate week start (Monday) and end (Sunday)
        LocalDate weekStart = currentDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        
        H3 weekHeader = new H3("Woche " + weekStart.format(DATE_FORMATTER) + " - " + weekEnd.format(DATE_FORMATTER));
        calendarContainer.add(weekHeader);

        // Get appointments for the entire week
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDateTime endOfWeek = weekEnd.atTime(LocalTime.MAX);
        List<Appointment> weekAppointments = appointmentService.findBySchedulerAndDateRange(
            currentScheduler,
            startOfWeek,
            endOfWeek
        );

        // Group appointments by date
        Map<LocalDate, List<Appointment>> appointmentsByDate = weekAppointments.stream()
            .collect(Collectors.groupingBy(apt -> apt.getStartTime().toLocalDate()));

        // Create week grid
        HorizontalLayout weekGrid = createWeekGrid(weekStart, appointmentsByDate);
        calendarContainer.add(weekGrid);
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

    private HorizontalLayout createWeekGrid(LocalDate weekStart, Map<LocalDate, List<Appointment>> appointmentsByDate) {
        HorizontalLayout weekLayout = new HorizontalLayout();
        weekLayout.setWidthFull();
        weekLayout.setSpacing(false);
        weekLayout.setPadding(false);
        weekLayout.addClassNames(LumoUtility.Gap.SMALL);

        // Get all office hours for the week to determine time range
        LocalTime earliestTime = LocalTime.of(23, 59);
        LocalTime latestTime = LocalTime.of(0, 0);
        
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<OfficeHours> dayOfficeHours = officeHoursService.findBySchedulerAndDate(currentScheduler, day);
            if (!dayOfficeHours.isEmpty()) {
                LocalTime dayEarliest = dayOfficeHours.stream()
                    .map(OfficeHours::getStartTime)
                    .min(LocalTime::compareTo)
                    .orElse(LocalTime.of(8, 0))
                    .minusHours(2);
                LocalTime dayLatest = dayOfficeHours.stream()
                    .map(OfficeHours::getEndTime)
                    .max(LocalTime::compareTo)
                    .orElse(LocalTime.of(18, 0))
                    .plusHours(2);
                
                if (dayEarliest.isBefore(earliestTime)) {
                    earliestTime = dayEarliest;
                }
                if (dayLatest.isAfter(latestTime)) {
                    latestTime = dayLatest;
                }
            }
        }

        // Fallback if no office hours found
        if (earliestTime.equals(LocalTime.of(23, 59))) {
            earliestTime = LocalTime.of(6, 0);
            latestTime = LocalTime.of(20, 0);
        }

        // Ensure times are within 0-24 range
        if (earliestTime.isBefore(LocalTime.of(0, 0))) {
            earliestTime = LocalTime.of(0, 0);
        }
        if (latestTime.isAfter(LocalTime.of(23, 59))) {
            latestTime = LocalTime.of(23, 59);
        }

        // Create column for each day (Monday to Sunday)
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            VerticalLayout dayColumn = createDayColumn(day, earliestTime, latestTime, 
                appointmentsByDate.getOrDefault(day, new ArrayList<>()));
            weekLayout.add(dayColumn);
        }

        return weekLayout;
    }

    private VerticalLayout createDayColumn(LocalDate day, LocalTime earliestTime, LocalTime latestTime, 
                                          List<Appointment> dayAppointments) {
        VerticalLayout column = new VerticalLayout();
        column.setPadding(false);
        column.setSpacing(false);
        column.setWidth("14%");
        column.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.SMALL);
        column.getStyle().set("min-width", "150px");

        // Day header
        H4 dayHeader = new H4(day.getDayOfWeek().toString() + "\n" + day.format(DateTimeFormatter.ofPattern("dd.MM.")));
        dayHeader.getStyle()
            .set("text-align", "center")
            .set("padding", "var(--lumo-space-s)")
            .set("margin", "0")
            .set("background-color", day.equals(LocalDate.now()) 
                ? "var(--lumo-primary-color-10pct)" 
                : "var(--lumo-contrast-5pct)");
        column.add(dayHeader);

        // Get office hours for this day
        List<OfficeHours> dayOfficeHours = officeHoursService.findBySchedulerAndDate(currentScheduler, day);

        // Time slots
        Div timeSlotsContainer = new Div();
        timeSlotsContainer.setWidthFull();
        timeSlotsContainer.getStyle()
            .set("overflow-y", "auto")
            .set("max-height", "600px");

        LocalTime currentTime = earliestTime;
        while (currentTime.isBefore(latestTime)) {
            Div timeSlot = createWeekTimeSlot(day, currentTime, dayOfficeHours, dayAppointments);
            timeSlotsContainer.add(timeSlot);
            currentTime = currentTime.plusMinutes(15);
        }

        column.add(timeSlotsContainer);
        column.setFlexGrow(1, timeSlotsContainer);

        return column;
    }

    private Div createWeekTimeSlot(LocalDate day, LocalTime time, List<OfficeHours> officeHours, 
                                   List<Appointment> appointments) {
        Div slot = new Div();
        slot.getStyle()
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("padding", "var(--lumo-space-xs)")
            .set("min-height", "30px")
            .set("position", "relative");

        // Check if within office hours
        boolean withinOfficeHours = officeHours.stream()
            .anyMatch(oh -> !time.isBefore(oh.getStartTime()) && time.isBefore(oh.getEndTime()));

        if (withinOfficeHours) {
            slot.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        }

        // Time label (only show every hour)
        if (time.getMinute() == 0) {
            Span timeLabel = new Span(time.format(TIME_FORMATTER));
            timeLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-contrast-70pct)")
                .set("position", "absolute")
                .set("top", "2px")
                .set("left", "4px");
            slot.add(timeLabel);
        }

        // Check for appointments at this time
        LocalDateTime slotDateTime = LocalDateTime.of(day, time);
        appointments.stream()
            .filter(apt -> !apt.getStartTime().isAfter(slotDateTime) 
                        && apt.getEndTime().isAfter(slotDateTime))
            .forEach(apt -> {
                Span appointmentLabel = new Span(
                    apt.getPatient().getLastName() + ", " + 
                    apt.getPatient().getFirstName()
                );
                appointmentLabel.getStyle()
                    .set("background-color", "var(--lumo-primary-color)")
                    .set("color", "var(--lumo-primary-contrast-color)")
                    .set("padding", "2px 4px")
                    .set("border-radius", "var(--lumo-border-radius-s)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("cursor", "pointer")
                    .set("display", "block")
                    .set("margin-top", "2px")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
                appointmentLabel.addClickListener(e -> openAppointmentDialog(apt));
                slot.add(appointmentLabel);
            });

        return slot;
    }

    private void openAppointmentDialog() {
        if (currentScheduler == null) {
            Notification.show("Bitte wählen Sie zuerst einen Terminplaner aus", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        
        AppointmentDialog dialog = new AppointmentDialog(
            appointmentService,
            patientService,
            currentScheduler,
            null
        );
        dialog.setOnSaveCallback(this::refreshCalendar);
        dialog.open();
    }

    private void openAppointmentDialog(Appointment appointment) {
        AppointmentDialog dialog = new AppointmentDialog(
            appointmentService,
            patientService,
            currentScheduler,
            appointment
        );
        dialog.setOnSaveCallback(this::refreshCalendar);
        dialog.open();
    }

    public void setCurrentScheduler(AppointmentScheduler scheduler) {
        this.currentScheduler = scheduler;
        refreshCalendar();
    }

    /**
     * Find the next available slot globally across all schedulers.
     */
    private void findGlobalNextSlot() {
        LocalDateTime from = LocalDateTime.now();
        
        globalAppointmentService.findNextAvailableSlotGlobally(from, 30)
            .ifPresentOrElse(
                slot -> {
                    // Switch to the scheduler with the earliest slot
                    setCurrentScheduler(slot.scheduler());
                    
                    // Open dialog with pre-filled time
                    Appointment newAppointment = new Appointment();
                    newAppointment.setScheduler(slot.scheduler());
                    newAppointment.setStartTime(slot.time());
                    newAppointment.setEndTime(slot.time().plusMinutes(30));
                    
                    AppointmentDialog dialog = new AppointmentDialog(
                        appointmentService,
                        patientService,
                        slot.scheduler(),
                        newAppointment
                    );
                    dialog.setOnSaveCallback(this::refreshCalendar);
                    dialog.open();
                    
                    Notification.show(
                        String.format("Nächster freier Termin bei %s: %s", 
                            slot.scheduler().getName(),
                            slot.time().format(DATETIME_FORMATTER)),
                        5000,
                        Notification.Position.MIDDLE
                    ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                },
                () -> {
                    Notification.show(
                        "Kein freier Termin in den nächsten 4 Wochen bei keinem Arzt gefunden",
                        5000,
                        Notification.Position.MIDDLE
                    ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            );
    }
}
