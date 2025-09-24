package de.bbajor.pvs.ivomplan.ui;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomplan.controller.IvomDialogPresenter;
import de.bbajor.pvs.ivomplan.dto.IvomDiagnosisDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanTimeSlotDto;
import de.bbajor.pvs.ivomplan.dto.SideOfEye;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

public class IvomPlanForm extends Composite<FormLayout> {

    private Binder<IvomPlanDto> binder = new Binder<>(IvomPlanDto.class);

    final DatePicker creationDate = new DatePicker("Erstellt am");
    final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    final ComboBox<IvomDiagnosisDto> diseaseComboBox = new ComboBox<>("Behandlungsgrund");
    final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    final ComboBox<IvomDrugDto> ivomDrugsComboBox = new ComboBox<>("Medikament");
    final ComboBox<SurgeryUnitDto> surgeryUnitComboBox = new ComboBox<>("Behandlungsort");
    final TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    final Button filterTimeSlotsButton = new Button("Verfügbare Slots filtern");
    final Grid<IvomPlanTimeSlotDto> timeSlotGrid = new Grid<>();
    final TextArea additionalInformation = new TextArea("Notizen");

    // private final IvomDialogPresenter presenter;

    public IvomPlanForm(IvomDialogPresenter presenter) {
        // this.presenter = presenter;

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

        timeSlotGrid.addColumn(IvomPlanTimeSlotDto::getTimeSlotSurgeryUnit).setHeader("Einrichtung");
        timeSlotGrid.addColumn(IvomPlanTimeSlotDto::getDate).setHeader("Datum");
        timeSlotGrid.addColumn(IvomPlanTimeSlotDto::getDate)
                .setRenderer(new ComponentRenderer<NativeLabel, IvomPlanTimeSlotDto>(dto -> {
                    return new NativeLabel(dto.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.GERMAN));
                })).setHeader("Wochentag");

        surgeryUnitComboBox.setItems(presenter.getSurgeryUnits());
        surgeryUnitComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDate.setEnabled(false);

        diseaseComboBox.setItemLabelGenerator(IvomDiagnosisDto::getName);
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
                IvomDiagnosisDto saved = presenter.saveDiagnosis(newDto);

                // ComboBox aktualisieren
                List<IvomDiagnosisDto> items = new ArrayList<>(
                        diseaseComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(saved);
                diseaseComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                diseaseComboBox.setValue(newDto);
            }
        });
        diseaseComboBox.setItems(presenter.getDiseases());

        filterTimeSlotsButton.addClickListener(click -> {
            List<SurgeryUnitTimeSlotDto> filteredTimeSlots = new ArrayList<>(
                    presenter.getAllTimeSlotsFilteredBy(timeSlotConfigForm.getCurrentConfig(),
                            surgeryUnitComboBox.getValue()));
            List<IvomPlanTimeSlotDto> bookedTimeSlots = new ArrayList<>();
            for (SurgeryUnitTimeSlotDto availableSlot : filteredTimeSlots) {
                IvomPlanTimeSlotDto bookedTimeSlot = new IvomPlanTimeSlotDto();
                bookedTimeSlot.setIvomPlan(geIvomDto());
                bookedTimeSlot.setTimeSlotSurgeryUnit(availableSlot);
                bookedTimeSlots.add(bookedTimeSlot);
            }
            timeSlotGrid.setItems(bookedTimeSlots);
        });

        var formLayout = getContent();
        formLayout.add(creationDate);
        formLayout.add(patientSelectComboBox);
        formLayout.add(diseaseComboBox);
        formLayout.add(sideOfEye);
        formLayout.add(ivomDrugsComboBox);
        formLayout.add(surgeryUnitComboBox);
        formLayout.add(timeSlotConfigForm);
        formLayout.add(filterTimeSlotsButton);
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
