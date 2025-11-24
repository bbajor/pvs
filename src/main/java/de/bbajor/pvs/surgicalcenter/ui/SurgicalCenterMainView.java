package de.bbajor.pvs.surgicalcenter.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;
import de.bbajor.pvs.institution.context.InstitutionContext;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Route("surgicalcenter")
@PageTitle("OP-Planer")
@Menu(order = 4, icon = "vaadin:building", title = "OP-Planer")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
public class SurgicalCenterMainView extends Main implements BeforeEnterObserver {

    private final SurgicalCenterListPresenter presenter;
    private final Grid<SurgicalCenter> grid = new Grid<>(SurgicalCenter.class, false);
    private final TextField searchField = new TextField();
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

        // Überschrift
        H1 title = new H1("Operative Einrichtungen");
        add(title);

        // Section für Buttons und Suche
        Div toolbarSection = createToolbarSection();
        toolbarSection.getStyle().set("flex-shrink", "0");
        add(toolbarSection);

        // Grid - nimmt restlichen Platz ein und scrollt
        configureGrid();
        grid.setSizeFull();
        add(grid);

        setSizeFull();
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

    private Div createToolbarSection() {
        Div section = new Div();
        section.addClassName("dialog-section");
        section.setWidthFull();
        section.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("box-sizing", "border-box")
            .set("margin-bottom", "var(--lumo-space-m)");
        
        H4 sectionTitle = new H4("Aktionen");
        sectionTitle.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-s)")
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-size", "var(--lumo-font-size-m)")
            .set("font-weight", "600");
        section.add(sectionTitle);
        
        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setSpacing(true);
        toolbarLayout.setWidthFull();
        toolbarLayout.setAlignItems(HorizontalLayout.Alignment.CENTER);
        
        toolbarLayout.add(createButton);
        
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        toolbarLayout.add(searchField);
        toolbarLayout.setFlexGrow(1, searchField);
        
        section.add(toolbarLayout);
        return section;
    }
    
    private void filterGrid(String searchTerm) {
        // TODO filtern über eine FilterRow im Grid
        grid.getDataProvider().refreshAll();
    }

    private void navigateToDetailView(SurgicalCenter surgicalCenter) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("surgicalcenter/" + surgicalCenter.getId());
    }

    private void configureGrid() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        
        // Nr. Spalte als erste Spalte
        java.util.concurrent.atomic.AtomicInteger rowCounter = new java.util.concurrent.atomic.AtomicInteger(0);
        grid.addColumn(new ComponentRenderer<>(center -> {
            // Zeilennummer pro Seite (startet bei 1 auf jeder Seite)
            int rowNumber = rowCounter.incrementAndGet();
            Span span = new Span(String.valueOf(rowNumber));
            span.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            return span;
        })).setHeader("Nr.").setResizable(false).setAutoWidth(true).setFlexGrow(0);
        
        // Verbesserte Grid-Spalten mit ComponentRenderer
        Grid.Column<SurgicalCenter> nameColumn = grid.addColumn(new ComponentRenderer<>(center -> {
            String nameStr = center.getName() != null ? center.getName() : "-";
            Span name = new Span(nameStr);
            name.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            name.getStyle().set("white-space", "normal");
            name.getStyle().set("word-wrap", "break-word");
            return name;
        })).setHeader("Operative Einrichtung").setResizable(true).setAutoWidth(true).setSortable(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String address = center.getAddress() != null ? center.getAddress().toString() : "-";
            Span addressSpan = new Span(address);
            addressSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            addressSpan.getStyle().set("white-space", "normal");
            addressSpan.getStyle().set("word-wrap", "break-word");
            return addressSpan;
        })).setHeader("Adresse").setResizable(true).setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String phone = center.getPhone() != null ? center.getPhone() : "-";
            Span phoneSpan = new Span(phone);
            phoneSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            phoneSpan.getStyle().set("white-space", "normal");
            phoneSpan.getStyle().set("word-wrap", "break-word");
            return phoneSpan;
        })).setHeader("Telefonnummer").setResizable(true).setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String email = center.getEmail() != null ? center.getEmail() : "-";
            Span emailSpan = new Span(email);
            emailSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            emailSpan.getStyle().set("white-space", "normal");
            emailSpan.getStyle().set("word-wrap", "break-word");
            return emailSpan;
        })).setHeader("E-Mail").setResizable(true).setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String contact = center.getContact() != null ? center.getContact() : "-";
            Span contactSpan = new Span(contact);
            contactSpan.getStyle().set("white-space", "normal");
            contactSpan.getStyle().set("word-wrap", "break-word");
            return contactSpan;
        })).setHeader("Kontaktperson").setResizable(true).setAutoWidth(true);
        
        grid.addColumn(new ComponentRenderer<>(center -> {
            String phoneContact = center.getPhoneContact() != null ? center.getPhoneContact() : "-";
            Span phoneContactSpan = new Span(phoneContact);
            phoneContactSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
            phoneContactSpan.getStyle().set("white-space", "normal");
            phoneContactSpan.getStyle().set("word-wrap", "break-word");
            return phoneContactSpan;
        })).setHeader("Telefon Kontaktperson").setResizable(true).setAutoWidth(true);
        
        // Letzte Spalte: Zeitslots aktueller Monat
        grid.addColumn(new ComponentRenderer<>(center -> {
            int slotCount = countTimeSlotsForCurrentMonth(center);
            Span countSpan = new Span(String.valueOf(slotCount));
            countSpan.getStyle().set("white-space", "normal");
            countSpan.getStyle().set("word-wrap", "break-word");
            return countSpan;
        })).setHeader("Zeitslots aktueller Monat").setResizable(true).setAutoWidth(true);
        
        // Paging aktivieren mit Sortierung nach Name
        grid.setPageSize(20);
        grid.setItems(query -> {
            // Counter beim Start jeder Query zurücksetzen
            rowCounter.set(0);
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return Stream.empty();
            }
            Stream<SurgicalCenter> stream = presenter.getAll(toSpringPageRequest(query)).stream();
            
            // Sortiere alphanumerisch nach Name der operativen Einrichtung
            stream = stream.sorted(Comparator.comparing(center -> {
                String name = center.getName();
                return name != null ? name.toLowerCase() : "";
            }));
            
            return stream;
        });
        
        // Zeilenumbruch in Zellen aktivieren
        grid.getStyle().set("--vaadin-grid-cell-content-overflow", "visible");
        
        // Nur Width auf 100% setzen, Höhe wird über Flexbox gesteuert
        grid.setWidthFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        grid.asSingleSelect().addValueChangeListener(event -> {
            SurgicalCenter surgicalCenterDto = event.getValue();
            if (surgicalCenterDto != null) {
                navigateToDetailView(surgicalCenterDto);
            }
        });
    }
    
    /**
     * Zählt die Anzahl der Zeitslots für den aktuellen Monat (inklusive vergangene).
     */
    private int countTimeSlotsForCurrentMonth(SurgicalCenter center) {
        if (center == null || center.getAvailableTimeSlots() == null) {
            return 0;
        }
        
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
        
        List<SurgicalCenterTimeSlot> timeSlots = center.getAvailableTimeSlots();
        return (int) timeSlots.stream()
                .filter(slot -> slot != null && slot.getDate() != null)
                .filter(slot -> !slot.getDate().isBefore(firstDayOfMonth) && !slot.getDate().isAfter(lastDayOfMonth))
                .count();
    }

}