package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TimelineView extends VerticalLayout {

    private final Scroller scroller = new Scroller(ScrollDirection.HORIZONTAL);
    private HorizontalLayout timelineLayout = new HorizontalLayout();
    private List<TimeLineCardConfig> itemList = new ArrayList<>();
    private Map<TimeLineCardConfig, TimeLineCard> configToComponentMap = new HashMap<>();
    private boolean isOnlyShowFutureAndPresentCards = false;

    public TimelineView() {
        timelineLayout = new HorizontalLayout();
        timelineLayout.setAlignItems(Alignment.CENTER);
        scroller.setContent(timelineLayout);
        add(scroller);
    }

    public void setItems(List<TimeLineCardConfig> items) {
        timelineLayout = new HorizontalLayout();
        timelineLayout.setAlignItems(Alignment.CENTER);
        scroller.setContent(timelineLayout);

        if (isOnlyShowFutureAndPresentCards) {
            itemList = items.stream().filter(item -> item.getTreatmenDate().isAfter(LocalDate.now())
                    || item.getTreatmenDate().isEqual(LocalDate.now())).toList();
        }

        this.itemList = items;

        TimeLineCardConfig prev = null;
        for (TimeLineCardConfig current : itemList) {
            if (prev != null) {
                long days = ChronoUnit.DAYS.between(prev.getTreatmenDate(), current.getTreatmenDate());
                long weeks = ChronoUnit.WEEKS.between(prev.getTreatmenDate(), current.getTreatmenDate());

                // Berechne Pixelbreite proportional
                int maxWidth = 400; // maximale Linienstrecke
                int px = (int) Math.min(days * 10, maxWidth); // z.B. 10px pro Tag

                Div line = new Div();
                line.getStyle().set("height", "2px");
                line.getStyle().set("background-color", "#999");
                line.getStyle().set("flex-shrink", "0"); // verhindert, dass es zusammenschrumpft
                line.setWidth(px + "px");

                // optional: Label mittig über der Linie
                Span label = new Span(weeks + " Wochen");
                Div lineWithLabel = new Div(line, label);
                lineWithLabel.getStyle().set("display", "flex");
                lineWithLabel.getStyle().set("flex-direction", "column");
                lineWithLabel.getStyle().set("align-items", "center");
                lineWithLabel.getStyle().set("justify-content", "center");

                timelineLayout.add(lineWithLabel);
            }

            // Card simulieren (kann Vaadin Card-Component oder eigener Container sein)
            TimeLineCard card = createCard(current);
            timelineLayout.add(card);
            prev = current;
            configToComponentMap.put(current, card);
        }
    }

    public void setOnlyShowFutureAndPresentCards(boolean isOnlyShowFutureAndPresentCards) {
        this.isOnlyShowFutureAndPresentCards = isOnlyShowFutureAndPresentCards;
    }

    private TimeLineCard createCard(TimeLineCardConfig config) {
        TimeLineCard card = new TimeLineCard(config, t -> {
            itemList.remove(t);
            timelineLayout.remove(configToComponentMap.get(t));
        });
        return card;

    }
}