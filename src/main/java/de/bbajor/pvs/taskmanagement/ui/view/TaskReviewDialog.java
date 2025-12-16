package de.bbajor.pvs.taskmanagement.ui.view;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.LocalDate;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.intravitreal.treatment.ui.NextTreatmentBookingDialog;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;
import org.springframework.context.ApplicationContext;

public class TaskReviewDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(TaskReviewDialog.class);

    private List<Treatment> treatments;
    private int currentTreatmentIndex = 0;
    private Task task;
    private TreatmentRepository treatmentRepository;
    private TaskService taskService;
    private AuthenticationContext authenticationContext;
    private TreatmentReportService reportService;
    private UserAccountRepository userAccountRepository;
    private ApplicationContext applicationContext;
    private TreatmentPlanPresenter treatmentPlanPresenter;
    private de.bbajor.pvs.taskmanagement.service.StandardRemarkService standardRemarkService;
    private de.bbajor.pvs.taskmanagement.service.TreatmentRemarkService treatmentRemarkService;

    private VerticalLayout mainContent;
    private VerticalLayout overviewLayout;
    
    // Track which treatments have follow-up bookings for visual feedback
    private Set<Long> treatmentsWithFollowUpBooking = new HashSet<>();
    
    // Track selected treatments for bulk approval
    private Set<Long> selectedTreatmentsForApproval = new HashSet<>();
    
    // Referenz zur "Alle auswählen" Checkbox
    private Checkbox selectAllCheckbox;

    public TaskReviewDialog(Task task, TreatmentRepository treatmentRepository, TaskService taskService,
            AuthenticationContext authenticationContext, TreatmentReportService reportService,
            UserAccountRepository userAccountRepository, ApplicationContext applicationContext,
            TreatmentPlanPresenter treatmentPlanPresenter,
            de.bbajor.pvs.taskmanagement.service.StandardRemarkService standardRemarkService,
            de.bbajor.pvs.taskmanagement.service.TreatmentRemarkService treatmentRemarkService) {
        this.task = task;
        this.treatmentRepository = treatmentRepository;
        this.taskService = taskService;
        this.authenticationContext = authenticationContext;
        this.reportService = reportService;
        this.userAccountRepository = userAccountRepository;
        this.applicationContext = applicationContext;
        this.treatmentPlanPresenter = treatmentPlanPresenter;
        this.standardRemarkService = standardRemarkService;
        this.treatmentRemarkService = treatmentRemarkService;

        ensureInstitutionContext();
        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());

        setHeaderTitle("Dokumentation");
        setWidth("1400px");
        setHeight("900px");
        setDraggable(true);
        setResizable(true);
        setCloseOnOutsideClick(false);

        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);

        add(mainContent);
        
        // Footer-Buttons ZUERST erstellen, bevor showOverview() aufgerufen wird
        createFooterButtons();
        
        // Show overview by default (nach createFooterButtons, damit Buttons initialisiert sind)
        showOverview();
    }
    
    // Footer-Buttons als Instanzvariablen für Zugriff aus Detail-Ansicht
    private Button startReview;
    private Button approveSelected;
    private Button viewReport;
    private Button backToOverview;
    private Button prevButton;
    private Button nextButton;
    private Button approveSecond;
    
    /**
     * Erstellt die Footer-Buttons analog zum PatientDialog.
     */
    private void createFooterButtons() {
        // closeButton wird nicht mehr benötigt, da X-Icon im Header ist
        
        startReview = new Button("Dokumentation starten", e -> {
            if (treatments.isEmpty()) {
                Notification.show("Keine Behandlungen vorhanden");
                return;
            }
            showTreatmentDetail(0);
        });
        startReview.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // Prüfe ob alle Behandlungen dokumentiert sind
        boolean allDocumented = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        startReview.setEnabled(!task.isCompleted() && !treatments.isEmpty() && !allDocumented);
        
        // Button für Massendokumentation (nur undokumentierte Behandlungen)
        approveSelected = new Button("Dokumentation abschließen", VaadinIcon.CHECK.create(), e -> {
            approveSelectedTreatments();
        });
        approveSelected.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        approveSelected.setEnabled(!selectedTreatmentsForApproval.isEmpty() && !task.isCompleted() && hasReviewPermission());
        if (!hasReviewPermission()) {
            approveSelected.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen dokumentieren");
        }
        
        viewReport = new Button("Sammelbericht generieren", VaadinIcon.FILE_TEXT.create(), e -> generateCombinedReport());
        viewReport.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        
        // Always enabled - can generate preliminary reports
        viewReport.setEnabled(!treatments.isEmpty());
        boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        if (!allApproved && !treatments.isEmpty()) {
            viewReport.setTooltipText("Vorläufiger Sammelbericht (nicht alle Behandlungen sind dokumentiert)");
        } else if (!treatments.isEmpty()) {
            viewReport.setTooltipText("Sammelbericht generieren");
        }
        
        // Navigation-Buttons für Detail-Ansicht
        backToOverview = new Button("Zurück zur Übersicht", e -> showOverview());
        backToOverview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        prevButton = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            if (currentTreatmentIndex > 0) {
                Treatment currentTreatment = treatments.get(currentTreatmentIndex);
                // Speichere zusätzliche Infos falls vorhanden
                TextArea additionalInfoField = findAdditionalInfoField();
                if (additionalInfoField != null) {
                    saveAdditionalInfo(currentTreatment, additionalInfoField.getValue());
                }
                showTreatmentDetail(currentTreatmentIndex - 1);
            }
        });
        
        nextButton = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            if (currentTreatmentIndex < treatments.size() - 1) {
                Treatment currentTreatment = treatments.get(currentTreatmentIndex);
                // Speichere zusätzliche Infos falls vorhanden
                TextArea additionalInfoField = findAdditionalInfoField();
                if (additionalInfoField != null) {
                    saveAdditionalInfo(currentTreatment, additionalInfoField.getValue());
                }
                showTreatmentDetail(currentTreatmentIndex + 1);
            }
        });
        
        // Check if user can approve (only MEDICAL_STAFF, OWNER, DOCTOR - not ADMIN)
        boolean canApprove = hasReviewPermission();
        
        Button approveSingle = new Button("Dokumentieren", e -> {
            Treatment currentTreatment = treatments.get(currentTreatmentIndex);
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(currentTreatment.getId(), userId, user, false);
                TextArea additionalInfoField = findAdditionalInfoField();
                if (additionalInfoField != null) {
                    saveAdditionalInfo(currentTreatment, additionalInfoField.getValue());
                }
                Notification.show("Behandlung dokumentiert");
                reloadTreatments();
                
                // Navigate to next treatment if available, otherwise go back to overview
                if (currentTreatmentIndex < treatments.size() - 1) {
                    showTreatmentDetail(currentTreatmentIndex + 1);
                } else {
                    showOverview();
                }
            } catch (AccessDeniedException ex) {
                Notification errorNotification = new Notification(
                    ex.getMessage() != null ? ex.getMessage() : 
                    "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu dokumentieren.",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler beim Dokumentieren der Behandlung: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            }
        });
        approveSingle.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        approveSingle.setEnabled(!task.isCompleted() && canApprove);
        if (!canApprove) {
            approveSingle.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen dokumentieren");
        }
        
        approveSecond = new Button("Als Zweitprüfer dokumentieren", e -> {
            Treatment currentTreatment = treatments.get(currentTreatmentIndex);
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(currentTreatment.getId(), userId, user, true);
                TextArea additionalInfoField = findAdditionalInfoField();
                if (additionalInfoField != null) {
                    saveAdditionalInfo(currentTreatment, additionalInfoField.getValue());
                }
                Notification.show("Zweitprüfung dokumentiert");
                reloadTreatments();
                
                // Navigate to next treatment if available, otherwise go back to overview
                if (currentTreatmentIndex < treatments.size() - 1) {
                    showTreatmentDetail(currentTreatmentIndex + 1);
                } else {
                    showOverview();
                }
            } catch (AccessDeniedException ex) {
                Notification errorNotification = new Notification(
                    ex.getMessage() != null ? ex.getMessage() : 
                    "Sie haben nicht die erforderlichen Berechtigungen für die Zweitdokumentation.",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler bei der Zweitdokumentation: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            }
        });
        approveSecond.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        approveSecond.setEnabled(!task.isCompleted() && canApprove);
        if (!canApprove) {
            approveSecond.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen dokumentieren");
        }
        
        // Initial: Übersichts-Buttons anzeigen
        showOverviewFooter();
    }
    
    /**
     * Zeigt die Footer-Buttons für die Übersicht.
     */
    private void showOverviewFooter() {
        getFooter().removeAll();
        // Prüfe ob alle Behandlungen dokumentiert sind
        boolean allDocumented = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        startReview.setEnabled(!task.isCompleted() && !treatments.isEmpty() && !allDocumented);
        getFooter().add(approveSelected, startReview, viewReport);
        updateFooterButtons();
    }
    
    /**
     * Zeigt die Footer-Buttons für die Detail-Ansicht.
     */
    private void showDetailFooter(int index) {
        getFooter().removeAll();
        prevButton.setEnabled(index > 0);
        nextButton.setEnabled(index < treatments.size() - 1);
        
        // Button "Abschließen" für Einzeldokumentation
        Treatment currentTreatment = treatments.get(index);
        Button approveSingle = new Button("Abschließen", VaadinIcon.CHECK.create(), e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(currentTreatment.getId(), userId, user, false);
                
                Notification.show("Behandlung dokumentiert");
                reloadTreatments();
                
                // Navigate to next treatment if available, otherwise go back to overview
                if (index < treatments.size() - 1) {
                    showTreatmentDetail(index + 1);
                } else {
                    showOverview();
                }
            } catch (AccessDeniedException ex) {
                Notification errorNotification = new Notification(
                    ex.getMessage() != null ? ex.getMessage() : 
                    "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu dokumentieren.",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler beim Dokumentieren der Behandlung: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            }
        });
        approveSingle.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        boolean canApprove = hasReviewPermission();
        approveSingle.setEnabled(!task.isCompleted() && canApprove && currentTreatment.getApprovalDate() == null);
        if (!canApprove) {
            approveSingle.setTooltipText("Nur MFA, Inhaber, Ärzte und Institutionsadmins können Behandlungen dokumentieren");
        }
        
        getFooter().add(backToOverview, prevButton, nextButton, approveSingle, approveSecond);
    }
    
    /**
     * Aktualisiert die Footer-Buttons basierend auf dem aktuellen Zustand.
     */
    private void updateFooterButtons() {
        if (approveSelected != null) {
            approveSelected.setEnabled(!selectedTreatmentsForApproval.isEmpty() && !task.isCompleted() && hasReviewPermission());
        }
    }
    
    /**
     * Findet das AdditionalInfo-TextArea in der Detail-Ansicht.
     */
    private TextArea findAdditionalInfoField() {
        return findRemarksField();
    }
    
    /**
     * Findet das Bemerkungen-TextArea in der Detail-Ansicht.
     * Wird nicht mehr verwendet, da TextArea entfernt wurde.
     */
    private TextArea findRemarksField() {
        return null;
    }
    
    /**
     * Getter für TreatmentRepository (für TreatmentReviewDetailLayout).
     */
    TreatmentRepository getTreatmentRepository() {
        return treatmentRepository;
    }
    
    /**
     * Getter für TaskService (für TreatmentReviewDetailLayout).
     */
    TaskService getTaskService() {
        return taskService;
    }
    
    /**
     * Getter für ApplicationContext (für TreatmentReviewDetailLayout).
     */
    ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * Lädt alle ausgewählten Berichte herunter.
     */
    private void downloadSelectedReports() {
        if (selectedTreatmentsForApproval.isEmpty()) {
            Notification.show("Bitte wählen Sie mindestens eine Behandlung aus.", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            ensureInstitutionContext();
            String treatingDoctor = authenticationContext.getPrincipalName().orElse("Unbekannt");
            
            int downloadedCount = 0;
            int failedCount = 0;
            
            for (Long treatmentId : selectedTreatmentsForApproval) {
                try {
                    Treatment treatment = treatments.stream()
                            .filter(t -> t.getId().equals(treatmentId))
                            .findFirst()
                            .orElse(null);
                    
                    if (treatment == null) {
                        failedCount++;
                        continue;
                    }
                    
                    boolean isApproved = treatment.getApprovalDate() != null;
                    byte[] pdfBytes = reportService.generatePatientPdfReport(treatment, task.getTimeSlot(), treatingDoctor, isApproved);
                    
                    // Create filename with patient name
                    String patientName = treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null
                            ? treatment.getTreatmentPlan().getPatient().getLastName() + "_" + treatment.getTreatmentPlan().getPatient().getFirstName()
                            : "Patient";
                    String prefix = isApproved ? "Behandlungsbericht" : "Vorläufiger_Behandlungsbericht";
                    String filename = prefix + "_" + patientName + "_" + 
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
                    
                    downloadPdf(pdfBytes, filename);
                    downloadedCount++;
                    
                    // Kleine Verzögerung zwischen Downloads, damit Browser nicht überlastet wird
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception ex) {
                    log.error("Fehler beim Herunterladen des Berichts für Behandlung " + treatmentId, ex);
                    failedCount++;
                }
            }
            
            if (failedCount == 0) {
                Notification.show(downloadedCount + " Bericht(e) werden heruntergeladen", 3000, 
                        Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(downloadedCount + " Bericht(e) heruntergeladen, " + failedCount + " Fehler", 5000, 
                        Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        } catch (Exception e) {
            log.error("Fehler beim Herunterladen der Berichte", e);
            Notification notification = Notification.show(
                    "Fehler beim Herunterladen der Berichte: " + e.getMessage(), 5000, 
                    Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showOverview() {
        mainContent.removeAll();
        
        overviewLayout = new VerticalLayout();
        overviewLayout.setSizeFull();
        
        // Section mit Header und Anzahl - auf zwei Zeilen umbrechen
        Section headerSection = new Section();
        headerSection.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        // Berechne Anzahl der nicht dokumentierten Behandlungen
        long notDocumentedCount = treatments.stream()
                .filter(t -> t.getApprovalDate() == null)
                .count();
        long totalCount = treatments.size();
        
        // Zeile 1: "Behandlungen vom X um Y Uhr (Augenzentrum XY)"
        String dateTimeText = "";
        String centerName = "";
        if (task.getTimeSlot() != null) {
            var timeSlot = task.getTimeSlot();
            if (timeSlot.getDate() != null && timeSlot.getStartTime() != null) {
                dateTimeText = timeSlot.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                               " um " + timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " Uhr";
            }
            if (timeSlot.getSurgicalCenter() != null && timeSlot.getSurgicalCenter().getName() != null) {
                centerName = timeSlot.getSurgicalCenter().getName();
            }
        }
        
        Div headerLine1 = new Div();
        headerLine1.getStyle().set("font-size", "var(--lumo-font-size-l)");
        headerLine1.getStyle().set("font-weight", "600");
        headerLine1.setText("Behandlungen vom " + dateTimeText + (centerName.isEmpty() ? "" : " (" + centerName + ")"));
        
        // Zeile 2: "3/8 Behandlungen sind noch nicht dokumentiert"
        Div headerLine2 = new Div();
        headerLine2.getStyle().set("font-size", "var(--lumo-font-size-m)");
        headerLine2.getStyle().set("color", "var(--lumo-secondary-text-color)");
        headerLine2.getStyle().set("margin-top", "var(--lumo-space-xs)");
        headerLine2.setText(String.format("%d/%d Behandlungen sind noch nicht dokumentiert", notDocumentedCount, totalCount));
        
        VerticalLayout headerLayout = new VerticalLayout();
        headerLayout.setSpacing(false);
        headerLayout.setPadding(false);
        headerLayout.add(headerLine1, headerLine2);
        
        headerSection.add(headerLayout);
        overviewLayout.add(headerSection);
        
        // Grid mit angepasster Spaltenreihenfolge: Patient, Auge, Medikament, Status & Genehmigung, Dokumentiert, Bericht, Auswahl
        Grid<Treatment> grid = new Grid<>(Treatment.class, false);
        grid.setSizeFull();
        
        // Spalte 1: Patient (mit Renderer für bessere Darstellung)
        grid.addComponentColumn(treatment -> {
            if (treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null) {
                Patient patient = treatment.getTreatmentPlan().getPatient();
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
                        patient.getBirth().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    birthSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    birthSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    patientLayout.add(birthSpan);
                }
                
                return patientLayout;
            }
            return new Span("-");
        }).setHeader("Patient").setAutoWidth(true).setResizable(true);
        
        // Spalte 2: Auge (nur "rechts" oder "links" mit Farben)
        grid.addComponentColumn(treatment -> {
            if (treatment.getSideOfEye() == null) {
                return new Span("-");
            }
            Span eyeSpan = new Span(treatment.getSideOfEye() == SideOfEye.RIGHT ? "rechts" : "links");
            if (treatment.getSideOfEye() == SideOfEye.RIGHT) {
                eyeSpan.getStyle().set("background-color", "#E3F2FD");
                eyeSpan.getStyle().set("padding", "4px 8px");
                eyeSpan.getStyle().set("border-radius", "4px");
            } else {
                eyeSpan.getStyle().set("background-color", "#FFF3E0");
                eyeSpan.getStyle().set("padding", "4px 8px");
                eyeSpan.getStyle().set("border-radius", "4px");
            }
            return eyeSpan;
        }).setHeader("Auge").setAutoWidth(true).setResizable(true);
        
        // Spalte 3: Medikament
        grid.addColumn(t -> {
            if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament").setAutoWidth(true).setResizable(true);
        
        // Spalte 4: Status & Genehmigung (Status-Combobox + Ampel)
        grid.addComponentColumn(treatment -> {
            HorizontalLayout statusLayout = new HorizontalLayout();
            statusLayout.setSpacing(true);
            statusLayout.setAlignItems(Alignment.CENTER);
            
            // Prüfe ob Behandlung bereits dokumentiert ist
            boolean isDocumented = treatment.getApprovalDate() != null;
            
            // Combobox für Status
            ComboBox<TreatmentStatus> statusComboBox = new ComboBox<>();
            List<TreatmentStatus> statusOptions = Arrays.stream(TreatmentStatus.values())
                    .sorted(Comparator.comparing(TreatmentStatus::getShortLabel))
                    .collect(Collectors.toList());
            statusComboBox.setItems(statusOptions);
            statusComboBox.setItemLabelGenerator(TreatmentStatus::getShortLabel);
            statusComboBox.setValue(treatment.getTreatmentStatus() != null 
                    ? treatment.getTreatmentStatus() 
                    : TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL);
            statusComboBox.setWidth("200px");
            // Deaktiviere Combobox wenn Behandlung bereits dokumentiert ist
            statusComboBox.setEnabled(!isDocumented);
            
            // Ampel
            Div trafficLight = createTrafficLight(treatment.getTreatmentStatus() != null 
                    ? treatment.getTreatmentStatus() 
                    : TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL);
            
            // Status-Änderung
            statusComboBox.addValueChangeListener(e -> {
                TreatmentStatus newStatus = e.getValue();
                if (newStatus != null) {
                    try {
                        taskService.updateTreatmentStatus(treatment.getId(), newStatus);
                        treatment.setTreatmentStatus(newStatus);
                        trafficLight.removeAll();
                        trafficLight.add(createTrafficLightIcon(newStatus));
                    } catch (Exception ex) {
                        log.error("Fehler beim Aktualisieren des Treatment-Status", ex);
                        Notification errorNotification = new Notification(
                            "Fehler beim Aktualisieren: " + ex.getMessage(),
                            5000,
                            Notification.Position.MIDDLE
                        );
                        errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        errorNotification.open();
                        statusComboBox.setValue(treatment.getTreatmentStatus());
                    }
                }
            });
            
            statusLayout.add(statusComboBox, trafficLight);
            return statusLayout;
        }).setHeader("Behandlung erfolgt?").setAutoWidth(true).setResizable(true);
        
        // Spalte 5: Dokumentiert (pro Behandlung: grün wenn dokumentiert, gelb wenn nicht)
        grid.addComponentColumn(treatment -> {
            boolean isDocumented = treatment.getApprovalDate() != null;
            Icon icon;
            if (isDocumented) {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                icon.setColor("var(--lumo-success-color)");
            } else {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.setColor("var(--lumo-warning-color)");
            }
            icon.setSize("20px");
            return icon;
        }).setHeader("Dokumentiert").setAutoWidth(true).setResizable(true);
        
        // Spalte 6: Zweitprüfung (pro Behandlung: grün wenn zweitgeprüft, gelb wenn nicht, grau wenn nicht dokumentiert)
        grid.addComponentColumn(treatment -> {
            boolean isDocumented = treatment.getApprovalDate() != null;
            boolean isSecondApproved = treatment.getSecondApprovalDateTime() != null;
            
            Icon icon;
            if (!isDocumented) {
                // Nicht dokumentiert - grau
                icon = VaadinIcon.CIRCLE_THIN.create();
                icon.setColor("var(--lumo-contrast-30pct)");
            } else if (isSecondApproved) {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                icon.setColor("var(--lumo-success-color)");
            } else {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.setColor("var(--lumo-warning-color)");
            }
            icon.setSize("20px");
            return icon;
        }).setHeader("Zweitprüfung").setAutoWidth(true).setResizable(true);
        
        // Spalte 7: Bericht
        grid.addComponentColumn(treatment -> {
            Button reportButton = new Button(VaadinIcon.FILE_TEXT.create(), e -> generatePatientReport(treatment));
            reportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            boolean isApproved = treatment.getApprovalDate() != null;
            if (isApproved) {
                reportButton.setTooltipText("Bericht für diesen Patienten generieren");
            } else {
                reportButton.setTooltipText("Vorläufigen Bericht generieren (Behandlung noch nicht dokumentiert)");
            }
            reportButton.setEnabled(true); // Always enabled
            return reportButton;
        }).setHeader("Bericht").setAutoWidth(true).setResizable(true);
        
        // Spalte 8: Auswahl (nur Checkbox für Massengenehmigung)
        grid.addComponentColumn(treatment -> {
            Checkbox approvalCheckbox = new Checkbox();
            approvalCheckbox.setValue(selectedTreatmentsForApproval.contains(treatment.getId()));
            approvalCheckbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    selectedTreatmentsForApproval.add(treatment.getId());
                } else {
                    selectedTreatmentsForApproval.remove(treatment.getId());
                }
                // Aktualisiere "Alle auswählen" Checkbox
                updateSelectAllCheckbox();
                // Aktualisiere Footer-Buttons
                updateFooterButtons();
            });
            approvalCheckbox.setTooltipText("Für Massengenehmigung markieren (bekannte Patienten ohne besondere Vorkommnisse)");
            return approvalCheckbox;
        }).setHeader("Auswahl").setAutoWidth(true).setResizable(true);
        
        grid.setItems(treatments);
        
        // Grid-Row-Click-Listener: Springe zur Behandlungssicht
        grid.addItemClickListener(e -> {
            Treatment clickedTreatment = e.getItem();
            int index = treatments.indexOf(clickedTreatment);
            if (index >= 0) {
                showTreatmentDetail(index);
            }
        });
        
        overviewLayout.add(grid);
        
        // Footer-Zeile unter dem Grid mit "Alle auswählen" Checkbox und "Berichte herunterladen" Button
        // Section um den Footer, damit er die volle Grid-Breite einnimmt
        Section gridFooterSection = new Section();
        gridFooterSection.getStyle()
                .set("width", "100%")
                .set("margin", "0")
                .set("padding", "0");
        
        Div gridFooter = new Div();
        gridFooter.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-top", "1px solid var(--lumo-contrast-20pct)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("align-items", "center");
        
        // Leere Zellen für Spalten 1-5
        for (int i = 0; i < 5; i++) {
            Div emptyCell = new Div();
            gridFooter.add(emptyCell);
        }
        
        // Spalte 6 (Bericht): "Berichte herunterladen" Button (nur Icon)
        Button downloadReportsFooter = new Button(VaadinIcon.DOWNLOAD.create(), e -> {
            downloadSelectedReports();
        });
        downloadReportsFooter.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        downloadReportsFooter.setTooltipText("Berichte herunterladen");
        downloadReportsFooter.setEnabled(!selectedTreatmentsForApproval.isEmpty());
        Div reportCell = new Div();
        reportCell.add(downloadReportsFooter);
        gridFooter.add(reportCell);
        
        // Spalte 7 (Auswahl): "Alle auswählen" Checkbox
        Div selectAllCell = new Div();
        selectAllCheckbox = new Checkbox("Alle auswählen");
        selectAllCheckbox.addValueChangeListener(e -> {
            if (e.getValue()) {
                // Alle auswählen
                treatments.forEach(t -> selectedTreatmentsForApproval.add(t.getId()));
            } else {
                // Alle deselektieren
                selectedTreatmentsForApproval.clear();
            }
            // Grid aktualisieren, damit die Checkboxen aktualisiert werden
            grid.getDataProvider().refreshAll();
            // Aktualisiere Footer-Buttons
            updateFooterButtons();
            downloadReportsFooter.setEnabled(!selectedTreatmentsForApproval.isEmpty());
        });
        selectAllCell.add(selectAllCheckbox);
        gridFooter.add(selectAllCell);
        
        gridFooterSection.add(gridFooter);
        overviewLayout.add(gridFooterSection);
        
        mainContent.add(overviewLayout);
        
        // Footer für Übersicht anzeigen
        showOverviewFooter();
    }
    
    /**
     * Aktualisiert den Status der "Alle auswählen" Checkbox basierend auf der aktuellen Auswahl.
     */
    private void updateSelectAllCheckbox() {
        if (selectAllCheckbox != null && treatments != null && !treatments.isEmpty()) {
            boolean allSelected = treatments.stream()
                    .allMatch(t -> selectedTreatmentsForApproval.contains(t.getId()));
            selectAllCheckbox.setValue(allSelected);
        }
    }

    private void showTreatmentDetail(int index) {
        if (index < 0 || index >= treatments.size()) {
            return;
        }
        
        // Ensure InstitutionContext is set before accessing treatment data
        ensureInstitutionContext();
        
        currentTreatmentIndex = index;
        Treatment treatment = treatments.get(currentTreatmentIndex);
        
        mainContent.removeAll();
        
        // Verwende das neue TreatmentReviewDetailLayout
        TreatmentReviewDetailLayout detailLayoutComponent = new TreatmentReviewDetailLayout(
                treatment, index, treatments.size(), this, standardRemarkService, treatmentRemarkService);
        detailLayoutComponent.setSizeFull();
        
        mainContent.add(detailLayoutComponent);
        
        // Footer für Detail-Ansicht anzeigen
        showDetailFooter(index);
    }

    private void saveAdditionalInfo(Treatment treatment, String additionalInfo) {
        taskService.updateTreatmentAdditionalInfo(treatment.getId(), additionalInfo);
    }
    
    private void openFollowUpBookingDialog(Treatment treatment, int index) {
        try {
            ensureInstitutionContext();
            
            // Check if there's already a follow-up treatment
            Treatment existingFollowUp = taskService.findExistingFollowUpTreatment(treatment.getId());
            
            if (existingFollowUp != null) {
                // Show confirmation dialog to ask if user wants to adjust the existing appointment
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Folgetermin bereits vorhanden");
                confirmDialog.setText(
                    "Für diese Behandlung existiert bereits ein Folgetermin am " +
                    (existingFollowUp.getDate() != null ? 
                        existingFollowUp.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : 
                        "unbekanntem Datum") +
                    ". Möchten Sie diesen Termin anpassen?"
                );
                confirmDialog.setConfirmText("Termin anpassen");
                confirmDialog.setCancelText("Abbrechen");
                confirmDialog.setCancelable(true);
                
                confirmDialog.addConfirmListener(e -> {
                    // Open existing treatment in treatment plan view for editing
                    // This would require navigation to the treatment plan detail view
                    // For now, we'll show a notification that this feature needs to be implemented
                    Notification.show(
                        "Termin-Anpassung: Bitte öffnen Sie den Behandlungsplan im Ivom-Planer, um den Termin anzupassen.",
                        5000,
                        Notification.Position.MIDDLE
                    );
                });
                
                confirmDialog.open();
                return;
            }
            
            // Check if follow-up booking is still possible
            if (!taskService.canBookFollowUpTreatment(treatment.getId())) {
                Notification errorNotification = new Notification(
                    "Folgetermin kann nicht gebucht werden. Der Task ist möglicherweise bereits abgeschlossen oder es existiert bereits ein Folgetermin.",
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
                return;
            }
            
            // Get treatment plan and side of eye
            TreatmentPlan treatmentPlan = treatment.getTreatmentPlan();
            if (treatmentPlan == null) {
                Notification errorNotification = new Notification(
                    "Behandlung hat keinen Behandlungsplan.",
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
                return;
            }
            
            SideOfEye sideOfEye = treatment.getSideOfEye();
            if (sideOfEye == null) {
                Notification errorNotification = new Notification(
                    "Behandlung hat kein Auge zugewiesen.",
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
                return;
            }
            
            // Bestimme die Buchungslogik basierend auf dem Treatment-Status
            TreatmentStatus treatmentStatus = treatment.getTreatmentStatus();
            if (treatmentStatus == null) {
                treatmentStatus = TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL;
            }
            
            boolean useNormalInterval = treatmentStatus.shouldUseNormalInterval();
            
            // Open NextTreatmentBookingDialog mit Status-Information
            NextTreatmentBookingDialog dialog = new NextTreatmentBookingDialog(
                treatmentPlan, sideOfEye, applicationContext, treatmentPlanPresenter, 
                createdTreatment -> {
                    treatmentsWithFollowUpBooking.add(treatment.getId());
                    Notification.show("Folgetermin erfolgreich gebucht", 3000, Notification.Position.BOTTOM_CENTER);
                    // Refresh current view
                    showTreatmentDetail(index);
                }
            );
            
            // Setze die Buchungslogik basierend auf dem Status
            if (useNormalInterval) {
                // Grün: Intervall vorauswählen (z.B. 4 Wochen)
                dialog.setInitialIntervalMode(true);
                // Berechne das Intervall basierend auf dem letzten Treatment
                int weeksSinceLastTreatment = calculateWeeksSinceLastTreatment(treatment);
                if (weeksSinceLastTreatment > 0) {
                    dialog.setInitialWeeks(weeksSinceLastTreatment);
                } else {
                    dialog.setInitialWeeks(4); // Standard: 4 Wochen
                }
            } else {
                // Gelb/Rot: Nächstmöglicher Termin
                dialog.setInitialIntervalMode(false);
            }
            
            dialog.open();
            
        } catch (Exception ex) {
            log.error("Fehler beim Öffnen des Folgetermin-Buchungsdialogs", ex);
            Notification errorNotification = new Notification(
                "Fehler beim Öffnen des Folgetermin-Buchungsdialogs: " + ex.getMessage(),
                5000,
                Notification.Position.MIDDLE
            );
            errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            errorNotification.open();
        }
    }
    
    /**
     * Check if the current user has permission to review/approve treatments.
     * MEDICAL_STAFF, OWNER, DOCTOR, and INSTITUTION_ADMIN can approve.
     */
    private boolean hasReviewPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String authority = a.getAuthority();
                    return authority.equals("ROLE_" + AppRoles.MEDICAL_STAFF) ||
                           authority.equals("ROLE_" + AppRoles.OWNER) ||
                           authority.equals("ROLE_" + AppRoles.ADMIN) ||
                           authority.equals("ROLE_" + AppRoles.INSTITUTION_ADMIN) ||
                           authority.equals("ROLE_" + AppRoles.DOCTOR);
                });
    }

    private void reloadTreatments() {
        ensureInstitutionContext();
        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());
    }

    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set.
     * Public für Zugriff aus TreatmentReviewDetailLayout.
     */
    public void ensureInstitutionContext() {
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

    /**
     * Erstellt eine Ampel-Darstellung für den Treatment-Status.
     * Public für Zugriff aus TreatmentReviewDetailLayout.
     */
    public Div createTrafficLight(TreatmentStatus status) {
        Div trafficLight = new Div();
        trafficLight.getStyle()
            .set("width", "24px")
            .set("height", "24px")
            .set("border-radius", "50%")
            .set("flex-shrink", "0");
        
        trafficLight.add(createTrafficLightIcon(status));
        return trafficLight;
    }
    
    /**
     * Erstellt das Icon für die Ampel-Darstellung.
     * Public für Zugriff aus TreatmentReviewDetailLayout.
     */
    public Icon createTrafficLightIcon(TreatmentStatus status) {
        Icon icon = VaadinIcon.CIRCLE.create();
        icon.setSize("24px");
        
        TreatmentStatus.StatusColor color = status.getColor();
        switch (color) {
            case GREEN -> icon.setColor("var(--lumo-success-color)");
            case YELLOW -> icon.setColor("var(--lumo-warning-color)");
            case RED -> icon.setColor("var(--lumo-error-color)");
        }
        
        return icon;
    }
    
    
    /**
     * Berechnet die Wochen seit dem letzten Treatment für das gleiche Auge.
     */
    private int calculateWeeksSinceLastTreatment(Treatment currentTreatment) {
        if (currentTreatment == null || currentTreatment.getTreatmentPlan() == null) {
            return 4; // Standard: 4 Wochen
        }
        
        try {
            ensureInstitutionContext();
            List<Treatment> allTreatments = treatmentRepository.findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(
                currentTreatment.getTreatmentPlan().getId()
            );
            
            SideOfEye sideOfEye = currentTreatment.getSideOfEye();
            LocalDate currentDate = currentTreatment.getDate();
            
            // Finde das letzte Treatment für das gleiche Auge vor dem aktuellen
            Treatment lastTreatment = allTreatments.stream()
                .filter(t -> t.getSideOfEye() == sideOfEye)
                .filter(t -> t.getDate() != null && t.getDate().isBefore(currentDate))
                .max(Comparator.comparing(Treatment::getDate))
                .orElse(null);
            
            if (lastTreatment != null && lastTreatment.getDate() != null) {
                long weeks = ChronoUnit.WEEKS.between(lastTreatment.getDate(), currentDate);
                return (int) Math.max(1, Math.min(weeks, 16)); // Zwischen 1 und 16 Wochen
            }
        } catch (Exception ex) {
            log.warn("Fehler beim Berechnen der Wochen seit dem letzten Treatment", ex);
        }
        
        return 4; // Standard: 4 Wochen
    }
    
    private void generatePatientReport(Treatment treatment) {
        try {
            ensureInstitutionContext();
            String treatingDoctor = authenticationContext.getPrincipalName().orElse("Unbekannt");
            boolean isApproved = treatment.getApprovalDate() != null;
            byte[] pdfBytes = reportService.generatePatientPdfReport(treatment, task.getTimeSlot(), treatingDoctor, isApproved);
            
            // Create filename with patient name
            String patientName = treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null
                    ? treatment.getTreatmentPlan().getPatient().getLastName() + "_" + treatment.getTreatmentPlan().getPatient().getFirstName()
                    : "Patient";
            String prefix = isApproved ? "Behandlungsbericht" : "Vorläufiger_Behandlungsbericht";
            String filename = prefix + "_" + patientName + "_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = isApproved ? "Bericht wird heruntergeladen" : "Vorläufiger Bericht wird heruntergeladen";
            Notification.show(message, 3000, 
                    Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Patienten-Berichts", e);
            Notification notification = Notification.show(
                    "Fehler beim Generieren des Berichts: " + e.getMessage(), 5000, 
                    Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void generateCombinedReport() {
        try {
            ensureInstitutionContext();
            boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
            String treatingDoctor = authenticationContext.getPrincipalName().orElse("Unbekannt");
            byte[] pdfBytes = reportService.generatePdfReport(treatments, task.getTimeSlot(), treatingDoctor, allApproved);
            
            // Create filename with timestamp
            String prefix = allApproved ? "Sammelbericht" : "Vorläufiger_Sammelbericht";
            String filename = prefix + "_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = allApproved ? "Sammelbericht wird heruntergeladen" : "Vorläufiger Sammelbericht wird heruntergeladen";
            Notification.show(message, 3000, 
                    Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Sammelberichts", e);
            Notification notification = Notification.show(
                    "Fehler beim Generieren des Sammelberichts: " + e.getMessage(), 5000, 
                    Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        // Create StreamResource for download
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new ByteArrayInputStream(pdfBytes);
        });
        streamResource.setContentType("application/pdf");
        
        // Register the resource and get the URL
        getUI().ifPresent(ui -> {
            StreamRegistration registration = ui.getSession().getResourceRegistry()
                    .registerResource(streamResource);
            String resourceUrl = registration.getResourceUri().toString();
            
            // Create download link and trigger download via JavaScript
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
     * Dokumentiert alle markierten undokumentierten Behandlungen in einem Durchgang.
     */
    private void approveSelectedTreatments() {
        if (selectedTreatmentsForApproval.isEmpty()) {
            Notification.show("Bitte wählen Sie mindestens eine Behandlung aus.", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        if (!hasReviewPermission()) {
            Notification errorNotification = new Notification(
                "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu dokumentieren.",
                10000,
                Notification.Position.MIDDLE
            );
            errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            errorNotification.open();
            return;
        }
        
        try {
            ensureInstitutionContext();
            String user = authenticationContext.getPrincipalName().orElse("unknown");
            String userId = user;
            
            int approvedCount = 0;
            int failedCount = 0;
            int skippedCount = 0;
            
            // Filtere nur undokumentierte Behandlungen
            List<Long> undocumenteTreatments = selectedTreatmentsForApproval.stream()
                    .filter(treatmentId -> {
                        Treatment treatment = treatments.stream()
                                .filter(t -> t.getId().equals(treatmentId))
                                .findFirst()
                                .orElse(null);
                        return treatment != null && treatment.getApprovalDate() == null;
                    })
                    .collect(Collectors.toList());
            
            if (undocumenteTreatments.isEmpty()) {
                Notification.show("Keine undokumentierten Behandlungen in der Auswahl.", 3000, Notification.Position.MIDDLE);
                return;
            }
            
            for (Long treatmentId : undocumenteTreatments) {
                try {
                    Treatment treatment = treatments.stream()
                            .filter(t -> t.getId().equals(treatmentId))
                            .findFirst()
                            .orElse(null);
                    
                    if (treatment == null || treatment.getApprovalDate() != null) {
                        skippedCount++;
                        continue;
                    }
                    
                    taskService.approveTreatment(treatmentId, userId, user, false);
                    approvedCount++;
                } catch (Exception ex) {
                    log.error("Fehler beim Dokumentieren der Behandlung " + treatmentId, ex);
                    failedCount++;
                }
            }
            
            // Aktualisiere die Liste
            reloadTreatments();
            selectedTreatmentsForApproval.clear();
            
            // Zeige Ergebnis
            if (failedCount == 0 && skippedCount == 0) {
                Notification.show(approvedCount + " Behandlungen erfolgreich dokumentiert", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                String message = approvedCount + " Behandlungen dokumentiert";
                if (skippedCount > 0) {
                    message += ", " + skippedCount + " bereits dokumentiert";
                }
                if (failedCount > 0) {
                    message += ", " + failedCount + " Fehler";
                }
                Notification.show(message, 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
            
            // Aktualisiere die Übersicht (inkl. "Alle auswählen" Checkbox)
            showOverview();
        } catch (Exception ex) {
            log.error("Fehler bei der Massendokumentation", ex);
            Notification errorNotification = new Notification(
                "Fehler bei der Massendokumentation: " + ex.getMessage(),
                5000,
                Notification.Position.MIDDLE
            );
            errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            errorNotification.open();
        }
    }
}
