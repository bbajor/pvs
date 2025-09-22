package de.bbajor.pvs.ivomplan.ui;

import java.util.List;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomplan.dto.IvomDiagnosisDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.SideOfEye;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

public class IvomPlanForm extends Composite<FormLayout> {

    private Binder<IvomPlanDto> binder = new Binder<>(IvomPlanDto.class);

    final DatePicker creationDate = new DatePicker("Erstellt am");
    final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    final ComboBox<IvomDiagnosisDto> diseaseComboBox = new ComboBox<>("Grund der Behandlung");
    final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    final ComboBox<IvomDrugDto> ivomDrugsComboBox = new ComboBox<>("Medikament");
    final ComboBox<SurgeryUnitDto> surgeryUnitComboBox = new ComboBox<>("Behandlungsort");
    final HorizontalLayout timeSlotFilter = new HorizontalLayout();
    final Grid<SurgeryUnitTimeSlotDto> timeSlotGrid = new Grid<>();
    final TextArea additionalInformation = new TextArea("Notizen");

    public IvomPlanForm(List<PatientDto> patients, List<IvomDrugDto> ivomDrugs, List<SurgeryUnitDto> surgeryUnitDtos) {

        sideOfEye.setItems(SideOfEye.values());
        patientSelectComboBox.setItems(patients);
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new IvomPlanDto());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        ivomDrugsComboBox.setItems(ivomDrugs);
        surgeryUnitComboBox.setItems(surgeryUnitDtos);
        surgeryUnitComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                SurgeryUnitDto selectedSurgeryUnit = event.getValue();
                if (selectedSurgeryUnit.getAvailableTimeSlots() != null) {
                    timeSlotGrid.setItems(selectedSurgeryUnit.getAvailableTimeSlots());
                }
            }
        });

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDate.setEnabled(false);

        var formLayout = getContent();
        formLayout.add(creationDate);
        formLayout.add(patientSelectComboBox);
        formLayout.add(diseaseComboBox);
        formLayout.add(sideOfEye);
        formLayout.add(ivomDrugsComboBox);
        formLayout.add(surgeryUnitComboBox);
        formLayout.add(timeSlotFilter);
        formLayout.add(timeSlotGrid);
        formLayout.add(additionalInformation);

        binder.forField(creationDate).bind(IvomPlanDto::getCreationDate, IvomPlanDto::setCreationDate);
        binder.forField(sideOfEye).bind(IvomPlanDto::getSideOfEye, IvomPlanDto::setSideOfEye);

    }

    public void setIvom(IvomPlanDto ivom) {
        binder.setBean(ivom);
    }

    public IvomPlanDto geIvomDto() {
        return binder.getBean();
    }

}
