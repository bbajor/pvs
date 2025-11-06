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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;

public class TaskReviewDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(TaskReviewDialog.class);

    private List<Treatment> treatments;
    private int currentTreatmentIndex = 0;
    private Task task;
    private TreatmentRepository treatmentRepository;
    private TaskService taskService;
    private AuthenticationContext authenticationContext;
    private TreatmentReportService reportService;

    private VerticalLayout mainContent;
    private VerticalLayout overviewLayout;
    private VerticalLayout detailLayout;

    public TaskReviewDialog(Task task, TreatmentRepository treatmentRepository, TaskService taskService,
            AuthenticationContext authenticationContext, TreatmentReportService reportService) {
        this.task = task;
        this.treatmentRepository = treatmentRepository;
        this.taskService = taskService;
        this.authenticationContext = authenticationContext;
        this.reportService = reportService;

        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());

        setHeaderTitle("Behandlungen überprüfen");
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
        
        // Grid with simplified columns (no redundant date/location)
        Grid<Treatment> grid = new Grid<>(Treatment.class, false);
        grid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
                .setHeader("Auge");
        grid.addColumn(t -> t.getMedication() != null ? t.getMedication().getArzneimittelbezeichnung() : "-")
                .setHeader("Medikament");
        grid.addColumn(t -> t.getTreatmentPlan() != null && t.getTreatmentPlan().getPatient() != null 
                ? t.getTreatmentPlan().getPatient().toString() : "-")
                .setHeader("Patient");
        grid.addColumn(t -> t.getApprovalDate() != null ? "Genehmigt" : "Offen")
                .setHeader("Status");
        
        // Add report button column - always enabled, can generate preliminary reports
        grid.addComponentColumn(treatment -> {
            Button reportButton = new Button(VaadinIcon.FILE_TEXT.create(), e -> generatePatientReport(treatment));
            reportButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            boolean isApproved = treatment.getApprovalDate() != null;
            if (isApproved) {
                reportButton.setTooltipText("Bericht für diesen Patienten generieren");
            } else {
                reportButton.setTooltipText("Vorläufigen Bericht generieren (Behandlung noch nicht genehmigt)");
            }
            reportButton.setEnabled(true); // Always enabled
            return reportButton;
        }).setHeader("Bericht");
        
        grid.setItems(treatments);
        grid.setSizeFull();
        overviewLayout.add(grid);
        
        // Buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        
        Button startReview = new Button("Überprüfung starten", e -> {
            if (treatments.isEmpty()) {
                Notification.show("Keine Behandlungen vorhanden");
                return;
            }
            showTreatmentDetail(0);
        });
        startReview.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startReview.setEnabled(!task.isCompleted() && !treatments.isEmpty());
        
        Button viewReport = new Button("Sammelbericht generieren", VaadinIcon.FILE_TEXT.create(), e -> generateCombinedReport());
        viewReport.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        
        // Always enabled - can generate preliminary reports
        viewReport.setEnabled(!treatments.isEmpty());
        boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        if (!allApproved && !treatments.isEmpty()) {
            viewReport.setTooltipText("Vorläufiger Sammelbericht (nicht alle Behandlungen sind genehmigt)");
        } else if (!treatments.isEmpty()) {
            viewReport.setTooltipText("Sammelbericht generieren");
        }
        
        buttonLayout.add(startReview, viewReport);
        overviewLayout.add(buttonLayout);
        
        mainContent.add(overviewLayout);
    }

    private void showTreatmentDetail(int index) {
        if (index < 0 || index >= treatments.size()) {
            return;
        }
        
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
        infoLayout.add(new Span("Medikament: " + (treatment.getMedication() != null ? treatment.getMedication().getArzneimittelbezeichnung() : "-")));
        infoLayout.add(new Span("Dosierung: " + (treatment.getDosage() != null ? treatment.getDosage() : "-")));
        infoLayout.add(new Span("Frequenz: " + (treatment.getFrequency() != null ? treatment.getFrequency() : "-")));
        
        // Approval information
        String approvalStatus = "Status: Offen";
        if (treatment.getApprovalDate() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("dd.MM.yyyy HH:mm");
            approvalStatus = "Status: Geprüft am " + formatter.format(treatment.getApprovalDateTime());
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
        
        Button approveSelected = new Button("Genehmigen", e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(treatment.getId(), userId, user, false);
                saveAdditionalInfo(treatment, additionalInfoField.getValue());
                Notification.show("Behandlung genehmigt");
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
                    "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu genehmigen.",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler beim Genehmigen der Behandlung: " + ex.getMessage(),
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
            approveSelected.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen genehmigen");
        }
        
        Button approveSecond = new Button("Als Zweitprüfer bestätigen", e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(treatment.getId(), userId, user, true);
                saveAdditionalInfo(treatment, additionalInfoField.getValue());
                Notification.show("Zweitprüfung durchgeführt");
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
                    "Sie haben nicht die erforderlichen Berechtigungen für die Zweitprüfung.",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler bei der Zweitprüfung: " + ex.getMessage(),
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
            approveSecond.setTooltipText("Nur MFA, Inhaber und Ärzte können Behandlungen genehmigen");
        }
        
        buttonLayout.add(backToOverview, prevButton, nextButton, approveSelected, approveSecond);
        detailLayout.add(buttonLayout);
        
        mainContent.add(detailLayout);
    }

    private void saveAdditionalInfo(Treatment treatment, String additionalInfo) {
        taskService.updateTreatmentAdditionalInfo(treatment.getId(), additionalInfo);
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
        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());
    }

    private void generatePatientReport(Treatment treatment) {
        try {
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
}
