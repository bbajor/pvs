package de.bbajor.pvs.analytics.ui;

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

import de.bbajor.pvs.analytics.dto.InsuranceStatistics;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View für die Auswertung "Patienten nach Krankenversicherung".
 */
@Route("analytics/patients-by-insurance-provider")
@PageTitle("Patienten nach Krankenversicherung - Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class PatientsByInsuranceProviderView extends BaseAnalyticsView implements BeforeEnterObserver {

    private final AnalyticsService analyticsService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addClassName(Padding.MEDIUM);
        add(new ViewToolbar("Patienten nach Krankenversicherung"));
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE, Width.FULL);

        try {
            InsuranceStatistics stats = analyticsService.getAllAnalyticsData().insuranceStatistics();
            
            EChartsComponent chart = new EChartsComponent();
            chart.setHeight("500px");
            chart.setWidthFull();

            VerticalLayout card = createChartCard("Patienten nach Krankenversicherung", chart);
            updateBarChart(chart, stats.byProvider(), "Anzahl Patienten nach Krankenversicherung");

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

