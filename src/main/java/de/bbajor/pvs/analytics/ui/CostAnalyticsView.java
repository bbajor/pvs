package de.bbajor.pvs.analytics.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.analytics.dto.TimeSeriesData;
import de.bbajor.pvs.cost.service.TreatmentCostService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.FeatureFlagService;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

/**
 * Analytics-View für Kostenverläufe und Kostenübersicht.
 * Zeigt monatliche Kostenverläufe als Diagramm.
 */
@Route("analytics/costs")
@PageTitle("Kostenanalysen")
@Menu(order = 8, icon = "vaadin:euro", title = "Kostenanalysen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
public class CostAnalyticsView extends BaseAnalyticsView implements BeforeEnterObserver {

    @Autowired
    private TreatmentCostService treatmentCostService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Prüfe Feature Flag
        if (featureFlagService == null || !featureFlagService.isFeatureEnabled("COST_MODULE")) {
            event.forwardTo("analytics");
            return;
        }

        // Stelle sicher, dass InstitutionContext gesetzt ist
        if (!InstitutionContext.hasInstitution()) {
            event.forwardTo("analytics");
            return;
        }

        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        setSizeFull();

        // Überschrift
        H1 title = new H1("Kostenanalysen");
        add(title);

        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);

        try {
            // Hole monatliche Kosten
            LocalDate startDate = LocalDate.now().minusMonths(12); // Letzte 12 Monate
            Map<String, BigDecimal> monthlyCosts = treatmentCostService.getMonthlyCosts(startDate);

            if (monthlyCosts.isEmpty()) {
                layout.add(new Span("Keine Kostendaten verfügbar."));
                return layout;
            }

            // Überschrift für Diagramm
            H2 chartTitle = new H2("Monatliche Kostenverläufe");
            layout.add(chartTitle);

            // Erstelle ECharts-Komponente
            EChartsComponent chart = new EChartsComponent();
            chart.setWidthFull();
            chart.setHeight("400px");
            
            // Konvertiere Map zu TimeSeriesData-Liste
            List<TimeSeriesData> costData = monthlyCosts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new TimeSeriesData(formatMonthForDisplay(e.getKey()), e.getValue().longValue()))
                .collect(Collectors.toList());
            
            // Erstelle Chart-Optionen mit BaseAnalyticsView-Methode
            updateLineChart(chart, costData, "Monatliche Kostenverläufe");
            
            layout.add(chart);

            // Gesamtkosten anzeigen
            BigDecimal totalCosts = monthlyCosts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Span totalLabel = new Span("Gesamtkosten (letzte 12 Monate): " + 
                String.format("%.2f €", totalCosts));
            totalLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("margin-top", "var(--lumo-space-m)");
            layout.add(totalLabel);

        } catch (Exception e) {
            log.error("Fehler beim Laden der Kostenanalysen", e);
            layout.add(new Span("Fehler beim Laden der Kostenanalysen: " + e.getMessage()));
        }

        return layout;
    }

    /**
     * Formatiert Monat von "YYYY-MM" zu "MM.YYYY" für Anzeige.
     */
    private String formatMonthForDisplay(String month) {
        if (month == null || month.length() < 7) {
            return month;
        }
        // "2024-01" -> "01.2024"
        return month.substring(5, 7) + "." + month.substring(0, 4);
    }
}

