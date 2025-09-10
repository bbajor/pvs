package de.bbajor.pvs.patientsearch.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.service.PatientService;
import jakarta.annotation.security.PermitAll;

import java.util.List;




@Route("patient-search")
@PageTitle("Patientensuche")
@Menu(order = 1, icon = "vaadin:clipboard-check", title = "Patientensuche")
@PermitAll
public class PatientSearchView extends Main {

    private final PatientService patientService;
    private final Grid<Patient> patientGrid = new Grid<>(Patient.class, false);
    private final TextField searchField = new TextField("Suche (Vorname, Nachname, Geburtsdatum, Krankenkasse)");
    private final Button searchButton = new Button("Suchen");

    public PatientSearchView(PatientService patientService) {
        this.patientService = patientService;

        configureGrid();
        configureSearch();

        add(searchField, searchButton, patientGrid);
        updateList("");
    }

    private void configureGrid() {
        patientGrid.addColumn(Patient::getFirstName).setHeader("Vorname");
        patientGrid.addColumn(Patient::getLastName).setHeader("Nachname");
        patientGrid.addColumn(Patient::getBirth).setHeader("Geburtsdatum");
        patientGrid.addColumn(Patient::getHealthInsuranceCard).setHeader("Krankenkasse");
        patientGrid.setSizeFull();
    }

    private void configureSearch() {
        searchButton.addClickListener(e -> updateList(searchField.getValue()));
        searchField.addValueChangeListener(e -> updateList(e.getValue()));
    }

    private void updateList(String filter) {
        List<Patient> patients = patientService.findPatients(filter);
        patientGrid.setItems(patients);
    }
}