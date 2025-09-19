package de.bbajor.pvs.ivomplan.ui;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import jakarta.annotation.security.PermitAll;

@Route("surgeryunit")
@PageTitle("Operationszentren")
@Menu(order = 4, icon = "vaadin:clipboard-check", title = "Operationszentren")
@PermitAll
public class SurgeryUnitView extends Main {

    // private final SurgeryUnitListPresenter surgeryUnitListPresenter;
    private final Grid<SurgeryUnitDto> grid = new Grid<>(SurgeryUnitDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Suchen");
    private final Button newSurgeryUnitButton;

    public SurgeryUnitView() {

        setSizeFull();

        newSurgeryUnitButton = new Button("Neue Ambulanz anlegen", event -> {
            SurgeryUnitDto dto = new SurgeryUnitDto();
            dto.setId(-1);
            navigateToDetailView(dto);
        });

        newSurgeryUnitButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        HorizontalLayout controls = new HorizontalLayout(newSurgeryUnitButton, searchField, searchButton);
        controls.setWidthFull();

        add(controls, grid);
        configureGrid();
        configureSearch();
    }

    private void navigateToDetailView(SurgeryUnitDto surgeryUnitDto) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("surgeryunit/" + String.valueOf(surgeryUnitDto.getId()));
    }

    private void configureGrid() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addColumn(SurgeryUnitDto::getName).setHeader("Operationszentrum");
        grid.addColumn(SurgeryUnitDto::getAddress).setHeader("Adresse");
        grid.addColumn(SurgeryUnitDto::getPhone).setHeader("Telefonnummer");
        grid.addColumn(SurgeryUnitDto::getEmail).setHeader("E-Mail");
        grid.addColumn(SurgeryUnitDto::getContact).setHeader("Kontakt");
        grid.addColumn(SurgeryUnitDto::getPhoneContact).setHeader("Telefonnummer der Kontaktperson");
        grid.setSizeFull();

        // refresh("");

        grid.asSingleSelect().addValueChangeListener(event -> {
            SurgeryUnitDto surgeryUnitDto = event.getValue();
            if (surgeryUnitDto != null) {
                navigateToDetailView(surgeryUnitDto);
            }
        });

    }

    private void configureSearch() {
        // searchButton.addClickListener(e -> refresh(searchField.getValue()));
        // searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

}