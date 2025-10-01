package de.bbajor.pvs.patient.ui.view;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.presenter.PatientListPresenter;
import jakarta.annotation.security.PermitAll;

@Route("patient-search")
@PageTitle("Patientensuche")
@Menu(order = 1, icon = "vaadin:male", title = "Patientensuche")
@PermitAll
public class PatientMainView extends Main implements PatientChangeListener {

    private final PatientListPresenter patientListPresenter;
    private final Grid<PatientDto> patientGrid = new Grid<>(PatientDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");

    private final DateTimeFormatter germanFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy") // klassisch deutsch
            .withLocale(Locale.GERMAN);

    public PatientMainView(PatientListPresenter patientListPresenter) {
        this.patientListPresenter = patientListPresenter;

        Button newPatientButton = new Button("Neuer Patient", event -> openPatientDialog(null));
        newPatientButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        configureGrid();
        configureSearch();
        add(new ViewToolbar("Patientensuche", ViewToolbar.group(newPatientButton, searchField, searchButton)));
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        add(patientGrid);
    }

    private void configureGrid() {
        patientGrid.addColumn(PatientDto::getLastName).setHeader("Nachname");
        patientGrid.addColumn(PatientDto::getFirstName).setHeader("Vorname");
        patientGrid
                .addColumn(dto -> dto != null && dto.getBirth() != null ? germanFormatter.format(dto.getBirth()) : "-")
                .setHeader("Geburtsdatum");
        patientGrid.addColumn(PatientDto::getHealthInsurance).setHeader("Krankenkasse");
        patientGrid.setSizeFull();

        patientGrid.setItems(patientListPresenter.findAll());

        patientGrid.asSingleSelect().addValueChangeListener(event -> {
            PatientDto patientDto = event.getValue();
            if (patientDto != null) {
                openPatientDialog(patientDto);
            }
        });

    }

    private void configureSearch() {
        searchButton.addClickListener(e -> refresh(searchField.getValue()));
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

    private void openPatientDialog(PatientDto dto) {
        PatientDialog dialog = new PatientDialog(patientListPresenter.getDialogPresenter(), dto);
        dialog.addChangeListener(this);
        dialog.open();
    }

    public void refresh(String searchString) {
        List<PatientDto> patientList = patientListPresenter.findAllBy(searchString);
        patientGrid.setItems(patientList);
    }

    @Override
    public void onPatientChanged() {
        refresh("");
    }
}