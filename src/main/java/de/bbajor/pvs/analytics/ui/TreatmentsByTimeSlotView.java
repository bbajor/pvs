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

import de.bbajor.pvs.analytics.dto.TreatmentStatistics;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View für die Auswertung "Behandlungen je Zeitslot".
 */
@Route("analytics/treatments-by-timeslot")
@PageTitle("Behandlungen je Zeitslot - Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class TreatmentsByTimeSlotView extends BaseAnalyticsView implements BeforeEnterObserver {

    private final AnalyticsService analyticsService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addClassNames(Padding.MEDIUM, "view-content");
        
        // Überschrift
        H1 title = new H1("Behandlungen je Zeitslot");
        title.addClassNames(Margin.Bottom.LARGE);
        add(title);
        
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE, Width.FULL);

        try {
            TreatmentStatistics stats = analyticsService.getAllAnalyticsData().treatmentStatistics();
            
            EChartsComponent chart = new EChartsComponent();
            chart.setHeight("500px");
            chart.setWidthFull();

            // Toggle-Buttons für Linie/Balken
            Button lineButton = new Button("Linie", VaadinIcon.LINE_CHART.create());
            Button barButton = new Button("Balken", VaadinIcon.BAR_CHART.create());
            lineButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            lineButton.addClickListener(e -> {
                lineButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                barButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                updateLineChart(chart, stats.byTimeSlot(), "Behandlungen je Zeitslot");
            });

            barButton.addClickListener(e -> {
                barButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                lineButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                updateBarChart(chart, stats.byTimeSlot(), "Behandlungen je Zeitslot");
            });

            FlexLayout buttonLayout = new FlexLayout(lineButton, barButton);
            buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.MEDIUM);

            VerticalLayout card = createChartCard("Behandlungen je Zeitslot", chart);
            card.addComponentAtIndex(1, buttonLayout);
            
            // Initial: Linie anzeigen
            updateLineChart(chart, stats.byTimeSlot(), "Behandlungen je Zeitslot");

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

