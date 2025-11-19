package de.bbajor.pvs.analytics.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import de.bbajor.pvs.analytics.dto.AgeGroupStatistics;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View für die Auswertung "Patienten nach Altersklassen" (nur Balkendiagramm).
 */
@Route("analytics/patients-by-age")
@PageTitle("Patienten nach Altersklassen - Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class PatientsByAgeGroupView extends BaseAnalyticsView implements BeforeEnterObserver {

    private final AnalyticsService analyticsService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addClassNames(Padding.MEDIUM, "view-content");
        
        // Überschrift
        H1 title = new H1("Patienten nach Altersklassen");
        title.addClassNames(Margin.Bottom.LARGE);
        add(title);
        
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE, Width.FULL);

        try {
            AgeGroupStatistics stats = analyticsService.getAllAnalyticsData().ageGroupStatistics();
            
            EChartsComponent chart = new EChartsComponent();
            chart.setHeight("500px");
            chart.setWidthFull();

            VerticalLayout card = createChartCard("Patienten nach Altersklassen", chart);
            
            // Nur Balkendiagramm anzeigen (kein Kreisdiagramm)
            updateBarChart(chart, stats.ageGroups(), "Anzahl Patienten nach Altersklassen");

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

