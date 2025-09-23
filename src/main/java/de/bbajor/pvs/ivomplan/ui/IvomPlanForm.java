package de.bbajor.pvs.ivomplan.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomplan.controller.IvomDialogPresenter;
import de.bbajor.pvs.ivomplan.dto.IvomDiagnosisDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.SideOfEye;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

public class IvomPlanForm extends Composite<FormLayout> {

    private Binder<IvomPlanDto> binder = new Binder<>(IvomPlanDto.class);

    final DatePicker creationDate = new DatePicker("Erstellt am");
    final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    final ComboBox<IvomDiagnosisDto> diseaseComboBox = new ComboBox<>("Behandlungsgrund");
    final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    final ComboBox<IvomDrugDto> ivomDrugsComboBox = new ComboBox<>("Medikament");
    final ComboBox<SurgeryUnitDto> surgeryUnitComboBox = new ComboBox<>("Behandlungsort");
    final HorizontalLayout timeSlotFilter = new HorizontalLayout();
    final Grid<SurgeryUnitTimeSlotDto> timeSlotGrid = new Grid<>(SurgeryUnitTimeSlotDto.class);
    final TextArea additionalInformation = new TextArea("Notizen");

    private final IvomDialogPresenter presenter;

    public IvomPlanForm(IvomDialogPresenter presenter) {
        this.presenter = presenter;

        sideOfEye.setItems(SideOfEye.values());
        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new IvomPlanDto());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        ivomDrugsComboBox.setItems(presenter.getDrugs());

        timeSlotGrid.addColumn(SurgeryUnitTimeSlotDto::getSurgeryUnit).setHeader("Einrichtung");
        timeSlotGrid.addColumn(SurgeryUnitTimeSlotDto::getDate).setHeader("Datum");
        timeSlotGrid.addColumn(SurgeryUnitTimeSlotDto::getStartTime).setHeader("Von");
        timeSlotGrid.addColumn(SurgeryUnitTimeSlotDto::getEndTime).setHeader("Bis");
        timeSlotGrid.addColumn(SurgeryUnitTimeSlotDto::getDescription).setHeader("Beschreibung");

        surgeryUnitComboBox.setItems(presenter.getSurgeryUnits());
        surgeryUnitComboBox.addValueChangeListener(event -> {
            SurgeryUnitDto selectedSurgeryUnit = event.getValue();
            List<SurgeryUnitTimeSlotDto> availableTimeSlots = presenter
                    .loadAvailableSurgeryUnitTimeSlots(selectedSurgeryUnit);
            timeSlotGrid.setItems(availableTimeSlots);
        });

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDate.setReadOnly(true);

        diseaseComboBox.setItemLabelGenerator(IvomDiagnosisDto::getDescription);
        diseaseComboBox.setAllowCustomValue(true);
        diseaseComboBox.setClearButtonVisible(true);
        diseaseComboBox.addCustomValueSetListener(event -> {
            String newValue = event.getDetail();
            // Optional: trim & prüfen
            if (newValue != null && !newValue.trim().isEmpty()) {
                // Neues Dto
                IvomDiagnosisDto newDto = new IvomDiagnosisDto();
                newDto.setName(newValue.trim());

                // In DB speichern, falls nötig
                presenter.saveDiagnosis(newDto);

                // ComboBox aktualisieren
                List<IvomDiagnosisDto> items = new ArrayList<>(
                        diseaseComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(newDto);
                diseaseComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                diseaseComboBox.setValue(newDto);
            }
        });

        var formLayout = getContent();
        formLayout.add(creationDate);
        formLayout.add(patientSelectComboBox);
        formLayout.add(diseaseComboBox);
        formLayout.add(sideOfEye);
        formLayout.add(ivomDrugsComboBox);
        formLayout.add(surgeryUnitComboBox);
        formLayout.add(timeSlotFilter);
        formLayout.add(timeSlotGrid, 2);
        formLayout.add(additionalInformation, 2);

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
