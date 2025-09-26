package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TimelineView extends VerticalLayout {

    // TODO: anpassen auf die eigenen TimeSlots und Cards!!!
    private final Scroller scroller = new Scroller(ScrollDirection.HORIZONTAL);
    private final HorizontalLayout timelineLayout = new HorizontalLayout();
    private final List<TimeLineCardConfig> itemList = new ArrayList<>();
    private boolean isOnlyShowFutureAndPresentCards = false;

    public TimelineView() {
        timelineLayout.setAlignItems(Alignment.CENTER);
        scroller.setContent(timelineLayout);
        add(scroller);
    }

    public void setItems(List<TimeLineCardConfig> items) {
        this.itemList.clear();
        this.itemList.addAll(items);
        timelineLayout.removeAll();

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
            Card card = createCard(current.getTreatmenDate());
            timelineLayout.add(card);

            prev = current;
        }
    }

    public void setOnlyShowFutureAndPresentCards(boolean isOnlyShowFutureAndPresentCards) {
        this.isOnlyShowFutureAndPresentCards = isOnlyShowFutureAndPresentCards;
    }

    private Card createCard(LocalDate date) {
        Card card = new Card();
        card.setTitle(date.toString());
        card.getStyle().set("border", "1px solid #ccc");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("padding", "1em");
        card.getStyle().set("min-width", "120px");
        card.getStyle().set("text-align", "center");
        card.getStyle().set("background-color", "white");
        card.getStyle().set("box-shadow", "2px 2px 5px rgba(0,0,0,0.1)");
        return card;
    }
}