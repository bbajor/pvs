package de.bbajor.pvs.analytics.ui;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexWrap;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import de.bbajor.pvs.analytics.dto.AnalyticsData;
import de.bbajor.pvs.analytics.dto.TimeSeriesData;
import de.bbajor.pvs.analytics.service.AnalyticsService;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.AppRoles;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * View für Analytics und Auswertungen mit verschiedenen Charts.
 */
@Route("analytics")
@PageTitle("Auswertungen")
@Menu(order = 7, icon = "vaadin:chart", title = "Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class AnalyticsView extends Main implements BeforeEnterObserver {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addClassName(Padding.MEDIUM);
        add(new ViewToolbar("Auswertungen"));
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE);

        try {
            AnalyticsData data = analyticsService.getAllAnalyticsData();

            // Grid-Layout für Charts (2 Spalten, responsive)
            FlexLayout chartsGrid = new FlexLayout();
            chartsGrid.addClassNames(
                Display.FLEX,
                FlexWrap.WRAP,
                Gap.LARGE,
                Width.FULL
            );
            chartsGrid.setFlexDirection(FlexLayout.FlexDirection.ROW);

            // Chart 1: Behandlungen pro Monat/Jahr
            chartsGrid.add(createTreatmentChartCard(data.treatmentStatistics()));

            // Chart 2: Behandlungen je Zeitslot
            chartsGrid.add(createTimeSlotChartCard(data.treatmentStatistics()));

            // Chart 3: Patienten nach Altersklassen
            chartsGrid.add(createAgeGroupChartCard(data.ageGroupStatistics()));

            // Chart 4: Patienten Kasse/privat
            chartsGrid.add(createInsuranceTypeChartCard(data.insuranceStatistics()));

            // Chart 5: Patienten nach Krankenversicherung
            chartsGrid.add(createInsuranceProviderChartCard(data.insuranceStatistics()));

            // Chart 6: Behandlungen je Medikament
            chartsGrid.add(createMedicationChartCard(data.medicationStatistics()));

            content.add(chartsGrid);
        } catch (Exception e) {
            log.error("Fehler beim Laden der Analytics-Daten", e);
            content.add(new H2("Fehler beim Laden der Daten. Bitte versuchen Sie es später erneut."));
        }

        return content;
    }

    private VerticalLayout createChartCard(String title, EChartsComponent chart) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
            Padding.LARGE,
            Border.ALL,
            BorderRadius.MEDIUM,
            "shadow-s"
        );
        card.getStyle().set("min-width", "400px");
        card.getStyle().set("flex", "1 1 45%");
        card.setSpacing(false);
        card.setPadding(true);

        H2 cardTitle = new H2(title);
        cardTitle.addClassNames(Margin.NONE, Margin.Bottom.MEDIUM);
        card.add(cardTitle);
        card.add(chart);

        return card;
    }

    private VerticalLayout createTreatmentChartCard(de.bbajor.pvs.analytics.dto.TreatmentStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");

        // Toggle-Buttons für Monat/Jahr
        Button monthButton = new Button("Monat", VaadinIcon.CALENDAR.create());
        Button yearButton = new Button("Jahr", VaadinIcon.CALENDAR.create());
        monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        monthButton.addClickListener(e -> {
            monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            yearButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            updateLineChart(chart, stats.monthlyData(), "Behandlungen pro Monat");
        });

        yearButton.addClickListener(e -> {
            yearButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            monthButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            updateLineChart(chart, stats.yearlyData(), "Behandlungen pro Jahr");
        });

        FlexLayout buttonLayout = new FlexLayout(monthButton, yearButton);
        buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.SMALL);

        VerticalLayout card = createChartCard("Patientenbehandlungen", chart);
        card.addComponentAtIndex(1, buttonLayout);
        
        // Initial: Monat anzeigen
        updateLineChart(chart, stats.monthlyData(), "Behandlungen pro Monat");

        return card;
    }

    private VerticalLayout createTimeSlotChartCard(de.bbajor.pvs.analytics.dto.TreatmentStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");

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
        buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.SMALL);

        VerticalLayout card = createChartCard("Behandlungen je Zeitslot", chart);
        card.addComponentAtIndex(1, buttonLayout);
        
        // Initial: Linie anzeigen
        updateLineChart(chart, stats.byTimeSlot(), "Behandlungen je Zeitslot");

        return card;
    }

    private VerticalLayout createAgeGroupChartCard(de.bbajor.pvs.analytics.dto.AgeGroupStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");

        // Toggle-Buttons für Balken/Kreis
        Button barButton = new Button("Balken", VaadinIcon.BAR_CHART.create());
        Button pieButton = new Button("Kreis", VaadinIcon.CIRCLE_THIN.create());
        barButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        barButton.addClickListener(e -> {
            barButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            pieButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            updateBarChart(chart, stats.ageGroups(), "Anzahl Patienten nach Altersklassen");
        });

        pieButton.addClickListener(e -> {
            pieButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            barButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            updatePieChart(chart, stats.ageGroups(), "Anzahl Patienten nach Altersklassen");
        });

        FlexLayout buttonLayout = new FlexLayout(barButton, pieButton);
        buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.SMALL);

        VerticalLayout card = createChartCard("Patienten nach Altersklassen", chart);
        card.addComponentAtIndex(1, buttonLayout);
        
        // Initial: Balken anzeigen
        updateBarChart(chart, stats.ageGroups(), "Anzahl Patienten nach Altersklassen");

        return card;
    }

    private VerticalLayout createInsuranceTypeChartCard(de.bbajor.pvs.analytics.dto.InsuranceStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");
        updatePieChart(chart, stats.byType(), "Anzahl Patienten nach Versicherungsart");
        return createChartCard("Patienten Kasse/Privat", chart);
    }

    private VerticalLayout createInsuranceProviderChartCard(de.bbajor.pvs.analytics.dto.InsuranceStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");
        updateBarChart(chart, stats.byProvider(), "Anzahl Patienten nach Krankenversicherung");
        return createChartCard("Patienten nach Krankenversicherung", chart);
    }

    private VerticalLayout createMedicationChartCard(de.bbajor.pvs.analytics.dto.MedicationStatistics stats) {
        EChartsComponent chart = new EChartsComponent();
        chart.setHeight("400px");

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
        buttonLayout.addClassNames(Gap.SMALL, Margin.Bottom.SMALL);

        VerticalLayout card = createChartCard("Behandlungen je Medikament", chart);
        card.addComponentAtIndex(1, buttonLayout);
        
        // Initial: Monat anzeigen
        updateBarChart(chart, stats.monthlyData(), "Behandlungen je Medikament pro Monat");

        return card;
    }

    private void updateLineChart(EChartsComponent chart, List<TimeSeriesData> data, String title) {
        JsonObject option = createLineChartOption(data, title);
        chart.setOption(option);
    }

    private void updateBarChart(EChartsComponent chart, List<TimeSeriesData> data, String title) {
        JsonObject option = createBarChartOption(data, title);
        chart.setOption(option);
    }

    private void updateBarChart(EChartsComponent chart, Map<String, Long> data, String title) {
        List<TimeSeriesData> timeSeriesData = data.entrySet().stream()
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .toList();
        updateBarChart(chart, timeSeriesData, title);
    }

    private void updatePieChart(EChartsComponent chart, Map<String, Long> data, String title) {
        JsonObject option = createPieChartOption(data, title);
        chart.setOption(option);
    }

    private JsonObject createLineChartOption(List<TimeSeriesData> data, String title) {
        JsonObject option = Json.createObject();
        option.put("title", createTitle(title));
        option.put("tooltip", createTooltip());
        option.put("xAxis", createXAxis(data));
        option.put("yAxis", createYAxis());
        option.put("series", createLineSeries(data));
        return option;
    }

    private JsonObject createBarChartOption(List<TimeSeriesData> data, String title) {
        JsonObject option = Json.createObject();
        option.put("title", createTitle(title));
        option.put("tooltip", createTooltip());
        option.put("xAxis", createXAxis(data));
        option.put("yAxis", createYAxis());
        option.put("series", createBarSeries(data));
        return option;
    }

    private JsonObject createPieChartOption(Map<String, Long> data, String title) {
        JsonObject option = Json.createObject();
        option.put("title", createTitle(title));
        option.put("tooltip", createTooltip());
        
        JsonArray seriesData = Json.createArray();
        int index = 0;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            JsonObject item = Json.createObject();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            seriesData.set(index++, item);
        }

        JsonObject series = Json.createObject();
        series.put("type", "pie");
        series.put("radius", "50%");
        series.put("data", seriesData);
        series.put("emphasis", createEmphasis());

        JsonArray seriesArray = Json.createArray();
        seriesArray.set(0, series);
        option.put("series", seriesArray);

        return option;
    }

    private JsonObject createTitle(String text) {
        JsonObject title = Json.createObject();
        title.put("text", text);
        title.put("left", "center");
        return title;
    }

    private JsonObject createTooltip() {
        JsonObject tooltip = Json.createObject();
        tooltip.put("trigger", "axis");
        return tooltip;
    }

    private JsonObject createXAxis(List<TimeSeriesData> data) {
        JsonObject xAxis = Json.createObject();
        xAxis.put("type", "category");
        
        JsonArray dataArray = Json.createArray();
        for (int i = 0; i < data.size(); i++) {
            dataArray.set(i, data.get(i).label());
        }
        xAxis.put("data", dataArray);
        
        return xAxis;
    }

    private JsonObject createYAxis() {
        JsonObject yAxis = Json.createObject();
        yAxis.put("type", "value");
        return yAxis;
    }

    private JsonArray createLineSeries(List<TimeSeriesData> data) {
        JsonObject series = Json.createObject();
        series.put("type", "line");
        series.put("smooth", true);
        
        JsonArray dataArray = Json.createArray();
        for (int i = 0; i < data.size(); i++) {
            dataArray.set(i, data.get(i).value());
        }
        series.put("data", dataArray);
        
        JsonArray seriesArray = Json.createArray();
        seriesArray.set(0, series);
        return seriesArray;
    }

    private JsonArray createBarSeries(List<TimeSeriesData> data) {
        JsonObject series = Json.createObject();
        series.put("type", "bar");
        
        JsonArray dataArray = Json.createArray();
        for (int i = 0; i < data.size(); i++) {
            dataArray.set(i, data.get(i).value());
        }
        series.put("data", dataArray);
        
        JsonArray seriesArray = Json.createArray();
        seriesArray.set(0, series);
        return seriesArray;
    }

    private JsonObject createEmphasis() {
        JsonObject emphasis = Json.createObject();
        JsonObject itemStyle = Json.createObject();
        itemStyle.put("shadowBlur", 10);
        itemStyle.put("shadowOffsetX", 0);
        itemStyle.put("shadowColor", "rgba(0, 0, 0, 0.5)");
        emphasis.put("itemStyle", itemStyle);
        return emphasis;
    }
}

