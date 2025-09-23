package de.bbajor.pvs.ivomplan.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.ivomplan.controller.IvomChangeListener;
import de.bbajor.pvs.ivomplan.controller.IvomListPresenter;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import jakarta.annotation.security.PermitAll;

@Route("ivom")
@PageTitle("IVOM-Verwaltung")
@Menu(order = 2, icon = "vaadin:calendar-user", title = "IVOM-Verwaltung")
@PermitAll
public class IvomPlanView extends Main implements IvomChangeListener {

    private final IvomListPresenter ivomListPresenter;
    private final Grid<IvomPlanDto> ivomPlanGrid = new Grid<>(IvomPlanDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");
    private final Button newIvomButton;
    private final Button generateDailyListButton;

    public IvomPlanView(IvomListPresenter ivomListPresenter) {
        this.ivomListPresenter = ivomListPresenter;

        setSizeFull();

        newIvomButton = new Button("Neuer IVOM-Plan", event -> {
            Optional<IvomPlanDto> ivom = ivomPlanGrid.getSelectionModel().getFirstSelectedItem();
            if (ivom.isPresent()) {
                openIvomDialog(ivom.get());
            } else {
                openIvomDialog(new IvomPlanDto());
            }
        });

        newIvomButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        generateDailyListButton = new Button("Tagesliste generieren",
                event -> ivomListPresenter.generateDailyList(LocalDate.now()));
        generateDailyListButton
                .setTooltipText("Erzeugt eine Tagesliste für den nächsten anstehenden OP-Slot. " +
                        "Dabei werden die wesentlichen Patientendaten, sowie die jeweiligen Einrichtungen aufgelistet, " +
                        "an denen der Patient behandelt wird.");
        generateDailyListButton.getElement().setAttribute("theme", "primary");

        HorizontalLayout controls = new HorizontalLayout(newIvomButton, searchField, searchButton,
                generateDailyListButton);
        controls.setWidthFull();

        add(controls, ivomPlanGrid);

        configureGrid();
        configureSearch();
    }

    private void configureGrid() {
        ivomPlanGrid.addColumn(IvomPlanDto::getLastName).setHeader("Nachname");
        ivomPlanGrid.addColumn(IvomPlanDto::getFirstName).setHeader("Vorname");
        ivomPlanGrid.addColumn(IvomPlanDto::getBirth).setHeader("Geburtsdatum");
        ivomPlanGrid.addColumn(IvomPlanDto::getHealthInsurance).setHeader("Krankenkasse");
        ivomPlanGrid.addColumn(IvomPlanDto::getDiagnosis).setHeader("Grund der Behandlung");
        ivomPlanGrid.addColumn(IvomPlanDto::getSideOfEye).setHeader("Betroffenes Auge");
        ivomPlanGrid.addColumn(IvomPlanDto::getDrug).setHeader("Medikament");
        ivomPlanGrid.addColumn(IvomPlanDto::getAdditionalInformation).setHeader("Zusätzliche Informationen");
        ivomPlanGrid.setSizeFull();

        refresh("");

        ivomPlanGrid.asSingleSelect().addValueChangeListener(event -> {
            IvomPlanDto ivomDto = event.getValue();
            if (ivomDto != null) {
                openIvomDialog(ivomDto);
            }
        });

    }

    private void configureSearch() {
        searchButton.addClickListener(e -> refresh(searchField.getValue()));
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

    private void openIvomDialog(IvomPlanDto dto) {
        IvomPlanDialog dialog = new IvomPlanDialog(ivomListPresenter.getDialogPresenter());
        dialog.addChangeListener(this);
        dialog.loadIvomById(dto == null ? null : dto.getId());
        dialog.open();
    }

    public void refresh(String searchString) {
        List<IvomPlanDto> ivomList = ivomListPresenter.findAllBy(searchString);
        ivomPlanGrid.setItems(ivomList);
    }

    @Override
    public void onIvomChanged() {
        refresh("");
    }
}