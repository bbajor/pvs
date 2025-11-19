package de.bbajor.pvs.analytics.ui;

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import de.bbajor.pvs.analytics.dto.TimeSeriesData;
import de.bbajor.pvs.base.ui.component.ViewToolbar;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Basisklasse für Analytics-Views mit gemeinsamer Chart-Funktionalität.
 */
public abstract class BaseAnalyticsView extends Main {

    protected VerticalLayout createChartCard(String title, EChartsComponent chart) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
            Padding.LARGE,
            "shadow-s"
        );
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidthFull();

        H2 cardTitle = new H2(title);
        cardTitle.addClassNames(Margin.NONE, Margin.Bottom.MEDIUM);
        card.add(cardTitle);
        card.add(chart);

        return card;
    }

    protected void updateLineChart(EChartsComponent chart, List<TimeSeriesData> data, String title) {
        JsonObject option = createLineChartOption(data, title);
        chart.setOption(option);
    }

    protected void updateBarChart(EChartsComponent chart, List<TimeSeriesData> data, String title) {
        JsonObject option = createBarChartOption(data, title);
        chart.setOption(option);
    }

    protected void updateBarChart(EChartsComponent chart, Map<String, Long> data, String title) {
        List<TimeSeriesData> timeSeriesData = data.entrySet().stream()
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .toList();
        updateBarChart(chart, timeSeriesData, title);
    }

    protected void updatePieChart(EChartsComponent chart, Map<String, Long> data, String title) {
        JsonObject option = createPieChartOption(data, title);
        chart.setOption(option);
    }

    protected JsonObject createLineChartOption(List<TimeSeriesData> data, String title) {
        JsonObject option = Json.createObject();
        option.put("title", createTitle(title));
        option.put("tooltip", createTooltip());
        option.put("xAxis", createXAxis(data));
        option.put("yAxis", createYAxis());
        option.put("series", createLineSeries(data));
        return option;
    }

    protected JsonObject createBarChartOption(List<TimeSeriesData> data, String title) {
        JsonObject option = Json.createObject();
        option.put("title", createTitle(title));
        option.put("tooltip", createTooltip());
        option.put("xAxis", createXAxis(data));
        option.put("yAxis", createYAxis());
        option.put("series", createBarSeries(data));
        return option;
    }

    protected JsonObject createPieChartOption(Map<String, Long> data, String title) {
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

    protected JsonObject createTitle(String text) {
        JsonObject title = Json.createObject();
        title.put("text", text);
        title.put("left", "center");
        return title;
    }

    protected JsonObject createTooltip() {
        JsonObject tooltip = Json.createObject();
        tooltip.put("trigger", "axis");
        return tooltip;
    }

    protected JsonObject createXAxis(List<TimeSeriesData> data) {
        JsonObject xAxis = Json.createObject();
        xAxis.put("type", "category");
        
        JsonArray dataArray = Json.createArray();
        for (int i = 0; i < data.size(); i++) {
            dataArray.set(i, data.get(i).label());
        }
        xAxis.put("data", dataArray);
        
        return xAxis;
    }

    protected JsonObject createYAxis() {
        JsonObject yAxis = Json.createObject();
        yAxis.put("type", "value");
        return yAxis;
    }

    protected JsonArray createLineSeries(List<TimeSeriesData> data) {
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

    protected JsonArray createBarSeries(List<TimeSeriesData> data) {
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

    protected JsonObject createEmphasis() {
        JsonObject emphasis = Json.createObject();
        JsonObject itemStyle = Json.createObject();
        itemStyle.put("shadowBlur", 10);
        itemStyle.put("shadowOffsetX", 0);
        itemStyle.put("shadowColor", "rgba(0, 0, 0, 0.5)");
        emphasis.put("itemStyle", itemStyle);
        return emphasis;
    }
}

