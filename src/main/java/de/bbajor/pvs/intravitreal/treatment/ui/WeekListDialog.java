package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanListPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.taskmanagement.service.TimeSlotReportService;

public class WeekListDialog extends Dialog {

    private static final Logger log = LogManager.getLogger(WeekListDialog.class);
    
    private final Grid<TimeSlotSummary> grid = new Grid<>();
    private WeekListConfig config;
    private final ApplicationContext applicationContext;
    private final UserAccountRepository userAccountRepository;
    private final TreatmentPlanListPresenter treatmentPlanListPresenter;
    
    private LocalDate currentWeekStart;
    private Span weekLabel;
    private DatePicker datePicker;

    public WeekListDialog(WeekListConfig config, ApplicationContext applicationContext, TreatmentPlanListPresenter treatmentPlanListPresenter) {
        this.config = config;
        this.applicationContext = applicationContext;
        this.userAccountRepository = applicationContext.getBean(UserAccountRepository.class);
        this.treatmentPlanListPresenter = treatmentPlanListPresenter;
        this.currentWeekStart = config.getStartDateOfWeek();
        
        setHeight("1200px");
        setWidth("1400px");
        setHeaderTitle("Wochenliste");

        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        
        // Gruppiere nach Zeitslot (nach ID, da gleiche Zeitslots unterschiedliche Objekte sein können)
        Map<Long, List<Treatment>> treatmentsByTimeSlotId = config.getTreatmentsOfWeek().stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null)
            .collect(Collectors.groupingBy(t -> t.getSurgicalCenterTimeSlot().getId()));
        
        // Konvertiere zu Map mit TimeSlot als Key
        Map<SurgicalCenterTimeSlot, List<Treatment>> treatmentsByTimeSlot = treatmentsByTimeSlotId.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getValue().get(0).getSurgicalCenterTimeSlot(),
                entry -> entry.getValue()
            ));
        
        // Erstelle TimeSlotSummary-Liste
        List<TimeSlotSummary> summaries = treatmentsByTimeSlot.entrySet().stream()
            .map(entry -> new TimeSlotSummary(entry.getKey(), entry.getValue()))
            .sorted(Comparator
                .comparing(TimeSlotSummary::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ts -> ts.getTimeSlot() != null && ts.getTimeSlot().getStartTime() != null 
                    ? ts.getTimeSlot().getStartTime() : java.time.LocalTime.MIN))
            .collect(Collectors.toList());
        
        // Setze Nummern
        for (int i = 0; i < summaries.size(); i++) {
            final int number = i + 1;
            summaries.get(i).setNumber(number);
        }
        
        // Spalten konfigurieren
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        
        grid.addColumn(TimeSlotSummary::getNumber)
            .setHeader("Nr.")
            .setWidth("60px")
            .setFlexGrow(0);
        
        grid.addColumn(summary -> {
            LocalDate date = summary.getDate();
            return date != null ? date.format(formatter) : "-";
        }).setHeader("Datum")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getCenterShort)
            .setHeader("Einrichtung")
            .setWidth("250px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getPatientCount)
            .setHeader("Anzahl Patienten")
            .setWidth("130px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getTimeRange)
            .setHeader("Zeitumfang")
            .setWidth("180px")
            .setFlexGrow(0);
        
        // PDF-Icon-Button für Bericht
        grid.addColumn(new ComponentRenderer<>(summary -> {
            Button pdfButton = new Button(new Icon(VaadinIcon.FILE_TEXT));
            pdfButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ICON);
            pdfButton.setTooltipText("Bericht als PDF herunterladen");
            pdfButton.addClickListener(e -> generateTimeSlotReport(summary));
            return pdfButton;
        })).setHeader("Bericht")
            .setWidth("100px")
            .setFlexGrow(0);
        
        // Setze Row-Styling basierend auf Datum für visuelle Unterscheidung
        grid.setClassNameGenerator(summary -> {
            LocalDate date = summary.getDate();
            LocalDate now = LocalDate.now();
            if (date != null && date.isBefore(now)) {
                // Vergangene Zeitslots - grau abgestuft
                long daysPast = java.time.temporal.ChronoUnit.DAYS.between(date, now);
                if (daysPast <= 7) {
                    return "past-week"; // Letzte Woche - leicht grau
                } else if (daysPast <= 30) {
                    return "past-month"; // Letzter Monat - mittel grau
                } else {
                    return "past-old"; // Älter - dunkel grau
                }
            } else if (date != null && date.isAfter(now)) {
                return "future"; // Zukunft - normal
            }
            return null; // Heute - normal
        });
        
        grid.setItems(summaries);
        
        // Wochenauswahl-Controls im Header
        createWeekNavigationControls();
        
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(grid);
        add(content);
        
        // CSS für vergangene Zeitslots
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  vaadin-grid-row[class*='past-week'] { background-color: #f5f5f5 !important; }" +
            "  vaadin-grid-row[class*='past-month'] { background-color: #e8e8e8 !important; }" +
            "  vaadin-grid-row[class*='past-old'] { background-color: #d0d0d0 !important; }" +
            "  vaadin-grid-row[class*='future'] { background-color: #ffffff !important; }" +
            "`;" +
            "document.head.appendChild(style);"
        );

        Button closeButton = new Button("Schließen");
        closeButton.addClickListener(event -> close());
        getFooter().add(closeButton);
    }
    
    private void createWeekNavigationControls() {
        HorizontalLayout weekNavigation = new HorizontalLayout();
        weekNavigation.setWidthFull();
        weekNavigation.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        weekNavigation.setSpacing(true);
        weekNavigation.setPadding(true);
        
        // Pfeil nach links (vorherige Woche)
        Button prevWeekButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        prevWeekButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ICON);
        prevWeekButton.setTooltipText("Vorherige Woche");
        prevWeekButton.addClickListener(e -> navigateToWeek(-1));
        
        // Woche-Anzeige
        weekLabel = new Span();
        updateWeekLabel();
        
        // Pfeil nach rechts (nächste Woche)
        Button nextWeekButton = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
        nextWeekButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ICON);
        nextWeekButton.setTooltipText("Nächste Woche");
        nextWeekButton.addClickListener(e -> navigateToWeek(1));
        
        // DatePicker für spezifische Auswahl
        datePicker = new DatePicker("Datum auswählen");
        datePicker.setValue(currentWeekStart);
        datePicker.setWidth("200px");
        datePicker.setHelperText("Wählen Sie ein Datum aus der gewünschten Woche");
        datePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                LocalDate selectedDate = e.getValue();
                LocalDate monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                if (!monday.equals(currentWeekStart)) {
                    loadWeek(monday);
                }
            }
        });
        
        weekNavigation.add(prevWeekButton, weekLabel, nextWeekButton, datePicker);
        weekNavigation.setFlexGrow(1.0, weekLabel);
        weekNavigation.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.START);
        
        getHeader().add(weekNavigation);
    }
    
    private void updateWeekLabel() {
        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        weekLabel.setText(String.format("Woche: %s - %s", 
            currentWeekStart.format(formatter), 
            endOfWeek.format(formatter)));
    }
    
    private void navigateToWeek(int weeksOffset) {
        LocalDate newWeekStart = currentWeekStart.plusWeeks(weeksOffset);
        loadWeek(newWeekStart);
    }
    
    private void loadWeek(LocalDate weekStart) {
        currentWeekStart = weekStart;
        LocalDate endOfWeek = weekStart.plusDays(6);
        
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
        // Lade neue Daten
        List<Treatment> treatments = treatmentPlanListPresenter.generateWeekList(weekStart);
        config = new WeekListConfig(treatments, weekStart, endOfWeek);
        
        // Aktualisiere UI
        updateWeekLabel();
        datePicker.setValue(weekStart);
        refreshGrid();
    }
    
    private void refreshGrid() {
        // Lösche alte Spalten
        grid.removeAllColumns();
        
        // Gruppiere nach Zeitslot (nach ID, da gleiche Zeitslots unterschiedliche Objekte sein können)
        Map<Long, List<Treatment>> treatmentsByTimeSlotId = config.getTreatmentsOfWeek().stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null)
            .collect(Collectors.groupingBy(t -> t.getSurgicalCenterTimeSlot().getId()));
        
        // Konvertiere zu Map mit TimeSlot als Key
        Map<SurgicalCenterTimeSlot, List<Treatment>> treatmentsByTimeSlot = treatmentsByTimeSlotId.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getValue().get(0).getSurgicalCenterTimeSlot(),
                entry -> entry.getValue()
            ));
        
        // Erstelle TimeSlotSummary-Liste
        List<TimeSlotSummary> summaries = treatmentsByTimeSlot.entrySet().stream()
            .map(entry -> new TimeSlotSummary(entry.getKey(), entry.getValue()))
            .sorted(Comparator
                .comparing(TimeSlotSummary::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ts -> ts.getTimeSlot() != null && ts.getTimeSlot().getStartTime() != null 
                    ? ts.getTimeSlot().getStartTime() : java.time.LocalTime.MIN))
            .collect(Collectors.toList());
        
        // Setze Nummern
        for (int i = 0; i < summaries.size(); i++) {
            final int number = i + 1;
            summaries.get(i).setNumber(number);
        }
        
        // Spalten konfigurieren
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        
        grid.addColumn(TimeSlotSummary::getNumber)
            .setHeader("Nr.")
            .setWidth("60px")
            .setFlexGrow(0);
        
        grid.addColumn(summary -> {
            LocalDate date = summary.getDate();
            return date != null ? date.format(formatter) : "-";
        }).setHeader("Datum")
            .setWidth("150px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getCenterShort)
            .setHeader("Einrichtung")
            .setWidth("250px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getPatientCount)
            .setHeader("Anzahl Patienten")
            .setWidth("130px")
            .setFlexGrow(0);
        
        grid.addColumn(TimeSlotSummary::getTimeRange)
            .setHeader("Zeitumfang")
            .setWidth("180px")
            .setFlexGrow(0);
        
        // PDF-Icon-Button für Bericht
        grid.addColumn(new ComponentRenderer<>(summary -> {
            Button pdfButton = new Button(new Icon(VaadinIcon.FILE_TEXT));
            pdfButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ICON);
            pdfButton.setTooltipText("Bericht als PDF herunterladen");
            pdfButton.addClickListener(e -> generateTimeSlotReport(summary));
            return pdfButton;
        })).setHeader("Bericht")
            .setWidth("100px")
            .setFlexGrow(0);
        
        grid.setItems(summaries);
    }
    
    private void generateTimeSlotReport(TimeSlotSummary summary) {
        try {
            // Ensure InstitutionContext is set before service call
            ensureInstitutionContext();
            
            TimeSlotReportService reportService = applicationContext.getBean(TimeSlotReportService.class);
            byte[] pdfBytes = reportService.generateTimeSlotReport(summary.getTimeSlot(), summary.getTreatments());
            
            // Erstelle Dateiname
            String dateStr = summary.getDate() != null 
                ? summary.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "unbekannt";
            String centerName = summary.getCenterShort().replaceAll("[^a-zA-Z0-9]", "_");
            String filename = String.format("Zeitslot_Bericht_%s_%s_%s.pdf", 
                dateStr, centerName, java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            
            downloadPdf(pdfBytes, filename);
            
            Notification.show("Bericht wird heruntergeladen", 3000, 
                Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Fehler beim Generieren des Berichts: " + e.getMessage(), 5000, 
                Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new java.io.ByteArrayInputStream(pdfBytes);
        });
        streamResource.setContentType("application/pdf");
        
        getUI().ifPresent(ui -> {
            StreamRegistration registration = ui.getSession().getResourceRegistry()
                .registerResource(streamResource);
            String resourceUrl = registration.getResourceUri().toString();
            
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
}
