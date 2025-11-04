package de.bbajor.pvs.surgicalcenter.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.RolesAllowed;

@Route("surgicalcenter")
@PageTitle("Operationszentren")
@Menu(order = 4, icon = "vaadin:building", title = "Operationszentren")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
public class SurgicalCenterMainView extends Main implements BeforeEnterObserver {

    private final SurgicalCenterListPresenter presenter;
    private final Grid<SurgicalCenter> grid = new Grid<>(SurgicalCenter.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button(VaadinIcon.SEARCH.create());
    private final Button createButton = new Button(VaadinIcon.FILE_ADD.create());

    public SurgicalCenterMainView(SurgicalCenterListPresenter presenter) {
        this.presenter = presenter;

        createButton.addClickListener(event -> {
            SurgicalCenter dto = new SurgicalCenter();
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access surgical center data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
        }
    }

    private void filterGrid(String searchTerm) {
        // TODO filtern über eine FilterRow im Grid
    }

    private void navigateToDetailView(SurgicalCenter surgicalCenter) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("surgicalcenter/" + surgicalCenter.getId());
    }

    private void configureGrid() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addColumn(SurgicalCenter::toString).setHeader("Operative Einrichtung");
        grid.addColumn(SurgicalCenter::getAddress).setHeader("Adresse");
        grid.addColumn(SurgicalCenter::getPhone).setHeader("Telefonnummer");
        grid.addColumn(SurgicalCenter::getEmail).setHeader("E-Mail");
        grid.addColumn(SurgicalCenter::getContact).setHeader("Name Kontaktperson");
        grid.addColumn(SurgicalCenter::getPhoneContact).setHeader("Telefonnummer der Kontaktperson");
        grid.setSizeFull();
        grid.setItems(presenter.getAll());

        grid.asSingleSelect().addValueChangeListener(event -> {
            SurgicalCenter surgicalCenterDto = event.getValue();
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