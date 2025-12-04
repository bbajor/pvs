package de.bbajor.pvs.taskmanagement.ui.view;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentStatus;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.taskmanagement.domain.StandardRemark;
import de.bbajor.pvs.taskmanagement.domain.TreatmentRemark;
import de.bbajor.pvs.taskmanagement.service.StandardRemarkService;
import de.bbajor.pvs.taskmanagement.service.TaskService;
import de.bbajor.pvs.taskmanagement.service.TreatmentRemarkService;

public class TreatmentReviewDetailLayout extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(TreatmentReviewDetailLayout.class);

    private final Treatment treatment;
    private final TaskReviewDialog parentDialog;
    private ComboBox<String> dosageComboBox;
    private ComboBox<TreatmentStatus> statusComboBox;
    private Div trafficLight;
    private Icon infoIcon;
    private final StandardRemarkService standardRemarkService;
    private final TreatmentRemarkService treatmentRemarkService;
    private Grid<StandardRemark> availableRemarksGrid;
    private Grid<TreatmentRemark> usedRemarksGrid;
    private List<StandardRemark> availableRemarks = new ArrayList<>();
    private List<TreatmentRemark> usedRemarks = new ArrayList<>();
    private TextField customRemarkField;
    private boolean isDocumented;

    public TreatmentReviewDetailLayout(Treatment treatment, int index, int total, TaskReviewDialog parentDialog,
            StandardRemarkService standardRemarkService, TreatmentRemarkService treatmentRemarkService) {
        this.treatment = treatment;
        this.parentDialog = parentDialog;
        this.standardRemarkService = standardRemarkService;
        this.treatmentRemarkService = treatmentRemarkService;
        this.isDocumented = treatment.getApprovalDate() != null;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        // Header with progress
        H4 header = new H4(String.format("Behandlung %d von %d", index + 1, total));
        header.getStyle().set("flex-shrink", "0");
        add(header);
        
        // Hauptlayout: Links Info-Section, rechts Dosierung/Status-Section
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setAlignItems(Alignment.STRETCH);
        mainLayout.getStyle().set("flex-shrink", "0");
        mainLayout.setWidthFull();
        
        // Linke Section: Patientendaten
        Section infoSection = createInfoSection();
        infoSection.getStyle().set("flex-shrink", "0");
        mainLayout.add(infoSection);
        mainLayout.setFlexGrow(1, infoSection);
        
        // Rechte Section: Dosierung und Status
        Section treatmentSection = createTreatmentSection();
        treatmentSection.setWidth("400px");
        treatmentSection.getStyle().set("flex-shrink", "0");
        mainLayout.add(treatmentSection);
        
        add(mainLayout);
        
        // Bemerkungen-Section: Zwei Listen nebeneinander
        Section remarksSection = createRemarksSection();
        remarksSection.getStyle().set("flex-shrink", "0");
        add(remarksSection);
        setFlexGrow(1, remarksSection);
    }
    
    private Section createInfoSection() {
        Section infoSection = new Section();
        infoSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        
        // Patient
        String patientInfo = "-";
        if (treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null) {
            patientInfo = treatment.getTreatmentPlan().getPatient().toString();
        }
        content.add(new Span("Patient: " + patientInfo));
        
        // Auge
        String eyeInfo = treatment.getSideOfEye() != null ? treatment.getSideOfEye().toString() : "-";
        content.add(new Span("Auge: " + eyeInfo));
        
        // Medikament
        String medicationName = "-";
        if (treatment.getMedicationFavourite() != null && treatment.getMedicationFavourite().getMedication() != null) {
            medicationName = treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
        }
        content.add(new Span("Medikament: " + medicationName));
        
        // Approval information
        String approvalStatus = "Status: Undokumentiert";
        if (treatment.getApprovalDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            if (treatment.getApprovalDateTime() != null) {
                approvalStatus = "Status: Dokumentiert am " + formatter.format(treatment.getApprovalDateTime());
            } else {
                approvalStatus = "Status: Dokumentiert am " + treatment.getApprovalDate().format(
                        DateTimeFormatter.ofPattern("dd.MM.yyyy"));
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
        content.add(new Span(approvalStatus));
        
        infoSection.add(content);
        return infoSection;
    }
    
    private Section createTreatmentSection() {
        Section treatmentSection = new Section();
        treatmentSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);
        content.setWidthFull();
        
        // Dosierung als ComboBox
        dosageComboBox = new ComboBox<>("Dosierung");
        dosageComboBox.setItems("0,5 mg", "1 mg", "1,5 mg", "2 mg", "2,5 mg", "3 mg", "4 mg");
        dosageComboBox.setAllowCustomValue(true);
        dosageComboBox.setWidthFull();
        if (treatment.getDosage() != null) {
            dosageComboBox.setValue(treatment.getDosage());
        }
        dosageComboBox.setEnabled(!isDocumented);
        dosageComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null && !isDocumented) {
                try {
                    parentDialog.ensureInstitutionContext();
                    Treatment treatmentToUpdate = parentDialog.getTreatmentRepository().findById(treatment.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatment.getId()));
                    treatmentToUpdate.setDosage(e.getValue());
                    parentDialog.getTreatmentRepository().save(treatmentToUpdate);
                    treatment.setDosage(e.getValue());
                } catch (Exception ex) {
                    log.error("Fehler beim Aktualisieren der Dosierung", ex);
                    Notification.show("Fehler beim Aktualisieren der Dosierung: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            }
        });
        content.add(dosageComboBox);
        
        // Treatment Status Combobox mit Ampel
        HorizontalLayout statusLayout = new HorizontalLayout();
        statusLayout.setWidthFull();
        statusLayout.setAlignItems(Alignment.CENTER);
        statusLayout.setSpacing(true);
        
        statusComboBox = new ComboBox<>("Behandlungsstatus");
        statusComboBox.setWidthFull();
        
        // Sortiere Status-Optionen alphanumerisch nach shortLabel
        List<TreatmentStatus> statusOptions = Arrays.stream(TreatmentStatus.values())
                .sorted(Comparator.comparing(TreatmentStatus::getShortLabel))
                .collect(Collectors.toList());
        statusComboBox.setItems(statusOptions);
        statusComboBox.setItemLabelGenerator(TreatmentStatus::getShortLabel);
        
        // Setze initialen Wert
        final TreatmentStatus currentStatus = treatment.getTreatmentStatus() != null 
            ? treatment.getTreatmentStatus() 
            : TreatmentStatus.PATIENT_APPEARED_SUCCESSFUL;
        statusComboBox.setValue(currentStatus);
        
        // Info-Icon mit Tooltip für ausführliche Beschreibung
        infoIcon = VaadinIcon.INFO_CIRCLE.create();
        infoIcon.setSize("16px");
        infoIcon.getStyle().set("cursor", "help");
        infoIcon.setTooltipText(currentStatus.getFullDescription());
        
        // Ampel-Darstellung
        trafficLight = parentDialog.createTrafficLight(currentStatus);
        
        statusComboBox.setEnabled(!isDocumented);
        statusComboBox.addValueChangeListener(e -> {
            TreatmentStatus newStatus = e.getValue();
            if (newStatus != null && !isDocumented) {
                try {
                    parentDialog.getTaskService().updateTreatmentStatus(treatment.getId(), newStatus);
                    treatment.setTreatmentStatus(newStatus);
                    infoIcon.setTooltipText(newStatus.getFullDescription());
                    trafficLight.removeAll();
                    trafficLight.add(parentDialog.createTrafficLightIcon(newStatus));
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
                    statusComboBox.setValue(currentStatus);
                }
            }
        });
        
        statusLayout.add(statusComboBox, trafficLight, infoIcon);
        statusLayout.setFlexGrow(1, statusComboBox);
        content.add(statusLayout);
        
        treatmentSection.add(content);
        return treatmentSection;
    }
    
    private Section createRemarksSection() {
        Section remarksSection = new Section();
        remarksSection.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("width", "100%");
        
        HorizontalLayout remarksLayout = new HorizontalLayout();
        remarksLayout.setWidthFull();
        remarksLayout.setSpacing(true);
        remarksLayout.setAlignItems(Alignment.STRETCH);
        
        // Linke Seite: Verfügbare Standardbemerkungen
        VerticalLayout availableLayout = new VerticalLayout();
        availableLayout.setSpacing(true);
        availableLayout.setPadding(false);
        availableLayout.setWidth("50%");
        availableLayout.getStyle().set("flex-shrink", "0");
        
        Span availableLabel = new Span("Verfügbare Bemerkungen");
        availableLabel.getStyle().set("font-weight", "600");
        availableLabel.getStyle().set("font-size", "var(--lumo-font-size-m)");
        availableLayout.add(availableLabel);
        
        // Lade verfügbare Standardbemerkungen
        try {
            parentDialog.ensureInstitutionContext();
            availableRemarks = standardRemarkService.findAllForCurrentInstitution();
        } catch (Exception e) {
            log.error("Fehler beim Laden der Standardbemerkungen", e);
            availableRemarks = new ArrayList<>();
        }
        
        // Lade verwendete Bemerkungen für diese Behandlung
        try {
            parentDialog.ensureInstitutionContext();
            usedRemarks = treatmentRemarkService.findByTreatmentId(treatment.getId());
        } catch (Exception e) {
            log.error("Fehler beim Laden der Behandlungsbemerkungen", e);
            usedRemarks = new ArrayList<>();
        }
        
        // Filtere verfügbare Bemerkungen (entferne bereits verwendete)
        List<StandardRemark> filteredAvailable = availableRemarks.stream()
                .filter(sr -> usedRemarks.stream()
                        .noneMatch(tr -> tr.getStandardRemark() != null && tr.getStandardRemark().getId().equals(sr.getId())))
                .sorted(Comparator.comparing(StandardRemark::getText))
                .collect(Collectors.toList());
        
        availableRemarksGrid = new Grid<>(StandardRemark.class, false);
        availableRemarksGrid.setItems(filteredAvailable);
        availableRemarksGrid.addColumn(StandardRemark::getText)
                .setHeader("Bemerkung")
                .setAutoWidth(true);
        availableRemarksGrid.setHeight("200px");
        availableRemarksGrid.setEnabled(!isDocumented);
        // Textauswahl deaktivieren
        availableRemarksGrid.getStyle().set("user-select", "none");
        availableRemarksGrid.addItemDoubleClickListener(e -> {
            if (!isDocumented && e.getItem() != null) {
                addRemarkToTreatment(e.getItem());
            }
        });
        availableLayout.add(availableRemarksGrid);
        
        // Feld für eigene Bemerkung
        customRemarkField = new TextField("Eigene Bemerkung hinzufügen");
        customRemarkField.setWidthFull();
        customRemarkField.setEnabled(!isDocumented);
        customRemarkField.setPlaceholder("Text eingeben und Enter drücken");
        // Enter-Taste über KeyDownEvent abfangen
        customRemarkField.addKeyDownListener(e -> {
            if (e.getKey().equals(Key.ENTER) && !isDocumented) {
                String text = customRemarkField.getValue();
                if (text != null && !text.trim().isEmpty()) {
                    addCustomRemark(text.trim());
                    customRemarkField.clear();
                }
            }
        });
        availableLayout.add(customRemarkField);
        
        // Rechte Seite: Verwendete Bemerkungen
        VerticalLayout usedLayout = new VerticalLayout();
        usedLayout.setSpacing(true);
        usedLayout.setPadding(false);
        usedLayout.setWidth("50%");
        usedLayout.getStyle().set("flex-shrink", "0");
        
        Span usedLabel = new Span("Verwendete Bemerkungen");
        usedLabel.getStyle().set("font-weight", "600");
        usedLabel.getStyle().set("font-size", "var(--lumo-font-size-m)");
        usedLayout.add(usedLabel);
        
        usedRemarksGrid = new Grid<>(TreatmentRemark.class, false);
        usedRemarksGrid.setItems(usedRemarks.stream()
                .sorted(Comparator.comparing(tr -> tr.getText() != null ? tr.getText() : ""))
                .collect(Collectors.toList()));
        usedRemarksGrid.addColumn(tr -> tr.getStandardRemark() != null && tr.getStandardRemark().getText() != null 
                ? tr.getStandardRemark().getText() 
                : tr.getText())
                .setHeader("Bemerkung")
                .setAutoWidth(true);
        usedRemarksGrid.setHeight("200px");
        usedRemarksGrid.setEnabled(!isDocumented);
        // Textauswahl deaktivieren
        usedRemarksGrid.getStyle().set("user-select", "none");
        usedRemarksGrid.addItemDoubleClickListener(e -> {
            if (!isDocumented && e.getItem() != null) {
                removeRemarkFromTreatment(e.getItem());
            }
        });
        usedLayout.add(usedRemarksGrid);
        
        remarksLayout.add(availableLayout, usedLayout);
        remarksSection.add(remarksLayout);
        
        return remarksSection;
    }
    
    private void addRemarkToTreatment(StandardRemark standardRemark) {
        try {
            parentDialog.ensureInstitutionContext();
            treatmentRemarkService.addStandardRemark(treatment.getId(), standardRemark.getId());
            refreshRemarks();
            Notification.show("Bemerkung hinzugefügt", 2000, Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Hinzufügen der Bemerkung", e);
            Notification.show("Fehler beim Hinzufügen der Bemerkung: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void addCustomRemark(String text) {
        try {
            parentDialog.ensureInstitutionContext();
            treatmentRemarkService.addCustomRemark(treatment.getId(), text);
            refreshRemarks();
            Notification.show("Eigene Bemerkung hinzugefügt", 2000, Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Hinzufügen der eigenen Bemerkung", e);
            Notification.show("Fehler beim Hinzufügen der Bemerkung: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void removeRemarkFromTreatment(TreatmentRemark remark) {
        try {
            parentDialog.ensureInstitutionContext();
            treatmentRemarkService.removeRemark(treatment.getId(), remark.getId());
            refreshRemarks();
            Notification.show("Bemerkung entfernt", 2000, Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            log.error("Fehler beim Entfernen der Bemerkung", e);
            Notification.show("Fehler beim Entfernen der Bemerkung: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void refreshRemarks() {
        // Lade verwendete Bemerkungen neu
        try {
            parentDialog.ensureInstitutionContext();
            usedRemarks = treatmentRemarkService.findByTreatmentId(treatment.getId());
            usedRemarksGrid.setItems(usedRemarks.stream()
                    .sorted(Comparator.comparing(tr -> tr.getText() != null ? tr.getText() : ""))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Fehler beim Aktualisieren der verwendeten Bemerkungen", e);
        }
        
        // Filtere verfügbare Bemerkungen neu
        List<StandardRemark> filteredAvailable = availableRemarks.stream()
                .filter(sr -> usedRemarks.stream()
                        .noneMatch(tr -> tr.getStandardRemark() != null && tr.getStandardRemark().getId().equals(sr.getId())))
                .sorted(Comparator.comparing(StandardRemark::getText))
                .collect(Collectors.toList());
        availableRemarksGrid.setItems(filteredAvailable);
        
    }
}

