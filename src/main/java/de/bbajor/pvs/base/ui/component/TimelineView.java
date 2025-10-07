package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TimelineView extends VerticalLayout {

    private final Scroller scroller;
    private final HorizontalLayout timelineLayout;
    private final Map<TimeLineCardConfig, TimeLineCard> configToComponentMap = new HashMap<>();

    private List<TimeLineCardConfig> itemList = new ArrayList<>();
    private boolean isOnlyShowFutureAndPresentCards = false;

    // NEU: Separate Attribute für Start- und Enddatum der leeren Timeline
    private LocalDate startOfTreatmentPlan = LocalDate.now();

    public TimelineView() {
        // UI-Komponenten nur einmal initialisieren
        timelineLayout = new HorizontalLayout();
        timelineLayout.setWidthFull();

        timelineLayout.setAlignItems(Alignment.CENTER);
        timelineLayout.getStyle().set("padding", "20px 0"); // Etwas Platz, damit die Labels nicht am Rand kleben
        timelineLayout.getStyle().set("flex-wrap", "nowrap");
        timelineLayout.getStyle().set("gap", "20px");

        scroller = new Scroller(timelineLayout);
        scroller.setScrollDirection(ScrollDirection.HORIZONTAL);
        scroller.setWidthFull();

        add(scroller);
    }

    // NEU: Setter für das Startdatum (muss gesetzt werden)
    public void setStartOfTreatmentPlan(LocalDate startDate) {
        this.startOfTreatmentPlan = (startDate != null) ? startDate : LocalDate.now();
        refresh(); // UI bei Änderung aktualisieren
    }

    public void setOnlyShowFutureAndPresentCards(boolean isOnlyShowFutureAndPresentCards) {
        this.isOnlyShowFutureAndPresentCards = isOnlyShowFutureAndPresentCards;
        refresh(); // UI bei Änderung aktualisieren
    }

    public void setItems(List<TimeLineCardConfig> items) {
        this.itemList = (items != null) ? items : new ArrayList<>();
        refresh(); // UI mit den neuen Daten aktualisieren
    }

    /**
     * NEU: Zentrale Methode, die die Timeline basierend auf den aktuellen Daten und
     * Einstellungen neu aufbaut.
     */
    public void refresh() {
        System.out.println("refresh called: " + itemList.size());
        timelineLayout.removeAll();
        configToComponentMap.clear();

        List<TimeLineCardConfig> itemsToRender = prepareItemsForRendering();

        itemsToRender.forEach(i -> System.out.println(i.getAdditionalInfo() + " -> " + i.getTreatmentDate()));

        // Sortieren der Elemente nach Datum, um eine korrekte Darstellung zu
        // gewährleisten
        itemsToRender.sort(Comparator.comparing(
                i -> i.getTreatmentDate() != null ? i.getTreatmentDate() : LocalDate.MAX));

        TimeLineCardConfig prev = null;
        for (TimeLineCardConfig current : itemsToRender) {
            // Fügt die Verbindungslinie zwischen den Karten hinzu
            if (prev != null) {
                timelineLayout.add(createLineBetween(prev, current));
            }

            // Fügt die Karte selbst hinzu
            TimeLineCard card = createCard(current);
            timelineLayout.add(card);
            configToComponentMap.put(current, card);

            prev = current;
        }
    }

    /**
     * NEU: Bereitet die Liste der anzuzeigenden Elemente vor.
     * Wendet Filter an oder erstellt die Start-/End-Marker, falls die Liste leer
     * ist.
     */
    private List<TimeLineCardConfig> prepareItemsForRendering() {
        itemList.add(new TimeLineCardConfig("Start", startOfTreatmentPlan));

        if (isOnlyShowFutureAndPresentCards) {
            return this.itemList.stream()
                    .filter(item -> !item.getTreatmentDate().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>(this.itemList); // Eine Kopie zurückgeben, um die Originalliste nicht zu verändern
    }

    private Div createLineBetween(TimeLineCardConfig prev, TimeLineCardConfig current) {
        if (prev.getTreatmentDate() == null || current.getTreatmentDate() == null) {
            Div empty = new Div();
            empty.setWidth("20px");
            return empty;
        }
        long days = Math.abs(ChronoUnit.DAYS.between(prev.getTreatmentDate(), current.getTreatmentDate()));
        long weeks = Math.abs(ChronoUnit.WEEKS.between(prev.getTreatmentDate(), current.getTreatmentDate()));

        // Mindestbreite, damit auch bei kurzen Abständen eine Linie sichtbar ist
        int minWidth = 30;
        int maxWidth = 400;
        int pxPerDay = 10;
        int px = (int) Math.min(Math.max(days * pxPerDay, minWidth), maxWidth);

        Div line = new Div();
        line.setHeight("2px");
        line.getStyle().set("background-color", "#999");
        line.getStyle().set("flex-shrink", "0");
        line.setWidth(px + "px");

        // Label nur anzeigen, wenn mindestens eine Woche vergangen ist
        String labelText = weeks > 0 ? (weeks + (weeks == 1 ? " Woche" : " Wochen")) : "";
        Span label = new Span(labelText);
        label.getStyle().set("color", "#555");

        Div lineWithLabel = new Div(label, line); // Label über der Linie
        lineWithLabel.getStyle().set("display", "flex");
        lineWithLabel.getStyle().set("flex-direction", "column-reverse"); // Linie unten, Text oben
        lineWithLabel.getStyle().set("align-items", "center");
        lineWithLabel.getStyle().set("justify-content", "center");
        lineWithLabel.getStyle().set("margin", "0 10px"); // Etwas Abstand zu den Karten

        return lineWithLabel;
    }

    private TimeLineCard createCard(TimeLineCardConfig config) {
        // Das Entfernen aus der Liste sollte auch ein Neuzeichnen auslösen
        TimeLineCard card = new TimeLineCard(config, t -> {
            itemList.remove(t);
            refresh(); // Statt manuell Komponenten zu entfernen, einfach die View neu aufbauen
        });
        return card;
    }
}