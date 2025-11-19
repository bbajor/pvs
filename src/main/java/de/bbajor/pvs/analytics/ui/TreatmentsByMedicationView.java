package de.bbajor.pvs.analytics.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import de.bbajor.pvs.analytics.dto.MedicationStatistics;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View für die Auswertung "Behandlungen je Medikament".
 */
@Route("analytics/treatments-by-medication")
@PageTitle("Behandlungen je Medikament - Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class TreatmentsByMedicationView extends BaseAnalyticsView implements BeforeEnterObserver {

    private final AnalyticsService analyticsService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addClassNames(Padding.MEDIUM, "view-content");
        
        // Überschrift
        H1 title = new H1("Behandlungen je Medikament");
        title.addClassNames(Margin.Bottom.LARGE);
        add(title);
        
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE, Width.FULL);

        try {
            MedicationStatistics stats = analyticsService.getAllAnalyticsData().medicationStatistics();
            
            EChartsComponent chart = new EChartsComponent();
            chart.setHeight("500px");
            chart.setWidthFull();

            // Toggle-Buttons für Monat/Jahr
            Button monthButton = new Button("Monat", VaadinIcon.CALENDAR.create());
            Button yearButton = new Button("Jahr", VaadinIcon.CALENDAR.create());
            monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            monthButton.addClickListener(e -> {
                monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                yearButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                updateBarChart(chart, stats.monthlyData(), "Behandlungen je Medikament pro Monat");
            });

            yearButton.addClickListener(e -> {
                yearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                monthButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                updateBarChart(chart, stats.yearlyData(), "Behandlungen je Medikament pro Jahr");
            });

            FlexLayout buttonLayout = new FlexLayout(monthButton, yearButton);
            buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.MEDIUM);

            VerticalLayout card = createChartCard("Behandlungen je Medikament", chart);
            card.addComponentAtIndex(1, buttonLayout);
            
            // Initial: Monat anzeigen
            updateBarChart(chart, stats.monthlyData(), "Behandlungen je Medikament pro Monat");

            content.add(card);
        } catch (Exception e) {
            log.error("Fehler beim Laden der Analytics-Daten", e);
            H2 error = new H2("Fehler beim Laden der Daten. Bitte versuchen Sie es später erneut.");
            error.addClassNames(Margin.NONE);
            content.add(error);
        }

        return content;
    }
}

