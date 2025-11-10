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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import de.bbajor.pvs.ai.extraction.ExtractionOrchestrator;
import de.bbajor.pvs.ai.service.ExtractionClient;
import de.bbajor.pvs.ai.service.VoiceTranscriptionService;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.ophthalmology.presenter.OphthalmologyAppointmentPresenter;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.presenter.PatientListPresenter;
import de.bbajor.pvs.security.AppRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

@Route("patient-search")
@PageTitle("Patientenverwaltung")
@Menu(order = 1, icon = "vaadin:male", title = "Patientenverwaltung")
@PermitAll
public class PatientMainView extends Main implements PatientChangeListener, BeforeEnterObserver {

    private final PatientListPresenter patientListPresenter;
    private final VoiceTranscriptionService transcriptionService;
    private final ExtractionOrchestrator extractionOrchestrator;
    private Grid<Patient> patientGrid;

    private final DateTimeFormatter germanFormatter = DateAndTimeUtils.getGermanDateTimeFormatter();

    public PatientMainView(PatientListPresenter patientListPresenter, VoiceTranscriptionService transcriptionService, ExtractionOrchestrator extractionOrchestrator) {
        this.patientListPresenter = patientListPresenter;
        this.transcriptionService = transcriptionService;
        this.extractionOrchestrator = extractionOrchestrator;
        
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        setSizeFull();

        Button newPatientButton = new Button("Patienten anlegen", event -> openPatientDialog(new Patient()));
        newPatientButton.setIcon(VaadinIcon.USER.create());
        newPatientButton.getElement().setAttribute("theme", "primary");

        add(new ViewToolbar("Übersicht Patienten", ViewToolbar.group(newPatientButton)));
        configureGrid();
    }

    private void configureGrid() {
        if (patientGrid != null) {
            remove(patientGrid);
        }
        patientGrid = new Grid<>(Patient.class, false);
        Grid.Column<Patient> lastNameColumn = patientGrid.addColumn(Patient::getLastName).setHeader("Nachname");
        Grid.Column<Patient> firstNameColumn = patientGrid.addColumn(Patient::getFirstName).setHeader("Vorname");
        Grid.Column<Patient> birthColumn = patientGrid
                .addColumn(dto -> dto != null && dto.getBirth() != null ? germanFormatter.format(dto.getBirth()) : "-")
                .setHeader("Geburtsdatum");
        Grid.Column<Patient> insuranceColumn = patientGrid.addColumn(Patient::getHealthInsurance)
                .setHeader("Krankenkasse");

        GridListDataView<Patient> dataView = patientGrid.setItems(patientListPresenter.findAll());
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
            Patient patientDto = event.getValue();
            if (patientDto != null) {
                openPatientDialog(patientDto);
            }
        });

        add(patientGrid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access patient data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
        }
    }

    private void openPatientDialog(Patient dto) {
        PatientDialog dialog = new PatientDialog(patientListPresenter.getDialogPresenter(), dto, 
                new ExtractionClient(extractionOrchestrator), transcriptionService);
        dialog.addChangeListener(this);
        dialog.open();
    }

    @Override
    public void onPatientChanged(Patient patientDto) {
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
        private final GridListDataView<Patient> dataView;

        private String lastName;
        private String firstName;
        private String birth;
        private String insuranceId;

        public PatientFilter(GridListDataView<Patient> dataView) {
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

        public boolean test(Patient patient) {
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