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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;
    private final Grid<SurgicalCenter> grid = new Grid<>(SurgicalCenter.class, false);
    private final TextField searchField = new TextField();
    private final Button createButton = new Button(VaadinIcon.FILE_ADD.create());

    public SurgicalCenterMainView(SurgicalCenterListPresenter presenter, ApplicationContext applicationContext) {
        this.presenter = presenter;
        this.applicationContext = applicationContext;

        createButton.setText("Neue Einrichtung");
        createButton.addClickListener(event -> {
            SurgicalCenter dto = new SurgicalCenter();
            dto.setId(-1);
            navigateToDetailView(dto);
        });
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createButton.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        searchField.setPlaceholder("Mindestens 3 Zeichen für Suche eingeben");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        
        // Verwende ValueChangeMode.EAGER für sofortige Reaktion beim Tippen
        // Dies löst ValueChangeEvents bei jedem Tastendruck aus (nicht nur bei Blur)
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        searchField.setValueChangeTimeout(300); // 300ms Debouncing
        
        // ValueChangeListener reagiert jetzt sofort beim Tippen (dank EAGER mode)
        searchField.addValueChangeListener(event -> {
            String searchTerm = event.getValue();
            
            if (searchTerm == null || searchTerm.trim().length() < 3) {
                // Wenn weniger als 3 Zeichen, zeige alle Ergebnisse (keine Suche)
                scheduleSearch("");
                return;
            }
            
            // Mindestens 3 Zeichen: Suche auslösen
            scheduleSearch(searchTerm.trim());
        });
        
        // Zusätzlich KeyUpListener für sofortige Reaktion (falls ValueChangeMode nicht ausreicht)
        searchField.addKeyUpListener(event -> {
            // Hole aktuellen Wert aus dem Feld
            String searchTerm = searchField.getValue();
            
            if (searchTerm == null || searchTerm.trim().length() < 3) {
                // Wenn weniger als 3 Zeichen, zeige alle Ergebnisse (keine Suche)
                scheduleSearch("");
                return;
            }
            
            // Mindestens 3 Zeichen: Suche auslösen mit Debouncing
            scheduleSearch(searchTerm.trim());
        });

        // Überschrift
        H1 title = new H1("Operative Einrichtungen");
        add(title);

        // Container als Flexbox konfigurieren
        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("min-height", "0");

        // Section für Buttons und Suche
        Div toolbarSection = createToolbarSection();
        toolbarSection.getStyle().set("flex-shrink", "0");
        add(toolbarSection);

        // Grid - nimmt restlichen Platz ein und scrollt
        configureGrid();
        grid.setSizeFull();
        grid.getStyle().set("flex-grow", "1");
        grid.getStyle().set("min-height", "0");
        add(grid);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
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
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks and search don't trigger BeforeEnterEvent,
     * so the context might not be set.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccountRepository userAccountRepository = applicationContext.getBean(UserAccountRepository.class);
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                }
            } catch (Exception e) {
                // Log error but continue
            }
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
    
    /**
     * Plant eine Suche mit Debouncing ein, um zyklische Abhängigkeiten zu vermeiden.
     * Bricht vorherige Suchen ab, wenn eine neue gestartet wird.
     */
    private void scheduleSearch(String searchString) {
        // Plane neue Suche mit 300ms Verzögerung (Debouncing) über JavaScript
        // JavaScript kümmert sich um das Abbrechen vorheriger Timeouts
        com.vaadin.flow.component.UI currentUI = com.vaadin.flow.component.UI.getCurrent();
        if (currentUI != null) {
            final String finalSearchString = searchString != null ? searchString : "";
            currentUI.getPage().executeJs(
                "if (window.surgicalCenterSearchTimeout) { clearTimeout(window.surgicalCenterSearchTimeout); } " +
                "window.surgicalCenterSearchTimeout = setTimeout(function() { $0.$server.executeSurgicalCenterSearch($1); }, 300);",
                getElement(), finalSearchString
            );
        }
    }
    
    /**
     * Wird von JavaScript aufgerufen, um die Suche tatsächlich auszuführen.
     * Dies verhindert zyklische Abhängigkeiten, da die Suche asynchron erfolgt.
     */
    @com.vaadin.flow.component.ClientCallable
    private void executeSurgicalCenterSearch(String searchString) {
        filterGrid(searchString);
    }
    
    private void filterGrid(String searchTerm) {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
        // Setze den Suchbegriff im Feld (falls nicht bereits gesetzt)
        // Der DataProvider liest den Wert direkt aus searchField.getValue()
        if (searchTerm != null && !searchTerm.equals(searchField.getValue())) {
            searchField.setValue(searchTerm);
        }
        // DataProvider wird automatisch neu geladen, da er searchField.getValue() verwendet
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
        grid.addColumn(new ComponentRenderer<>(center -> {
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
            // Stelle sicher, dass InstitutionContext gesetzt ist
            ensureInstitutionContext();
            
            // Counter beim Start jeder Query zurücksetzen
            rowCounter.set(0);
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return Stream.empty();
            }
            
            // Konvertiere zu Spring PageRequest
            org.springframework.data.domain.Pageable pageable = toSpringPageRequest(query);
            
            // Hole Suchbegriff
            String searchTerm = searchField.getValue();
            
            // Lade nur die benötigte Seite (nicht alle Daten)
            org.springframework.data.domain.Slice<SurgicalCenter> slice;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                slice = presenter.getAll(pageable);
            } else {
                slice = presenter.findAllBy(searchTerm, pageable);
            }
            
            // Sortiere alphanumerisch nach Name der operativen Einrichtung
            Stream<SurgicalCenter> stream = slice.getContent().stream()
                .sorted(Comparator.comparing(center -> {
                    String name = center.getName();
                    return name != null ? name.toLowerCase() : "";
                }));
            
            return stream;
        });
        
        // Count-Callback: Da der Presenter ein Slice (nicht Page) zurückgibt, können wir keine exakte Anzahl bestimmen.
        // Wir verwenden eine Schätzung: Wenn die aktuelle Seite voll ist (20 Items), schätzen wir, dass es mehr gibt.
        // Dies verhindert den Fehler "Trying to use exact size with a lazy loading component".
        grid.getLazyDataView().setItemCountCallback(query -> {
            // Stelle sicher, dass InstitutionContext gesetzt ist
            ensureInstitutionContext();
            
            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                return 0;
            }
            
            // Verwende die PageSize des Grids statt query.getLimit(), da getLimit() negativ sein kann
            int pageSize = Math.max(1, grid.getPageSize());
            int offset = Math.max(0, query.getOffset());
            
            String searchTerm = searchField.getValue();
            
            // Lade eine Seite, um zu sehen, ob es mehr Items gibt
            // Verwende PageSize statt query.getLimit() um negative Werte zu vermeiden
            org.springframework.data.domain.Pageable testPageable = org.springframework.data.domain.PageRequest.of(0, pageSize);
            org.springframework.data.domain.Slice<SurgicalCenter> slice;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                slice = presenter.getAll(testPageable);
            } else {
                slice = presenter.findAllBy(searchTerm, testPageable);
            }
            
            // Wenn die Seite voll ist und es eine nächste Seite gibt, schätze eine große Anzahl
            // Ansonsten verwende die tatsächliche Anzahl der Items
            int currentPageSize = slice.getContent().size();
            if (slice.hasNext() && currentPageSize == pageSize) {
                // Schätze: mindestens (aktuelle Seite + 1) * PageSize
                // Verwende eine große Zahl, um sicherzustellen, dass Pagination funktioniert
                int currentPage = offset / pageSize;
                return (currentPage + 2) * pageSize;
            } else {
                // Letzte Seite oder weniger Items: exakte Anzahl
                return offset + currentPageSize;
            }
        });
        
        // Zeilenumbruch in Zellen aktivieren
        grid.getStyle().set("--vaadin-grid-cell-content-overflow", "visible");
        
        // Nur Width auf 100% setzen, Höhe wird über Flexbox gesteuert
        grid.setWidthFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);

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