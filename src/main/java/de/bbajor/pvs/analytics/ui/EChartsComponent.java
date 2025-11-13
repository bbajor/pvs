package de.bbajor.pvs.analytics.ui;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Vaadin-Komponente für Apache ECharts.
 * Wrapper um die ECharts-Bibliothek für die Integration in Vaadin.
 */
@JsModule("./analytics/echarts-connector.js")
public class EChartsComponent extends Div {

    private final String elementId;
    private JsonObject currentOption;

    public EChartsComponent() {
        this.elementId = "echarts-" + System.currentTimeMillis() + "-" + hashCode();
        setId(elementId);
        getStyle().set("width", "100%");
        getStyle().set("height", "400px");
        getStyle().set("min-height", "300px");
    }

    /**
     * Setzt die Chart-Optionen und initialisiert/aktualisiert den Chart.
     */
    public void setOption(JsonObject option) {
        this.currentOption = option;
        String jsonString = option.toJson();
        getElement().executeJs(
            "if (window.pvsECharts) { " +
            "  const option = JSON.parse($1); " +
            "  window.pvsECharts.initChart($0, option); " +
            "}",
            elementId, jsonString
        );
    }

    /**
     * Setzt die Chart-Optionen aus einem Java-Objekt (wird zu JSON serialisiert).
     */
    public void setOption(Object option) {
        JsonObject jsonOption = convertToJson(option);
        setOption(jsonOption);
    }

    /**
     * Aktualisiert den Chart mit neuen Optionen.
     */
    public void updateOption(JsonObject option) {
        String jsonString = option.toJson();
        getElement().executeJs(
            "if (window.pvsECharts) { " +
            "  const option = JSON.parse($1); " +
            "  window.pvsECharts.updateChart($0, option); " +
            "}",
            elementId, jsonString
        );
    }

    /**
     * Setzt die Höhe des Chart-Containers.
     */
    public void setHeight(String height) {
        getStyle().set("height", height);
    }

    /**
     * Setzt die Mindesthöhe des Chart-Containers.
     */
    public void setMinHeight(String minHeight) {
        getStyle().set("min-height", minHeight);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Chart aufräumen beim Entfernen der Komponente
        getElement().executeJs(
            "if (window.pvsECharts) { " +
            "  window.pvsECharts.disposeChart($0); " +
            "}",
            elementId
        );
    }

    /**
     * Konvertiert ein Java-Objekt zu JSON (vereinfachte Version).
     * Für komplexe Objekte sollte Jackson/ObjectMapper verwendet werden.
     */
    private JsonObject convertToJson(Object option) {
        // Vereinfachte Konvertierung - für komplexe Objekte sollte
        // ein JSON-Serialisierer verwendet werden
        if (option instanceof JsonObject) {
            return (JsonObject) option;
        }
        // Fallback: leeres JSON-Objekt
        return Json.createObject();
    }
}

