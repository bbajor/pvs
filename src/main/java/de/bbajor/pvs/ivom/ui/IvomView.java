package de.bbajor.pvs.ivom.ui;

import java.time.LocalDate;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.ivom.dto.IvomDto;
import jakarta.annotation.security.PermitAll;

@Route("ivom")
@PageTitle("IVOM-Verwaltung")
@Menu(order = 2, icon = "vaadin:clipboard-check", title = "IVOM-Verwaltung")
@PermitAll
public class IvomView extends Main implements IvomChangeListener {

    private final IvomListPresenter ivomListPresenter;
    private final Grid<IvomDto> ivomGrid = new Grid<>(IvomDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");

    public IvomView(IvomListPresenter ivomListPresenter) {
        this.ivomListPresenter = ivomListPresenter;

        setSizeFull();

        Button newIvomButton = new Button("Neue Ivom planen", event -> ivomGrid.getSelectionModel().getFirstSelectedItem().ifPresent(this::openIvomDialog));
        newIvomButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        Button generateDailyListButton = new Button("Tagesliste generieren", event -> ivomListPresenter.generateDailyList(LocalDate.now()));
        generateDailyListButton.getElement().setAttribute("theme", "primary");

        HorizontalLayout controls = new HorizontalLayout(newIvomButton, searchField, searchButton, generateDailyListButton);
        controls.setWidthFull();

        add(controls, ivomGrid);

        configureGrid();
        configureSearch();
    }

    private void configureGrid() {
        ivomGrid.addColumn(IvomDto::getLastName).setHeader("Nachname");
        ivomGrid.addColumn(IvomDto::getFirstName).setHeader("Vorname");
        ivomGrid.addColumn(IvomDto::getBirth).setHeader("Geburtsdatum");
        ivomGrid.addColumn(IvomDto::getHealthInsurance).setHeader("Krankenkasse");
        ivomGrid.addColumn(IvomDto::getDiagnoseIvom).setHeader("Diagnose");
        ivomGrid.addColumn(IvomDto::getCurrentSideOfEye).setHeader("Betroffenes Auge");
        ivomGrid.addColumn(IvomDto::getCurrentDrug).setHeader("Aktuelles Medikament");
        ivomGrid.addColumn(IvomDto::getPlannedDateOfProcedure).setHeader("Nächster Termin");
        ivomGrid.addColumn(IvomDto::getAdditionalInformation).setHeader("Zusätzliche Informationen");
        ivomGrid.setSizeFull();

        refresh("");

        ivomGrid.asSingleSelect().addValueChangeListener(event -> {
            IvomDto ivomDto = event.getValue();
            if (ivomDto != null) {
                openIvomDialog(ivomDto);
            }
        });

    }

    private void configureSearch() {
        searchButton.addClickListener(e -> refresh(searchField.getValue()));
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

    private void openIvomDialog(IvomDto dto) {
        IvomDialog dialog = new IvomDialog(ivomListPresenter.getDialogPresenter());
        dialog.addChangeListener(this);
        dialog.loadIvomById(dto == null ? null : dto.getId());
        dialog.open();
    }

    public void refresh(String searchString) {
        List<IvomDto> ivomList = ivomListPresenter.findAllBy(searchString);
        ivomGrid.setItems(ivomList);
    }

    @Override
    public void onIvomChanged() {
        refresh("");
    }
}