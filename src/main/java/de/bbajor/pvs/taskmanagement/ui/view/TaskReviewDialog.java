package de.bbajor.pvs.taskmanagement.ui.view;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

    private VerticalLayout mainContent;
    private VerticalLayout overviewLayout;
    private VerticalLayout detailLayout;
    
    // Track which treatments have follow-up bookings for visual feedback
    private java.util.Set<Long> treatmentsWithFollowUpBooking = new java.util.HashSet<>();
    
    // Track selected treatments for bulk approval
    private java.util.Set<Long> selectedTreatmentsForApproval = new java.util.HashSet<>();

    public TaskReviewDialog(Task task, TreatmentRepository treatmentRepository, TaskService taskService,
            AuthenticationContext authenticationContext, TreatmentReportService reportService,
            UserAccountRepository userAccountRepository, ApplicationContext applicationContext,
            TreatmentPlanPresenter treatmentPlanPresenter) {
        this.task = task;
        this.treatmentRepository = treatmentRepository;
        this.taskService = taskService;
        this.authenticationContext = authenticationContext;
        this.reportService = reportService;
        this.userAccountRepository = userAccountRepository;
        this.applicationContext = applicationContext;
        this.treatmentPlanPresenter = treatmentPlanPresenter;

        ensureInstitutionContext();
        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());

        setHeaderTitle("Dokumentation");
        setWidth("900px");
        setHeight("700px");
        setDraggable(true);
        setResizable(true);

        mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(false);

        // Show overview by default
        showOverview();
        
        add(mainContent);
    }

    private void showOverview() {
        mainContent.removeAll();
        
        overviewLayout = new VerticalLayout();
        overviewLayout.setSizeFull();
        
        // Header with task info
        String headerText = task.getDescription() != null ? task.getDescription() : "Behandlungen im Task";
        H3 header = new H3(headerText);
        overviewLayout.add(header);
        
        // Grid mit angepasster Spaltenreihenfolge: Patient, Auge, Medikament, Status & Genehmigung, Dokumentiert, Bericht, Auswahl
        Grid<Treatment> grid = new Grid<>(Treatment.class, false);
        grid.setSizeFull();
        
        // Spalte 1: Patient (mit Renderer für bessere Darstellung)
        grid.addComponentColumn(treatment -> {
            if (treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null) {
                de.bbajor.pvs.patient.model.Patient patient = treatment.getTreatmentPlan().getPatient();
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
        }).setHeader("Patient").setAutoWidth(true).setResizable(true);
        
        // Spalte 2: Auge
        grid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
                .setHeader("Auge").setAutoWidth(true).setResizable(true);
        
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
            statusLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
            
            // Combobox für Status
            ComboBox<TreatmentStatus> statusComboBox = new ComboBox<>();
            List<TreatmentStatus> statusOptions = java.util.Arrays.stream(TreatmentStatus.values())
                    .sorted(java.util.Comparator.comparing(TreatmentStatus::getShortLabel))
                    .collect(java.util.stream.Collectors.toList());
            statusComboBox.setItems(statusOptions);
            statusComboBox.setItemLabelGenerator(TreatmentStatus::getShortLabel);
            statusComboBox.setValue(treatment.getTreatmentStatus() != null 
                    ? treatment.getTreatmentStatus() 
                    : TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL);
            statusComboBox.setWidth("200px");
            
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
        }).setHeader("Status & Genehmigung").setAutoWidth(true).setResizable(true);
        
        // Spalte 5: Dokumentiert (grünes Häkchen oder graues X)
        grid.addComponentColumn(treatment -> {
            boolean isDocumented = treatment.getApprovalDate() != null;
            Icon icon;
            if (isDocumented) {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                icon.setColor("var(--lumo-success-color)");
            } else {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.setColor("var(--lumo-contrast-50pct)");
            }
            icon.setSize("20px");
            return icon;
        }).setHeader("Dokumentiert").setAutoWidth(true).setResizable(true);
        
        // Spalte 6: Bericht
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
        
        // Spalte 7: Auswahl (nur Checkbox für Massengenehmigung)
        grid.addComponentColumn(treatment -> {
            com.vaadin.flow.component.checkbox.Checkbox approvalCheckbox = new com.vaadin.flow.component.checkbox.Checkbox();
            approvalCheckbox.setValue(selectedTreatmentsForApproval.contains(treatment.getId()));
            approvalCheckbox.addValueChangeListener(e -> {
                if (e.getValue()) {
                    selectedTreatmentsForApproval.add(treatment.getId());
                } else {
                    selectedTreatmentsForApproval.remove(treatment.getId());
                }
            });
            approvalCheckbox.setTooltipText("Für Massengenehmigung markieren (bekannte Patienten ohne besondere Vorkommnisse)");
            return approvalCheckbox;
        }).setHeader("Auswahl").setAutoWidth(true).setResizable(true);
        
        grid.setItems(treatments);
        overviewLayout.add(grid);
        
        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        
        Button startReview = new Button("Dokumentation starten", e -> {
            if (treatments.isEmpty()) {
                Notification.show("Keine Behandlungen vorhanden");
                return;
            }
            showTreatmentDetail(0);
        });
        startReview.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startReview.setEnabled(!task.isCompleted() && !treatments.isEmpty());
        
        // Button für Massengenehmigung
        Button approveSelected = new Button("Auswahl dokumentieren", VaadinIcon.CHECK.create(), e -> {
            approveSelectedTreatments();
        });
        approveSelected.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        approveSelected.setEnabled(!selectedTreatmentsForApproval.isEmpty() && !task.isCompleted() && hasReviewPermission());
        if (!hasReviewPermission()) {
            approveSelected.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen dokumentieren");
        }
        
        Button viewReport = new Button("Sammelbericht generieren", VaadinIcon.FILE_TEXT.create(), e -> generateCombinedReport());
        viewReport.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        
        // Always enabled - can generate preliminary reports
        viewReport.setEnabled(!treatments.isEmpty());
        boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        if (!allApproved && !treatments.isEmpty()) {
            viewReport.setTooltipText("Vorläufiger Sammelbericht (nicht alle Behandlungen sind dokumentiert)");
        } else if (!treatments.isEmpty()) {
            viewReport.setTooltipText("Sammelbericht generieren");
        }
        
        buttonLayout.add(startReview, approveSelected, viewReport);
        overviewLayout.add(buttonLayout);
        
        mainContent.add(overviewLayout);
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
        
        detailLayout = new VerticalLayout();
        detailLayout.setSizeFull();
        detailLayout.setPadding(true);
        
        // Header with progress
        H4 header = new H4(String.format("Behandlung %d von %d", index + 1, treatments.size()));
        detailLayout.add(header);
        
        // Treatment info display
        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setPadding(true);
        infoLayout.setSpacing(true);
        infoLayout.getStyle().set("border", "1px solid #ddd");
        infoLayout.getStyle().set("border-radius", "4px");
        
        infoLayout.add(new Span("Patient: " + (treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null 
                ? treatment.getTreatmentPlan().getPatient().toString() : "-")));
        infoLayout.add(new Span("Auge: " + (treatment.getSideOfEye() != null ? treatment.getSideOfEye().toString() : "-")));
        String medicationName = "-";
        if (treatment.getMedicationFavourite() != null && treatment.getMedicationFavourite().getMedication() != null) {
            medicationName = treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
        }
        infoLayout.add(new Span("Medikament: " + medicationName));
        infoLayout.add(new Span("Dosierung: " + (treatment.getDosage() != null ? treatment.getDosage() : "-")));
        infoLayout.add(new Span("Frequenz: " + (treatment.getFrequency() != null ? treatment.getFrequency() : "-")));
        
        // Approval information
        String approvalStatus = "Status: Undokumentiert";
        if (treatment.getApprovalDate() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm");
            if (treatment.getApprovalDateTime() != null) {
                approvalStatus = "Status: Dokumentiert am " + formatter.format(treatment.getApprovalDateTime());
            } else {
                // Fallback: Verwende approvalDate wenn approvalDateTime nicht gesetzt ist
                approvalStatus = "Status: Dokumentiert am " + treatment.getApprovalDate().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
            if (treatment.getApprovedByUserName() != null) {
                approvalStatus += " von " + treatment.getApprovedByUserName();
            }
            if (treatment.getSecondApprovalDateTime() != null) {
                approvalStatus += "\nZweitprüfung: " + formatter.format(treatment.getSecondApprovalDateTime());
                if (treatment.getSecondApprovedByUserName() != null) {
                    approvalStatus += " von " + treatment.getSecondApprovedByUserName();
                }
            }
        }
        infoLayout.add(new Span(approvalStatus));
        
        detailLayout.add(infoLayout);
        
        // Treatment Status Combobox mit Ampel
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setWidthFull();
        statusLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        statusLayout.setSpacing(true);
        
        ComboBox<TreatmentStatus> statusComboBox = new ComboBox<>("Behandlungsstatus");
        statusComboBox.setWidthFull();
        
        // Sortiere Status-Optionen alphanumerisch nach shortLabel
        List<TreatmentStatus> statusOptions = java.util.Arrays.stream(TreatmentStatus.values())
                .sorted(java.util.Comparator.comparing(TreatmentStatus::getShortLabel))
                .collect(java.util.stream.Collectors.toList());
        statusComboBox.setItems(statusOptions);
        statusComboBox.setItemLabelGenerator(TreatmentStatus::getShortLabel);
        
        // Setze initialen Wert
        final TreatmentStatus currentStatus = treatment.getTreatmentStatus() != null 
            ? treatment.getTreatmentStatus() 
            : TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL;
        statusComboBox.setValue(currentStatus);
        
        // Info-Icon mit Tooltip für ausführliche Beschreibung
        Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
        infoIcon.setSize("16px");
        infoIcon.getStyle().set("cursor", "help");
        infoIcon.setTooltipText(currentStatus.getFullDescription());
        
        // Ampel-Darstellung
        Div trafficLight = createTrafficLight(currentStatus);
        
        statusComboBox.addValueChangeListener(e -> {
            TreatmentStatus newStatus = e.getValue();
            if (newStatus != null) {
                try {
                    taskService.updateTreatmentStatus(treatment.getId(), newStatus);
                    treatment.setTreatmentStatus(newStatus);
                    infoIcon.setTooltipText(newStatus.getFullDescription());
                    trafficLight.removeAll();
                    trafficLight.add(createTrafficLightIcon(newStatus));
                    Notification.show("Status aktualisiert", 2000, Notification.Position.BOTTOM_CENTER);
                } catch (Exception ex) {
                    log.error("Fehler beim Aktualisieren des Treatment-Status", ex);
                    Notification errorNotification = new Notification(
                        "Fehler beim Aktualisieren: " + ex.getMessage(),
                        5000,
                        Notification.Position.MIDDLE
                    );
                    errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    errorNotification.open();
                    // Reset combobox to previous value
                    statusComboBox.setValue(currentStatus);
                }
            }
        });
        
        statusLayout.add(statusComboBox, trafficLight, infoIcon);
        statusLayout.setFlexGrow(1, statusComboBox);
        detailLayout.add(statusLayout);
        
        // Additional info input
        TextArea additionalInfoField = new TextArea("Zusätzliche Informationen");
        additionalInfoField.setWidthFull();
        additionalInfoField.setMaxHeight("150px");
        if (treatment.getAdditionalInfo() != null) {
            additionalInfoField.setValue(treatment.getAdditionalInfo());
        }
        detailLayout.add(additionalInfoField);
        
        // Navigation and action buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        
        Button backToOverview = new Button("Zurück zur Übersicht", e -> showOverview());
        backToOverview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        Button prevButton = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            saveAdditionalInfo(treatment, additionalInfoField.getValue());
            showTreatmentDetail(index - 1);
        });
        prevButton.setEnabled(index > 0);
        
        Button nextButton = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            saveAdditionalInfo(treatment, additionalInfoField.getValue());
            showTreatmentDetail(index + 1);
        });
        nextButton.setEnabled(index < treatments.size() - 1);
        
        // Check if user can approve (only MEDICAL_STAFF, OWNER, DOCTOR - not ADMIN)
        boolean canApprove = hasReviewPermission();
        
        Button approveSelected = new Button("Dokumentieren", e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(treatment.getId(), userId, user, false);
                saveAdditionalInfo(treatment, additionalInfoField.getValue());
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
        approveSelected.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        approveSelected.setEnabled(!task.isCompleted() && canApprove);
        if (!canApprove) {
            approveSelected.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen dokumentieren");
        }
        
        Button approveSecond = new Button("Als Zweitprüfer dokumentieren", e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(treatment.getId(), userId, user, true);
                saveAdditionalInfo(treatment, additionalInfoField.getValue());
                Notification.show("Zweitprüfung dokumentiert");
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
        
        // Follow-up booking button
        Button followUpBookingButton = new Button("Folgetermin planen", VaadinIcon.CALENDAR.create(), e -> {
            openFollowUpBookingDialog(treatment, index);
        });
        followUpBookingButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        // Check if follow-up booking is possible
        boolean canBookFollowUp = false;
        try {
            canBookFollowUp = taskService.canBookFollowUpTreatment(treatment.getId());
        } catch (Exception ex) {
            log.warn("Fehler beim Prüfen der Folgetermin-Buchungsmöglichkeit", ex);
        }
        followUpBookingButton.setEnabled(canBookFollowUp && !task.isCompleted());
        if (!canBookFollowUp) {
            followUpBookingButton.setTooltipText("Folgetermin kann nur gebucht werden, wenn der Task noch offen ist und noch keine Folgebuchung existiert");
        }
        
        // Visual indicator if follow-up was already booked
        if (treatmentsWithFollowUpBooking.contains(treatment.getId())) {
            followUpBookingButton.setIcon(VaadinIcon.CHECK_CIRCLE.create());
            followUpBookingButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            followUpBookingButton.setText("Folgetermin gebucht");
            followUpBookingButton.setEnabled(false);
        }
        
        buttonLayout.add(backToOverview, prevButton, nextButton, approveSelected, approveSecond, followUpBookingButton);
        detailLayout.add(buttonLayout);
        
        mainContent.add(detailLayout);
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
                        existingFollowUp.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : 
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
     * Only MEDICAL_STAFF, OWNER, and DOCTOR can approve. ADMIN cannot approve.
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

    /**
     * Erstellt eine Ampel-Darstellung für den Treatment-Status.
     */
    private Div createTrafficLight(TreatmentStatus status) {
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
     */
    private Icon createTrafficLightIcon(TreatmentStatus status) {
        Icon icon = VaadinIcon.CIRCLE.create();
        icon.setSize("24px");
        
        TreatmentStatus.StatusColor color = status.getColor();
        switch (color) {
            case GREEN:
                icon.setColor("var(--lumo-success-color)");
                break;
            case YELLOW:
                icon.setColor("var(--lumo-warning-color)");
                break;
            case RED:
                icon.setColor("var(--lumo-error-color)");
                break;
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
                .max(java.util.Comparator.comparing(Treatment::getDate))
                .orElse(null);
            
            if (lastTreatment != null && lastTreatment.getDate() != null) {
                long weeks = java.time.temporal.ChronoUnit.WEEKS.between(lastTreatment.getDate(), currentDate);
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
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = isApproved ? "Bericht wird heruntergeladen" : "Vorläufiger Bericht wird heruntergeladen";
            Notification.show(message, 3000, 
                    com.vaadin.flow.component.notification.Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Patienten-Berichts", e);
            Notification notification = Notification.show(
                    "Fehler beim Generieren des Berichts: " + e.getMessage(), 5000, 
                    com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
            notification.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
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
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = allApproved ? "Sammelbericht wird heruntergeladen" : "Vorläufiger Sammelbericht wird heruntergeladen";
            Notification.show(message, 3000, 
                    com.vaadin.flow.component.notification.Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Generieren des Sammelberichts", e);
            Notification notification = Notification.show(
                    "Fehler beim Generieren des Sammelberichts: " + e.getMessage(), 5000, 
                    com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
            notification.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        // Create StreamResource for download
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new java.io.ByteArrayInputStream(pdfBytes);
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
     * Dokumentiert alle markierten Behandlungen in einem Durchgang (für bekannte Patienten ohne besondere Vorkommnisse).
     */
    private void approveSelectedTreatments() {
        if (selectedTreatmentsForApproval.isEmpty()) {
            Notification.show("Bitte wählen Sie mindestens eine Behandlung aus (bekannte Patienten ohne besondere Vorkommnisse).", 3000, Notification.Position.MIDDLE);
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
            
            for (Long treatmentId : selectedTreatmentsForApproval) {
                try {
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
            if (failedCount == 0) {
                Notification.show(approvedCount + " Behandlungen erfolgreich dokumentiert", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(approvedCount + " Behandlungen dokumentiert, " + failedCount + " Fehler", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
            
            // Aktualisiere die Übersicht
            showOverview();
        } catch (Exception ex) {
            log.error("Fehler bei der Massengenehmigung", ex);
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
