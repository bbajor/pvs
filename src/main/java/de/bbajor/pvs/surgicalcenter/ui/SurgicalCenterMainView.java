package de.bbajor.pvs.surgicalcenter.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
@PageTitle("OP-Planer")
@Menu(order = 4, icon = "vaadin:building", title = "OP-Planer")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
public class SurgicalCenterMainView extends Main implements BeforeEnterObserver {

    private final SurgicalCenterListPresenter presenter;
    private final Grid<SurgicalCenter> grid = new Grid<>(SurgicalCenter.class, false);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button(VaadinIcon.SEARCH.create());
    private final Button createButton = new Button(VaadinIcon.FILE_ADD.create());

    public SurgicalCenterMainView(SurgicalCenterListPresenter presenter) {
        this.presenter = presenter;

        createButton.setText("Neue Einrichtung");
        createButton.addClickListener(event -> {
            SurgicalCenter dto = new SurgicalCenter();
            dto.setId(Integer.valueOf(-1));
            navigateToDetailView(dto);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        searchField.setPlaceholder("Suche nach Name, Adresse oder Kontakt");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.addKeyUpListener(event -> {
            var searchTerm = searchField.getValue();
            if (searchTerm != null) {
                filterGrid(searchTerm);
            }
        });
        
        searchButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        configureGrid();
        configureSearch();

        // Moderne Button-Anordnung
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.add(createButton);
        buttonLayout.setFlexGrow(1, new Div()); // Spacer
        
        HorizontalLayout searchLayout = new HorizontalLayout();
        searchLayout.setSpacing(true);
        searchLayout.setWidthFull();
        searchLayout.add(searchField, searchButton);
        searchLayout.setFlexGrow(1, searchField);
        
        VerticalLayout toolbarContent = new VerticalLayout();
        toolbarContent.setSpacing(true);
        toolbarContent.setPadding(false);
        toolbarContent.add(buttonLayout, searchLayout);

        add(new ViewToolbar("Operative Einrichtungen", toolbarContent));
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
        
        // Verbesserte Grid-Spalten mit ComponentRenderer
        grid.addColumn(new ComponentRenderer<>(center -> {
            Span name = new Span(center.toString() != null ? center.toString() : "-");
            name.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            return name;
        })).setHeader("Operative Einrichtung").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String address = center.getAddress() != null ? center.getAddress().toString() : "-";
            Span addressSpan = new Span(address);
            addressSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            return addressSpan;
        })).setHeader("Adresse").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String phone = center.getPhone() != null ? center.getPhone() : "-";
            Span phoneSpan = new Span(phone);
            phoneSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            return phoneSpan;
        })).setHeader("Telefonnummer").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String email = center.getEmail() != null ? center.getEmail() : "-";
            Span emailSpan = new Span(email);
            emailSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            return emailSpan;
        })).setHeader("E-Mail").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String contact = center.getContact() != null ? center.getContact() : "-";
            Span contactSpan = new Span(contact);
            return contactSpan;
        })).setHeader("Kontaktperson").setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String phoneContact = center.getPhoneContact() != null ? center.getPhoneContact() : "-";
            Span phoneContactSpan = new Span(phoneContact);
            phoneContactSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            return phoneContactSpan;
        })).setHeader("Telefon Kontaktperson").setAutoWidth(true);
        
        grid.setSizeFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
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