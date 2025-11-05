package de.bbajor.pvs.taskmanagement.ui.view;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.spring.security.AuthenticationContext;

import org.springframework.security.access.AccessDeniedException;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;

public class TaskReviewDialog extends Dialog {

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
        
        Button viewReport = new Button("Bericht generieren", e -> generateReport());
        viewReport.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        
        // Check if all treatments are approved
        boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
        viewReport.setEnabled(allApproved && !treatments.isEmpty());
        if (!allApproved && !treatments.isEmpty()) {
            viewReport.setTooltipText("Alle Behandlungen müssen genehmigt sein, bevor ein Bericht generiert werden kann");
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
        
        Button approveSelected = new Button("Approbieren", e -> {
            try {
                String user = authenticationContext.getPrincipalName().orElse("unknown");
                String userId = user;
                taskService.approveTreatment(treatment.getId(), userId, user, false);
                saveAdditionalInfo(treatment, additionalInfoField.getValue());
                Notification.show("Behandlung approbiert");
                reloadTreatments();
                
                // Navigate to next treatment if available, otherwise go back to overview
                if (index < treatments.size() - 1) {
                    showTreatmentDetail(index + 1);
                } else {
                    showOverview();
                }
            } catch (AccessDeniedException ex) {
                Notification errorNotification = new Notification(
                    "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu approbieren. " +
                    "Bitte wenden Sie sich an einen berechtigten Benutzer (z.B. einen Arzt).",
                    10000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            } catch (Exception ex) {
                Notification errorNotification = new Notification(
                    "Fehler beim Approbieren der Behandlung: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                errorNotification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                errorNotification.open();
            }
        });
        approveSelected.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        approveSelected.setEnabled(!task.isCompleted());
        
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
                    "Sie haben nicht die erforderlichen Berechtigungen, um Behandlungen zu approbieren. " +
                    "Bitte wenden Sie sich an einen berechtigten Benutzer (z.B. einen Arzt).",
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
        approveSecond.setEnabled(!task.isCompleted());
        
        buttonLayout.add(backToOverview, prevButton, nextButton, approveSelected, approveSecond);
        detailLayout.add(buttonLayout);
        
        mainContent.add(detailLayout);
    }

    private void saveAdditionalInfo(Treatment treatment, String additionalInfo) {
        taskService.updateTreatmentAdditionalInfo(treatment.getId(), additionalInfo);
    }

    private void reloadTreatments() {
        treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());
    }

    private void generateReport() {
        try {
            // Verify all treatments are approved
            boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
            if (!allApproved) {
                Notification.show("Bitte genehmigen Sie zunächst alle Behandlungen, bevor Sie einen Bericht generieren.", 5000, 
                        com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
                return;
            }
            
            String treatingDoctor = authenticationContext.getPrincipalName().orElse("Unbekannt");
            byte[] pdfBytes = reportService.generatePdfReport(treatments, task.getTimeSlot(), treatingDoctor);
            
            // Use Vaadin 24 DownloadHandler API (non-deprecated)
            DownloadHandler downloadHandler = event -> {
                try (var outputStream = event.getOutputStream()) {
                    outputStream.write(pdfBytes);
                } catch (Exception e) {
                    throw new RuntimeException("Fehler beim Schreiben des PDFs", e);
                }
            };
            
            // Create Anchor component for download using DownloadHandler
            Anchor downloadLink = new Anchor();
            downloadLink.setText("Behandlungsbericht.pdf");
            downloadLink.setHref(downloadHandler);
            downloadLink.getElement().setAttribute("download", true);
            
            // Trigger download
            getUI().ifPresent(ui -> {
                ui.getPage().open(downloadLink.getHref(), "_blank");
            });
            
            Notification.show("Bericht wird heruntergeladen");
        } catch (Exception e) {
            Notification.show("Fehler beim Generieren des Berichts: " + e.getMessage(), 5000, 
                    com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
        }
    }
}
