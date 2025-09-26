package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanListPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.IntravitrealTreatmentDto;
import jakarta.annotation.security.PermitAll;

@Route("ivom")
@PageTitle("IVOM-Verwaltung")
@Menu(order = 2, icon = "vaadin:calendar-user", title = "IVOM-Verwaltung")
@PermitAll
public class TreatmentPlanMainView extends Main implements TreatmentPlanChangeListener {

    private final TreatmentPlanListPresenter ivomListPresenter;
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");
    private final Button createButton;
    private final Button generateDailyListButton;
    private final Grid<IntravitrealTreatmentDto> ivomPlanGrid = new Grid<>(IntravitrealTreatmentDto.class, false);

    public TreatmentPlanMainView(TreatmentPlanListPresenter ivomListPresenter) {
        this.ivomListPresenter = ivomListPresenter;

        createButton = new Button("Neuer IVOM-Plan", event -> {
            Optional<IntravitrealTreatmentDto> ivom = ivomPlanGrid.getSelectionModel().getFirstSelectedItem();
            if (ivom.isPresent()) {
                navigateToDetailView(ivom.get());
            } else {
                navigateToDetailView(new IntravitrealTreatmentDto().setId(-1L));
            }
        });
        createButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        generateDailyListButton = new Button("Tagesliste generieren",
                event -> ivomListPresenter.generateDailyList(LocalDate.now()));
        generateDailyListButton
                .setTooltipText("Erzeugt eine Tagesliste für den nächsten anstehenden OP-Slot. " +
                        "Dabei werden die wesentlichen Patientendaten, sowie die jeweiligen Einrichtungen aufgelistet, "
                        +
                        "an denen der Patient behandelt wird.");
        generateDailyListButton.getElement().setAttribute("theme", "primary");

        add(new ViewToolbar("IVOM-Planer",
                ViewToolbar.group(createButton, searchField, searchButton, generateDailyListButton)));
        add(ivomPlanGrid);

        configureGrid();
        configureSearch();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private void navigateToDetailView(IntravitrealTreatmentDto ivomPlanDto) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("ivom/" + ivomPlanDto.getId());
    }

    private void configureGrid() {
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getLastName).setHeader("Nachname");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getFirstName).setHeader("Vorname");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getBirth).setHeader("Geburtsdatum");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getHealthInsurance).setHeader("Krankenkasse");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getDiagnosis).setHeader("Grund der Behandlung");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getSideOfEye).setHeader("Betroffenes Auge");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getDrug).setHeader("Medikament");
        ivomPlanGrid.addColumn(IntravitrealTreatmentDto::getAdditionalInformation).setHeader("Zusätzliche Informationen");
        ivomPlanGrid.setSizeFull();

        refresh("");

        ivomPlanGrid.asSingleSelect().addValueChangeListener(event -> {
            IntravitrealTreatmentDto ivomDto = event.getValue();
            if (ivomDto != null) {
                navigateToDetailView(ivomDto);
            }
        });

    }

    private void configureSearch() {
        searchButton.addClickListener(e -> refresh(searchField.getValue()));
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

    public void refresh(String searchString) {
        List<IntravitrealTreatmentDto> ivomList = ivomListPresenter.findAllBy(searchString);
        ivomPlanGrid.setItems(ivomList);
    }

    @Override
    public void onTreatmentPlanChanged() {
        refresh("");
    }
}