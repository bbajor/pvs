package de.bbajor.pvs.patient.ui.view;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.presenter.PatientListPresenter;
import jakarta.annotation.security.PermitAll;

@Route("patient-search")
@PageTitle("Patientenverwaltung")
@Menu(order = 1, icon = "vaadin:male", title = "Patientenverwaltung")
@PermitAll
public class PatientMainView extends Main implements PatientChangeListener {

    private final PatientListPresenter patientListPresenter;
    private Grid<PatientDto> patientGrid;

    private final DateTimeFormatter germanFormatter = DateAndTimeUtils.getGermanDateTimeFormatter();

    public PatientMainView(PatientListPresenter patientListPresenter) {
        this.patientListPresenter = patientListPresenter;
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        setSizeFull();

        Button newPatientButton = new Button("Patienten anlegen", event -> openPatientDialog(new PatientDto()));
        newPatientButton.setIcon(VaadinIcon.USER.create());
        newPatientButton.getElement().setAttribute("theme", "primary");

        add(new ViewToolbar("Übersicht Patienten", ViewToolbar.group(newPatientButton)));
        configureGrid();
    }

    private void configureGrid() {
        if (patientGrid != null) {
            remove(patientGrid);
        }
        patientGrid = new Grid<>(PatientDto.class, false);
        Grid.Column<PatientDto> lastNameColumn = patientGrid.addColumn(PatientDto::getLastName).setHeader("Nachname");
        Grid.Column<PatientDto> firstNameColumn = patientGrid.addColumn(PatientDto::getFirstName).setHeader("Vorname");
        Grid.Column<PatientDto> birthColumn = patientGrid
                .addColumn(dto -> dto != null && dto.getBirth() != null ? germanFormatter.format(dto.getBirth()) : "-")
                .setHeader("Geburtsdatum");
        Grid.Column<PatientDto> insuranceColumn = patientGrid.addColumn(PatientDto::getHealthInsurance)
                .setHeader("Krankenkasse");

        GridListDataView<PatientDto> dataView = patientGrid.setItems(patientListPresenter.findAll());
        PatientFilter patientFilter = new PatientFilter(dataView);
        patientGrid.getHeaderRows().clear();
        HeaderRow headerRow = patientGrid.appendHeaderRow();

        headerRow.getCell(lastNameColumn).setComponent(
                createFilterHeader("Nachname", patientFilter::setLastName));
        headerRow.getCell(firstNameColumn).setComponent(
                createFilterHeader("Vorname", patientFilter::setFirstName));
        headerRow.getCell(birthColumn).setComponent(
                createFilterHeader("Geburtstag", patientFilter::setBirth));
        headerRow.getCell(insuranceColumn).setComponent(
                createFilterHeader("Versicherung", patientFilter::setInsuranceId));

        patientGrid.setSizeFull();
        patientGrid.asSingleSelect().addValueChangeListener(event -> {
            PatientDto patientDto = event.getValue();
            if (patientDto != null) {
                openPatientDialog(patientDto);
            }
        });

        add(patientGrid);
    }

    private void openPatientDialog(PatientDto dto) {
        PatientDialog dialog = new PatientDialog(patientListPresenter.getDialogPresenter(), dto);
        dialog.addChangeListener(this);
        dialog.open();
    }

    @Override
    public void onPatientChanged(PatientDto patientDto) {
        configureGrid();
    }

    private static Component createFilterHeader(String labelText,
            Consumer<String> filterChangeConsumer) {
        NativeLabel label = new NativeLabel(labelText);
        label.getStyle().set("padding-top", "var(--lumo-space-m)")
                .set("font-size", "var(--lumo-font-size-xs)");
        TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setClearButtonVisible(true);
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setWidthFull();
        textField.getStyle().set("max-width", "100%");
        textField.addValueChangeListener(
                e -> filterChangeConsumer.accept(e.getValue()));
        VerticalLayout layout = new VerticalLayout(label, textField);
        layout.getThemeList().clear();
        layout.getThemeList().add("spacing-xs");

        return layout;
    }

    private static class PatientFilter {
        private final GridListDataView<PatientDto> dataView;

        private String lastName;
        private String firstName;
        private String birth;
        private String insuranceId;

        public PatientFilter(GridListDataView<PatientDto> dataView) {
            this.dataView = dataView;
            this.dataView.addFilter(this::test);
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
            this.dataView.refreshAll();
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
            this.dataView.refreshAll();
        }

        public void setBirth(String birth) {
            this.birth = birth;
            this.dataView.refreshAll();
        }

        public void setInsuranceId(String insuranceId) {
            this.insuranceId = insuranceId;
            this.dataView.refreshAll();
        }

        public boolean test(PatientDto patient) {
            boolean matchesFirstName = matches(patient.getFirstName(), firstName);
            boolean matchesLastName = matches(patient.getLastName(), lastName);
            boolean matchesBirth = matches(patient != null && patient.getBirth() != null
                    ? DateAndTimeUtils.getGermanDateTimeFormatter().format(patient.getBirth())
                    : "", birth);
            boolean matchesInsurance = matches(
                    patient.getHealthInsurance() != null ? patient.getHealthInsurance().toString() : "", insuranceId);
            return matchesFirstName && matchesLastName && matchesBirth && matchesInsurance;
        }

        private boolean matches(String value, String searchTerm) {
            return searchTerm == null || searchTerm.isEmpty()
                    || value.toLowerCase().contains(searchTerm.toLowerCase());
        }
    }
}