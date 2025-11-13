package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailLayout extends FormLayout {

    private final Binder<Treatment> binder = new Binder<>(Treatment.class);

    private final ComboBox<SideOfEye> sideOfEyeComboBox = new ComboBox<>("Seite des Auges");
    private final DatePicker treatmentDatePicker = new DatePicker("Behandlungsdatum");
    private final NativeLabel surgicalCenterLabel = new NativeLabel("Operationszentrum");
    private final NativeLabel timeSlotLabel = new NativeLabel("Uhrzeit");

    private final ComboBox<MedicationFavourite> medicationComboBox = new ComboBox<>("Medikament");
    private final MultiSelectComboBox<UserAccount> treatingDoctorsComboBox = new MultiSelectComboBox<>("Behandelnde Ärzte");
    private final TextArea additionalInfoField = new TextArea("Notizen");
    private final DatePicker approvalDatePicker = new DatePicker("Behandlung geprüft am");

    private final boolean isEditable;

    private final TreatmentPlanService treatmentPlanService;
    private final UserAccountService userAccountService;

    public TreatmentDetailLayout(Treatment treatment, boolean isEditable,
            TreatmentPlanService treatmentPlanService, UserAccountService userAccountService) {
        Objects.requireNonNull(treatment);
        Objects.requireNonNull(treatmentPlanService);
        setSizeFull();
        this.isEditable = isEditable;
        this.treatmentPlanService = treatmentPlanService;
        this.userAccountService = userAccountService;

        surgicalCenterLabel.setTitle("Operationszentrum");
        surgicalCenterLabel.setText(treatment.getSurgicalCenterString());
        add(surgicalCenterLabel);

        timeSlotLabel.setTitle("Uhrzeit der Behandlung");
        timeSlotLabel.setText(treatment.getSurgicalCenterTimeSlot().getStartTime().toString());
        add(timeSlotLabel);

        sideOfEyeComboBox.setItems(SideOfEye.values());
        sideOfEyeComboBox.setValue(treatment.getSideOfEye());
        sideOfEyeComboBox.setItemLabelGenerator(SideOfEye::toString);
        add(sideOfEyeComboBox);

        treatmentDatePicker.setValue(treatment.getDate());
        add(treatmentDatePicker);

        medicationComboBox.setItems(treatmentPlanService.getFavouriteMedications());
        medicationComboBox.setValue(treatment.getMedicationFavourite());
        medicationComboBox.setItemLabelGenerator(MedicationFavourite::getEffectiveDisplayName);
        add(medicationComboBox);

        // Treating doctors selection
        treatingDoctorsComboBox.setItems(userAccountService.findUsersByRole(AppRoles.DOCTOR));
        treatingDoctorsComboBox.setValue(treatment.getTreatingDoctors());
        treatingDoctorsComboBox.setItemLabelGenerator(user -> 
            user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
        treatingDoctorsComboBox.setPlaceholder("Ärzte auswählen");
        add(treatingDoctorsComboBox, 2);

        additionalInfoField.setTitle("Notizen");
        additionalInfoField.setWidthFull();
        additionalInfoField.setHeight("500px");
        additionalInfoField.setPlaceholder("Zusätzliche Informationen...");
        add(additionalInfoField, 2);

        approvalDatePicker.setValue(treatment.getApprovalDate());
        add(approvalDatePicker);

        initializeBinder(treatment);

        setReadOnly();
    }

    private void initializeBinder(Treatment treatment) {
        binder.forField(sideOfEyeComboBox).asRequired("Bitte Seite des Auges auswählen")
                .bind(Treatment::getSideOfEye, Treatment::setSideOfEye);
        binder.forField(treatmentDatePicker).asRequired("Bitte Behandlungsdatum auswählen")
                .bind(Treatment::getDate, (t, v) -> {
                    // no setter available
                });
        binder.forField(medicationComboBox).asRequired("Bitte Medikament auswählen")
                .bind(Treatment::getMedicationFavourite, Treatment::setMedicationFavourite);
        binder.forField(treatingDoctorsComboBox)
                .bind(t -> t.getTreatingDoctors(), 
                      (t, doctors) -> {
                          t.getTreatingDoctors().clear();
                          if (doctors != null) {
                              t.getTreatingDoctors().addAll(doctors);
                          }
                      });
        binder.forField(additionalInfoField).bind(Treatment::getAdditionalInfo, Treatment::setAdditionalInfo);
        binder.forField(approvalDatePicker).bind(Treatment::getApprovalDate, Treatment::setApprovalDate);
        binder.readBean(treatment);
    }

    private void setReadOnly() {
        sideOfEyeComboBox.setReadOnly(!isEditable);
        treatmentDatePicker.setReadOnly(!isEditable);
        medicationComboBox.setReadOnly(!isEditable);
        treatingDoctorsComboBox.setReadOnly(!isEditable);
        additionalInfoField.setReadOnly(!isEditable);
        approvalDatePicker.setReadOnly(!isEditable);
    }
}