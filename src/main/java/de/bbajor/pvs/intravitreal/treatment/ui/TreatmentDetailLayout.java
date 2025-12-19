package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailLayout extends VerticalLayout {

    private final Binder<Treatment> binder = new Binder<>(Treatment.class);

    private final ComboBox<SideOfEye> sideOfEyeComboBox = new ComboBox<>("Augenseite");
    private final DatePicker treatmentDatePicker = new DatePicker("Behandlungsdatum");
    private final NativeLabel surgicalCenterLabel = new NativeLabel("Operative Einrichtung");
    private final NativeLabel timeSlotLabel = new NativeLabel("Uhrzeit der Behandlung");

    private final ComboBox<MedicationFavourite> medicationComboBox = new ComboBox<>("Medikament");
    private final NativeLabel dosageLabel = new NativeLabel("Dosis");
    private final MultiSelectComboBox<UserAccount> treatingDoctorsComboBox = new MultiSelectComboBox<>("Arzt/Ärztin");
    private final TextArea additionalInfoField = new TextArea();
    private final DatePicker approvalDatePicker = new DatePicker("Dokumentiert am");
    
    // Befund-Felder
    private final Checkbox subretinalFluidCheckbox = new Checkbox("subretinale Flüssigkeit");
    private final Checkbox intraretinalFluidIncreaseCheckbox = new Checkbox("Zunahme intraretinale Flüssigkeit");
    private final Checkbox serousRpeDetachmentIncreaseCheckbox = new Checkbox("Zunahme seröse RPE-Abhebung");
    private final Checkbox newRetinalHemorrhageCheckbox = new Checkbox("neue retinale Blutung");
    private final TextField visualAcuityField = new TextField("Visus");

    private final boolean isEditable;

    private final TreatmentPlanService treatmentPlanService;
    private final UserAccountService userAccountService;

    public TreatmentDetailLayout(Treatment treatment, boolean isEditable,
            TreatmentPlanService treatmentPlanService, UserAccountService userAccountService) {
        Objects.requireNonNull(treatment);
        Objects.requireNonNull(treatmentPlanService);
        setWidthFull();
        setPadding(false);
        setSpacing(true);
        // Keine feste Höhe - Layout soll sich an Inhalt anpassen
        // Scrollbarkeit wird durch Dialog selbst gehandhabt
        this.isEditable = isEditable;
        this.treatmentPlanService = treatmentPlanService;
        this.userAccountService = userAccountService;

        // "Allgemein" und "Befund" nebeneinander
        HorizontalLayout generalFindingsLayout = new HorizontalLayout();
        generalFindingsLayout.setWidthFull();
        generalFindingsLayout.setSpacing(true);
        generalFindingsLayout.setPadding(false);
        
        // Section "Allgemein"
        Div generalSection = createSection("Allgemein");
        generalSection.setWidthFull();
        VerticalLayout generalContent = new VerticalLayout();
        generalContent.setSpacing(true);
        generalContent.setPadding(false);
        generalContent.setWidthFull();
        
        // Zeile 1: Behandlungsdatum, Operative Einrichtung, Uhrzeit (nebeneinander, nur so breit wie nötig)
        HorizontalLayout firstRow = new HorizontalLayout();
        firstRow.setSpacing(true);
        firstRow.setPadding(false);
        firstRow.setAlignItems(Alignment.BASELINE);
        
        treatmentDatePicker.setValue(treatment.getDate());
        treatmentDatePicker.setWidth("auto");
        firstRow.add(treatmentDatePicker);
        
        surgicalCenterLabel.setTitle("Operative Einrichtung");
        surgicalCenterLabel.setText(treatment.getSurgicalCenterString());
        surgicalCenterLabel.getStyle().set("white-space", "nowrap");
        firstRow.add(surgicalCenterLabel);
        
        timeSlotLabel.setTitle("Uhrzeit der Behandlung");
        if (treatment.getSurgicalCenterTimeSlot() != null) {
            timeSlotLabel.setText(treatment.getSurgicalCenterTimeSlot().getStartTime().toString());
        }
        timeSlotLabel.getStyle().set("white-space", "nowrap");
        firstRow.add(timeSlotLabel);
        
        generalContent.add(firstRow);
        
        // Zeile 2: Augenseite (alleine)
        sideOfEyeComboBox.setItems(SideOfEye.values());
        sideOfEyeComboBox.setValue(treatment.getSideOfEye());
        sideOfEyeComboBox.setItemLabelGenerator(SideOfEye::toString);
        sideOfEyeComboBox.setWidthFull();
        sideOfEyeComboBox.addValueChangeListener(e -> updateSideOfEyeStyling());
        updateSideOfEyeStyling();
        generalContent.add(sideOfEyeComboBox);
        
        // Zeile 3: Medikament und Dosis (nebeneinander)
        HorizontalLayout medicationRow = new HorizontalLayout();
        medicationRow.setSpacing(true);
        medicationRow.setPadding(false);
        medicationRow.setWidthFull();
        
        medicationComboBox.setItems(treatmentPlanService.getFavouriteMedications());
        medicationComboBox.setValue(treatment.getMedicationFavourite());
        medicationComboBox.setItemLabelGenerator(MedicationFavourite::getEffectiveDisplayName);
        medicationComboBox.setWidthFull();
        medicationRow.add(medicationComboBox);
        medicationRow.setFlexGrow(1, medicationComboBox);
        
        dosageLabel.setTitle("Dosis");
        dosageLabel.setText(treatment.getDosage() != null && !treatment.getDosage().isEmpty() ? treatment.getDosage() : "-");
        dosageLabel.getStyle().set("white-space", "nowrap");
        dosageLabel.getStyle().set("align-self", "center");
        medicationRow.add(dosageLabel);
        
        generalContent.add(medicationRow);
        
        // Zeile 4: Arzt/Ärztin (alleine)
        treatingDoctorsComboBox.setItems(userAccountService.findUsersByRole(AppRoles.DOCTOR));
        treatingDoctorsComboBox.setValue(treatment.getTreatingDoctors());
        treatingDoctorsComboBox.setItemLabelGenerator(user -> 
            user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
        treatingDoctorsComboBox.setPlaceholder("Ärzte auswählen");
        treatingDoctorsComboBox.setWidthFull();
        generalContent.add(treatingDoctorsComboBox);
        
        generalSection.add(generalContent);
        generalFindingsLayout.add(generalSection);
        generalFindingsLayout.setFlexGrow(1, generalSection);
        
        // Section "Befund (vor der Behandlung)"
        Div findingsSection = createSection("Befund (vor der Behandlung)");
        findingsSection.setWidthFull();
        VerticalLayout findingsContent = new VerticalLayout();
        findingsContent.setSpacing(true);
        findingsContent.setPadding(false);
        findingsContent.setWidthFull();
        
        // Visus zuerst
        visualAcuityField.setValue(treatment.getVisualAcuity() != null ? treatment.getVisualAcuity() : "");
        visualAcuityField.setWidthFull();
        findingsContent.add(visualAcuityField);
        
        // Checkboxen je zu zweit in einer Zeile
        HorizontalLayout checkboxRow1 = new HorizontalLayout();
        checkboxRow1.setSpacing(true);
        checkboxRow1.setPadding(false);
        checkboxRow1.setWidthFull();
        
        subretinalFluidCheckbox.setValue(treatment.getSubretinalFluid() != null ? treatment.getSubretinalFluid() : false);
        checkboxRow1.add(subretinalFluidCheckbox);
        checkboxRow1.setFlexGrow(1, subretinalFluidCheckbox);
        
        intraretinalFluidIncreaseCheckbox.setValue(treatment.getIntraretinalFluidIncrease() != null ? treatment.getIntraretinalFluidIncrease() : false);
        checkboxRow1.add(intraretinalFluidIncreaseCheckbox);
        checkboxRow1.setFlexGrow(1, intraretinalFluidIncreaseCheckbox);
        
        findingsContent.add(checkboxRow1);
        
        HorizontalLayout checkboxRow2 = new HorizontalLayout();
        checkboxRow2.setSpacing(true);
        checkboxRow2.setPadding(false);
        checkboxRow2.setWidthFull();
        
        serousRpeDetachmentIncreaseCheckbox.setValue(treatment.getSerousRpeDetachmentIncrease() != null ? treatment.getSerousRpeDetachmentIncrease() : false);
        checkboxRow2.add(serousRpeDetachmentIncreaseCheckbox);
        checkboxRow2.setFlexGrow(1, serousRpeDetachmentIncreaseCheckbox);
        
        newRetinalHemorrhageCheckbox.setValue(treatment.getNewRetinalHemorrhage() != null ? treatment.getNewRetinalHemorrhage() : false);
        checkboxRow2.add(newRetinalHemorrhageCheckbox);
        checkboxRow2.setFlexGrow(1, newRetinalHemorrhageCheckbox);
        
        findingsContent.add(checkboxRow2);
        
        findingsSection.add(findingsContent);
        generalFindingsLayout.add(findingsSection);
        generalFindingsLayout.setFlexGrow(1, findingsSection);
        
        add(generalFindingsLayout);
        
        // Sections "Bemerkungen" und "Dokumentation" nebeneinander
        HorizontalLayout remarksDocumentationLayout = new HorizontalLayout();
        remarksDocumentationLayout.setWidthFull();
        remarksDocumentationLayout.setSpacing(true);
        remarksDocumentationLayout.setPadding(false);
        
        // Section "Bemerkungen"
        Div remarksSection = createSection("Bemerkungen");
        remarksSection.setWidthFull();
        additionalInfoField.setWidthFull();
        additionalInfoField.setHeight("200px");
        additionalInfoField.setPlaceholder("Zusätzliche Informationen...");
        additionalInfoField.setLabel(null); // Keine Überschrift
        remarksSection.add(additionalInfoField);
        remarksDocumentationLayout.add(remarksSection);
        remarksDocumentationLayout.setFlexGrow(1, remarksSection);
        
        // Section "Dokumentation"
        Div documentationSection = createSection("Dokumentation");
        documentationSection.setWidthFull();
        VerticalLayout documentationContent = new VerticalLayout();
        documentationContent.setSpacing(true);
        documentationContent.setPadding(false);
        documentationContent.setWidthFull();
        
        approvalDatePicker.setValue(treatment.getApprovalDate());
        documentationContent.add(approvalDatePicker);
        
        if (treatment.getApprovalDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            
            // Dokumentierer
            if (treatment.getApprovedByUserName() != null) {
                Span documentedBy = new Span("Dokumentiert von: " + treatment.getApprovedByUserName());
                documentationContent.add(documentedBy);
            }
            
            // Zweitprüfung
            if (treatment.getSecondApprovalDateTime() != null) {
                Span secondApproval = new Span("Zweitprüfung: " + formatter.format(treatment.getSecondApprovalDateTime()));
                documentationContent.add(secondApproval);
                if (treatment.getSecondApprovedByUserName() != null) {
                    Span secondApprovedBy = new Span("Zweitprüfung von: " + treatment.getSecondApprovedByUserName());
                    documentationContent.add(secondApprovedBy);
                }
            } else {
                Span noSecondApproval = new Span("Zweitprüfung: Nicht durchgeführt");
                documentationContent.add(noSecondApproval);
            }
        } else {
            Span pending = new Span("Dokumentation: Ausstehend");
            documentationContent.add(pending);
        }
        
        documentationSection.add(documentationContent);
        remarksDocumentationLayout.add(documentationSection);
        remarksDocumentationLayout.setFlexGrow(1, documentationSection);
        
        add(remarksDocumentationLayout);

        initializeBinder(treatment);

        setReadOnly();
    }
    
    private void updateSideOfEyeStyling() {
        SideOfEye value = sideOfEyeComboBox.getValue();
        // Nur Textfarbe anpassen, keine Hintergrundfarbe
        // Textfarbe wird auf das Input-Element der ComboBox angewendet
        if (value == SideOfEye.RIGHT) {
            sideOfEyeComboBox.getStyle().set("--vaadin-input-field-value-color", "#1976D2"); // Blau für rechts
            sideOfEyeComboBox.getElement().getStyle().set("color", "#1976D2");
        } else if (value == SideOfEye.LEFT) {
            sideOfEyeComboBox.getStyle().set("--vaadin-input-field-value-color", "#F57C00"); // Orange für links
            sideOfEyeComboBox.getElement().getStyle().set("color", "#F57C00");
        } else {
            sideOfEyeComboBox.getStyle().remove("--vaadin-input-field-value-color");
            sideOfEyeComboBox.getElement().getStyle().remove("color");
        }
    }
    
    /**
     * Erstellt eine Section (wie im PatientDialog) statt Accordion.
     */
    private Div createSection(String title) {
        Div section = new Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        section.getStyle().set("display", "flex");
        section.getStyle().set("flex-direction", "column");
        section.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        section.getStyle().set("padding", "var(--lumo-space-m)");
        section.getStyle().set("box-sizing", "border-box");
        section.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        H4 sectionTitle = new H4(title);
        sectionTitle.getStyle().set("margin-top", "0");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        sectionTitle.getStyle().set("color", "var(--lumo-primary-text-color)");
        sectionTitle.getStyle().set("font-size", "var(--lumo-font-size-m)");
        sectionTitle.getStyle().set("font-weight", "600");
        sectionTitle.getStyle().set("flex-shrink", "0");
        section.add(sectionTitle);
        
        return section;
    }

    private void initializeBinder(Treatment treatment) {
        binder.forField(sideOfEyeComboBox).asRequired("Bitte Seite des Auges auswählen")
                .bind(t -> t.getSideOfEye(), (t, v) -> t.setSideOfEye(v));
        binder.forField(treatmentDatePicker).asRequired("Bitte Behandlungsdatum auswählen")
                .bind(Treatment::getDate, (t, v) -> {
                    // no setter available
                });
        binder.forField(medicationComboBox).asRequired("Bitte Medikament auswählen")
                .bind(t -> t.getMedicationFavourite(), (t, v) -> t.setMedicationFavourite(v));
        binder.forField(treatingDoctorsComboBox)
                .bind(t -> t.getTreatingDoctors(), 
                      (t, doctors) -> {
                          t.getTreatingDoctors().clear();
                          if (doctors != null) {
                              t.getTreatingDoctors().addAll(doctors);
                          }
                      });
        binder.forField(additionalInfoField).bind(t -> t.getAdditionalInfo(), (t, v) -> t.setAdditionalInfo(v));
        binder.forField(approvalDatePicker).bind(t -> t.getApprovalDate(), (t, v) -> t.setApprovalDate(v));
        binder.forField(subretinalFluidCheckbox).bind(t -> t.getSubretinalFluid(), (t, v) -> t.setSubretinalFluid(v));
        binder.forField(intraretinalFluidIncreaseCheckbox).bind(t -> t.getIntraretinalFluidIncrease(), (t, v) -> t.setIntraretinalFluidIncrease(v));
        binder.forField(serousRpeDetachmentIncreaseCheckbox).bind(t -> t.getSerousRpeDetachmentIncrease(), (t, v) -> t.setSerousRpeDetachmentIncrease(v));
        binder.forField(newRetinalHemorrhageCheckbox).bind(t -> t.getNewRetinalHemorrhage(), (t, v) -> t.setNewRetinalHemorrhage(v));
        binder.forField(visualAcuityField).bind(t -> t.getVisualAcuity(), (t, v) -> t.setVisualAcuity(v));
        binder.readBean(treatment);
    }

    private void setReadOnly() {
        sideOfEyeComboBox.setReadOnly(!isEditable);
        treatmentDatePicker.setReadOnly(!isEditable);
        medicationComboBox.setReadOnly(!isEditable);
        treatingDoctorsComboBox.setReadOnly(!isEditable);
        additionalInfoField.setReadOnly(!isEditable);
        approvalDatePicker.setReadOnly(!isEditable);
        subretinalFluidCheckbox.setReadOnly(!isEditable);
        intraretinalFluidIncreaseCheckbox.setReadOnly(!isEditable);
        serousRpeDetachmentIncreaseCheckbox.setReadOnly(!isEditable);
        newRetinalHemorrhageCheckbox.setReadOnly(!isEditable);
        visualAcuityField.setReadOnly(!isEditable);
    }
}
