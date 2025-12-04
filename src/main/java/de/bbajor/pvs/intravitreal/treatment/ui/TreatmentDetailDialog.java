package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailDialog extends Dialog {

    private final Treatment treatment;
    private final TreatmentPlanService treatmentPlanService;
    private Runnable onTreatmentDeletedOrCancelled;

    public TreatmentDetailDialog(Treatment treatment, TreatmentPlanService treatmentPlanService, 
            UserAccountService userAccountService) {
        this.treatment = treatment;
        this.treatmentPlanService = treatmentPlanService;

        setWidth("1000px");
        setHeight("600px");
        setHeaderTitle("Behandlungsdetails für " + treatment.getPatientInfo());
        setCloseOnOutsideClick(false);
        
        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        TreatmentDetailLayout layout = new TreatmentDetailLayout(
            treatment, 
            treatment.getApprovalDate() == null, 
            treatmentPlanService,
            userAccountService
        );
        layout.setSizeFull();
        add(layout);
        
        // Footer-Buttons: Löschen oder Absagen
        createFooterButtons();
    }
    
    public void setOnTreatmentDeletedOrCancelled(Runnable callback) {
        this.onTreatmentDeletedOrCancelled = callback;
    }
    
    private void createFooterButtons() {
        getFooter().removeAll();
        
        // Prüfe, ob Behandlung in der Zukunft liegt
        LocalDate treatmentDate = treatment.getDate();
        LocalDate today = LocalDate.now();
        boolean isFuture = treatmentDate != null && (treatmentDate.isAfter(today) || treatmentDate.equals(today));
        
        if (isFuture) {
            // Prüfe, ob mindestens 24 Stunden bis zum Termin verbleiben
            LocalDateTime treatmentDateTime = treatmentDate.atStartOfDay();
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilTreatment = ChronoUnit.HOURS.between(now, treatmentDateTime);
            
            if (hoursUntilTreatment >= 24) {
                // Löschen-Button
                Button deleteButton = new Button("Löschen", VaadinIcon.TRASH.create(), e -> confirmDelete());
                deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
                getFooter().add(deleteButton);
            } else {
                // Absagen-Button
                Button cancelButton = new Button("Absagen", VaadinIcon.BAN.create(), e -> showCancelDialog());
                cancelButton.addThemeVariants(ButtonVariant.LUMO_WARNING);
                getFooter().add(cancelButton);
            }
        }
    }
    
    private void confirmDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Behandlung löschen");
        confirmDialog.setText("Möchten Sie diese Behandlung wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.");
        confirmDialog.setConfirmText("Löschen");
        confirmDialog.setCancelText("Abbrechen");
        confirmDialog.setConfirmButtonTheme("error");
        confirmDialog.addConfirmListener(e -> {
            try {
                ensureInstitutionContext();
                treatmentPlanService.deleteTreatment(treatment.getId());
                Notification.show("Behandlung wurde gelöscht", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                if (onTreatmentDeletedOrCancelled != null) {
                    onTreatmentDeletedOrCancelled.run();
                }
                close();
            } catch (Exception ex) {
                Notification notification = Notification.show(
                    "Fehler beim Löschen: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmDialog.open();
    }
    
    private void showCancelDialog() {
        Dialog cancelDialog = new Dialog();
        cancelDialog.setHeaderTitle("Behandlung absagen");
        cancelDialog.setWidth("500px");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        
        Span infoText = new Span("Bitte geben Sie den Grund für die Absage an:");
        TextArea reasonField = new TextArea("Absagegrund");
        reasonField.setRequired(true);
        reasonField.setRequiredIndicatorVisible(true);
        reasonField.setWidthFull();
        reasonField.setMinHeight("100px");
        reasonField.setPlaceholder("z.B. Patient hat abgesagt, medizinische Kontraindikation, ...");
        
        content.add(infoText, reasonField);
        cancelDialog.add(content);
        
        Button confirmButton = new Button("Absagen", VaadinIcon.BAN.create(), e -> {
            String reason = reasonField.getValue();
            if (reason == null || reason.trim().isEmpty()) {
                Notification.show("Bitte geben Sie einen Absagegrund an", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            try {
                ensureInstitutionContext();
                treatmentPlanService.cancelTreatment(treatment.getId(), reason.trim());
                Notification.show("Behandlung wurde abgesagt", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                cancelDialog.close();
                if (onTreatmentDeletedOrCancelled != null) {
                    onTreatmentDeletedOrCancelled.run();
                }
                close();
            } catch (Exception ex) {
                Notification notification = Notification.show(
                    "Fehler beim Absagen: " + ex.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_WARNING);
        
        Button cancelButton = new Button("Abbrechen", e -> cancelDialog.close());
        
        cancelDialog.getFooter().add(cancelButton, confirmButton);
        cancelDialog.open();
    }
    
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        if (treatment.getTreatmentPlan() != null 
                && treatment.getTreatmentPlan().getInstitution() != null
                && treatment.getTreatmentPlan().getInstitution().getId() != null) {
            InstitutionContext.setInstitutionId(treatment.getTreatmentPlan().getInstitution().getId());
        }
    }
}
