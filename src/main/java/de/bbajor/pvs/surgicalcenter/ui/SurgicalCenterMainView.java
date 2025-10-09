package de.bbajor.pvs.surgicalcenter.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
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
@Menu(order = 4, icon = "vaadin:building", title = "Operationszentren")
@PermitAll
public class SurgicalCenterMainView extends Main {

    private final SurgicalCenterListPresenter presenter;
    private final Grid<SurgicalCenterDto> grid = new Grid<>(SurgicalCenterDto.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button(VaadinIcon.SEARCH.create());
    private final Button createButton = new Button(VaadinIcon.FILE_ADD.create());

    public SurgicalCenterMainView(SurgicalCenterListPresenter presenter) {
        this.presenter = presenter;

        createButton.addClickListener(event -> {
            SurgicalCenterDto dto = new SurgicalCenterDto();
            dto.setId(Integer.valueOf(-1));
            navigateToDetailView(dto);
        });
        createButton.getElement().setAttribute("theme", "primary");

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();
        searchField.addKeyUpListener(event -> {
            var searchTerm = searchField.getValue();
            if (searchTerm != null) {
                filterGrid(searchTerm);
            }
        });

        configureGrid();
        configureSearch();

        add(new ViewToolbar("Operative Einrichtung", ViewToolbar.group(createButton, searchField, searchButton)));
        add(grid);

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private void filterGrid(String searchTerm) {
        //TODO filtern über eine FilterRow im Grid
    }

    private void navigateToDetailView(SurgicalCenterDto surgicalCenterDto) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("surgicalcenter/" + surgicalCenterDto.getId());
    }

    private void configureGrid() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addColumn(SurgicalCenterDto::toString).setHeader("Operative Einrichtung");
        grid.addColumn(SurgicalCenterDto::getSurgicalCenterAddress).setHeader("Adresse");
        grid.addColumn(SurgicalCenterDto::getPhone).setHeader("Telefonnummer");
        grid.addColumn(SurgicalCenterDto::getEmail).setHeader("E-Mail");
        grid.addColumn(SurgicalCenterDto::getContact).setHeader("Name Kontaktperson");
        grid.addColumn(SurgicalCenterDto::getPhoneContact).setHeader("Telefonnummer der Kontaktperson");
        grid.setSizeFull();
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