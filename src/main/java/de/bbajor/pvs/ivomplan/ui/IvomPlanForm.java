package de.bbajor.pvs.ivomplan.ui;

import java.util.List;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.ivomplan.dto.TimeSlotDto;
import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomplan.dto.IvomDiagnosisDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.SideOfEye;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

public class IvomPlanForm extends Composite<FormLayout> {

    private Binder<IvomPlanDto> binder = new Binder<>(IvomPlanDto.class);

    final TextField patientSearch = new TextField("Patientensuche");
    final Grid<PatientDto> patientGrid = new Grid<>();
    final Button selectPatient;
    final ComboBox<PatientDto> selectedPatient = new ComboBox<>("Patient");
    final Button deselectPatient;

    final DatePicker creationDate = new DatePicker("Erstellt am");
    final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    final ComboBox<IvomDiagnosisDto> diseaseComboBox = new ComboBox<>("Grund der Behandlung");
    final ComboBox<IvomDrugDto> drugComboBox = new ComboBox<>("Medikament");
    final TextArea additionalInformation = new TextArea("Notizen");
    final HorizontalLayout timeSlotFilter = new HorizontalLayout();
    final Grid<TimeSlotDto> timeSlotGrid = new Grid<>();

    public IvomPlanForm(List<PatientDto> patients) {

        var formLayout = getContent();

        patientGrid.setHeight("150px");
        patientGrid.setWidthFull();
        patientGrid.setSelectionMode(SelectionMode.SINGLE);
        patientGrid.setItems(patients);
        patientGrid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(item -> selectedPatient.setItems(item));
        });

        selectPatient = new Button("Patient auswählen", event -> {
            // TODO Patient in Combobox setzen und Grid sperren
        });

        deselectPatient = new Button("Patientenauswahl zurücksetzen", event -> {

        });

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");

        VerticalLayout patientSelection = new VerticalLayout();
        patientSelection.setHeight("150px");
        patientSelection.setWidthFull();
        patientSelection.add(patientSearch, patientGrid);

        HorizontalLayout patientSelectionButtonBar = new HorizontalLayout();
        patientSelectionButtonBar.setWidthFull();
        patientSelectionButtonBar.add(selectPatient, deselectPatient);

        formLayout.add(patientSelection);

        formLayout.add(creationDate);
        formLayout.add(diseaseComboBox);
        formLayout.add(drugComboBox);
        formLayout.add(sideOfEye);
        formLayout.add(additionalInformation);
        formLayout.add(timeSlotFilter);
        formLayout.add(timeSlotGrid);

    }

    public void setIvom(IvomPlanDto ivom) {
        binder.setBean(ivom);
    }

    public IvomPlanDto geIvomDto() {
        return binder.getBean();
    }

}
