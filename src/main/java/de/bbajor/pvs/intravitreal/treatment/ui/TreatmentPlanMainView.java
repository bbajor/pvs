package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanListPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import jakarta.annotation.security.PermitAll;

@Route("ivom")
@PageTitle("IVOM-Verwaltung")
@Menu(order = 2, icon = "vaadin:calendar-user", title = "IVOM-Verwaltung")
@PermitAll
public class TreatmentPlanMainView extends Main implements TreatmentPlanChangeListener {

    private final TreatmentPlanListPresenter ivomListPresenter;
    private final CurrentUser currentUser;
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button(VaadinIcon.SEARCH.create());
    private final Button createButton = new Button(VaadinIcon.PLUS.create());
    private final Button generateDailyListButton;
    private final Grid<TreatmentPlan> ivomPlanGrid = new Grid<>(TreatmentPlan.class, false);

    public TreatmentPlanMainView(TreatmentPlanListPresenter ivomListPresenter, CurrentUser currentUser) {
        this.ivomListPresenter = ivomListPresenter;
        this.currentUser = currentUser;

        createButton.addClickListener(event -> {
            Optional<TreatmentPlan> ivom = ivomPlanGrid.getSelectionModel().getFirstSelectedItem();
            if (ivom.isPresent()) {
                navigateToDetailView(ivom.get());
            } else {
                TreatmentPlan newTreatmentPlan = new TreatmentPlan();
                newTreatmentPlan.setId(-1L);
                navigateToDetailView(newTreatmentPlan);
            }
        });
        createButton.getElement().setAttribute("theme", "primary");
        
        // Button nur für berechtigte Rollen aktivieren
        boolean canBook = currentUser.getPrincipal()
                .map(principal -> {
                    return principal.getAuthorities().stream()
                            .anyMatch(auth -> {
                                String authority = auth.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.ADMIN) ||
                                        authority.equals("ROLE_" + AppRoles.DOCTOR) ||
                                        authority.equals("ROLE_" + AppRoles.TECH_USER);
                            });
                })
                .orElse(false);
        createButton.setEnabled(canBook);
        if (!canBook) {
            createButton.setTooltipText("Sie benötigen die Rolle ADMIN, DOCTOR oder TECH_USER, um Termine zu buchen");
        }

        searchField.setPlaceholder("Suche nach Name, Vorname, Geburtsdatum oder Krankenkasse");
        searchField.setWidthFull();

        generateDailyListButton = new Button("Wochenliste anzeigen",
                event -> {
                    LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate endOfWeek = monday.plusDays(6);
                    WeekListConfig config = new WeekListConfig(ivomListPresenter.generateWeekList(monday), monday,
                            endOfWeek);
                    WeekListDialog dailyTreatmentsDialog = new WeekListDialog(config);
                    dailyTreatmentsDialog.open();
                });
        generateDailyListButton
                .setTooltipText("Erzeugt eine Tagesliste für die tagesaktuellen OP-Slots. " +
                        "Dabei werden die zu behandelnden Patienten, sowie die jeweiligen " +
                        "Einrichtungen aufgelistet, an denen die Behandlung stattfindet.");
        generateDailyListButton.getElement().setAttribute("theme", "primary");

        add(new ViewToolbar("IVOM-Behandlungspläne",
                ViewToolbar.group(createButton, searchField, searchButton, generateDailyListButton)));
        add(ivomPlanGrid);

        configureGrid();
        configureSearch();

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private void navigateToDetailView(TreatmentPlan treatmentPlan) {
        // TODO Achtung, hier sollte nicht mit der ID-Spalte aus der Datenbank
        // gearbeitet werden, sondern mit einer internen UUID, die nicht zu erraten
        // ist!!!!
        UI.getCurrent().navigate("ivom/" + treatmentPlan.getId());
    }

    private void configureGrid() {
        ivomPlanGrid.addColumn(TreatmentPlan::getLastName).setHeader("Nachname");
        ivomPlanGrid.addColumn(TreatmentPlan::getFirstName).setHeader("Vorname");
        ivomPlanGrid
                .addColumn(
                        treatmentPlan -> DateAndTimeUtils.getGermanDateTimeFormatter().format(treatmentPlan.getBirth()))
                .setHeader("Geburtsdatum");
        ivomPlanGrid.addColumn(TreatmentPlan::getHealthInsurance).setHeader("Krankenkasse");
        ivomPlanGrid.addColumn(TreatmentPlan::getDiagnosis).setHeader("Grund der Behandlung");
        ivomPlanGrid.addColumn(TreatmentPlan::getAdditionalInformation).setHeader("Zusätzliche Informationen");
        ivomPlanGrid.setSizeFull();
        ivomPlanGrid.asSingleSelect().addValueChangeListener(event -> {
            TreatmentPlan ivomDto = event.getValue();
            if (ivomDto != null) {
                navigateToDetailView(ivomDto);
            }
        });
        ivomPlanGrid.setItems(ivomListPresenter.findAll());
    }

    private void configureSearch() {
        searchButton.addClickListener(e -> refresh(searchField.getValue()));
        searchField.addValueChangeListener(e -> refresh(e.getValue()));
    }

    public void refresh(String searchString) {
        List<TreatmentPlan> ivomList = ivomListPresenter.findAllBy(searchString);
        ivomPlanGrid.setItems(ivomList);
    }

    @Override
    public void onTreatmentPlanChanged() {
        refresh("");
    }
}