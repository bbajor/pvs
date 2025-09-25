package de.bbajor.pvs.surgicalcenter.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import jakarta.annotation.security.PermitAll;

@Route("surgicalcenter")
@PageTitle("Operationszentren")
@Menu(order = 4, icon = "vaadin:hospital", title = "Operationszentren")
@PermitAll
public class SurgicalCenterMainView extends Main {

    private final SurgicalCenterListPresenter presenter;
    private final Grid<SurgicalCenterDto> grid = new Grid<>(SurgicalCenterDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");
    private final Button createButton;

    public SurgicalCenterMainView(SurgicalCenterListPresenter presenter) {
        this.presenter = presenter;

        createButton = new Button("Neues Operationszentrum anlegen", event -> {
            SurgicalCenterDto dto = new SurgicalCenterDto();
            dto.setId(Integer.valueOf(-1));
            navigateToDetailView(dto);
        });
        createButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        configureGrid();
        configureSearch();

        add(new ViewToolbar("Operationszentren", ViewToolbar.group(createButton, searchField, searchButton)));
        add(grid);

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private void navigateToDetailView(SurgicalCenterDto surgeryUnitDto) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("surgicalcenter/" + surgeryUnitDto.getId());
    }

    private void configureGrid() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addColumn(SurgicalCenterDto::getName).setHeader("Operationszentrum");
        grid.addColumn(SurgicalCenterDto::getSurgicalCenterAddress).setHeader("Adresse");
        grid.addColumn(SurgicalCenterDto::getPhone).setHeader("Telefonnummer");
        grid.addColumn(SurgicalCenterDto::getEmail).setHeader("E-Mail");
        grid.addColumn(SurgicalCenterDto::getContact).setHeader("Kontakt");
        grid.addColumn(SurgicalCenterDto::getPhoneContact).setHeader("Telefonnummer der Kontaktperson");
        grid.setSizeFull();

        // refresh("");
        grid.setItems(presenter.getAll());

        grid.asSingleSelect().addValueChangeListener(event -> {
            SurgicalCenterDto surgicalCenterDto = event.getValue();
            if (surgicalCenterDto != null) {
                navigateToDetailView(surgicalCenterDto);
            }
        });

    }

    private void configureSearch() {
        // searchButton.addClickListener(e -> refresh(searchField.getValue()));
        // searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

}