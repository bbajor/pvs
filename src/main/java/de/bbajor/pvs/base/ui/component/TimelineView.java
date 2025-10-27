package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.intravitreal.treatment.ui.TreatmentDetailDialog;

public class TimelineView extends VerticalLayout {

    private static final Logger LOG = Logger.getLogger(TimelineView.class.getName());

    public enum Orientation { HORIZONTAL, VERTICAL }

    private final Scroller scroller;
    private final Div timelineLayout;
    private final Button prevButton;
    private final Button nextButton;
    private Orientation orientation = Orientation.HORIZONTAL;
    private final Map<TimeLineCardConfig, TimeLineCard> configToComponentMap = new HashMap<>();

    private List<TimeLineCardConfig> itemList = new ArrayList<>();
    private boolean isOnlyShowFutureAndPresentCards = false;
    private LocalDate startOfTreatmentPlan = LocalDate.now();
    private final ApplicationContext context;

    public TimelineView(ApplicationContext context) {
        addClassName("timeline-view");
        Objects.requireNonNull(context);

        this.context = context;

        // Buttons to scroll timeline
        prevButton = new Button(new Icon(VaadinIcon.ANGLE_LEFT));
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_LARGE);
        prevButton.addClassName("timeline-scroll-button");
        prevButton.addClickListener(e -> scrollPrev());

        nextButton = new Button(new Icon(VaadinIcon.ANGLE_RIGHT));
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_LARGE);
        nextButton.addClassName("timeline-scroll-button");
        nextButton.addClickListener(e -> scrollNext());

        // Container for timeline content
        timelineLayout = new Div();
        timelineLayout.addClassName("timeline-content");
        timelineLayout.setWidthFull();
        timelineLayout.getStyle().set("display", "flex");
        timelineLayout.getStyle().set("align-items", "center");
        timelineLayout.getStyle().set("padding", "20px 0");
        timelineLayout.getStyle().set("flex-wrap", "nowrap");
        timelineLayout.getStyle().set("gap", "20px");

        // Scroller hosting the timeline
        scroller = new Scroller(timelineLayout);
        scroller.addClassName("timeline-scroller");
        scroller.setScrollDirection(ScrollDirection.HORIZONTAL);
        scroller.setWidthFull();

        // Build outer layout based on current orientation
        rebuildOuterLayout();
        applyOrientationStylesAndBehavior();
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
        LOG.info("refresh called: " + itemList.size());
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

        // Markiere die nächste Behandlung und scrolle dorthin
        updateNextTreatmentStatus();
    }

    /**
     * NEU: Bereitet die Liste der anzuzeigenden Elemente vor.
     * Wendet Filter an oder erstellt die Start-/End-Marker, falls die Liste leer
     * ist.
     */
    private List<TimeLineCardConfig> prepareItemsForRendering() {
        itemList.add(new TimeLineCardConfig().setFirst(true).setFirstDate(startOfTreatmentPlan));

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
            if (orientation == Orientation.HORIZONTAL) {
                empty.setWidth("20px");
            } else {
                empty.setHeight("20px");
            }
            return empty;
        }
        long days = Math.abs(ChronoUnit.DAYS.between(prev.getTreatmentDate(), current.getTreatmentDate()));
        long weeks = Math.abs(ChronoUnit.WEEKS.between(prev.getTreatmentDate(), current.getTreatmentDate()));

        // Mindestbreite, damit auch bei kurzen Abständen eine Linie sichtbar ist
        int minSize = 30;
        int maxSize = 400;
        int pxPerDay = 10;
        int px = (int) Math.min(Math.max(days * pxPerDay, minSize), maxSize);

        Div line = new Div();
        line.getStyle().set("background-color", "#999");
        line.getStyle().set("flex-shrink", "0");

        // Label nur anzeigen, wenn mindestens eine Woche vergangen ist
        String labelText = weeks > 0 ? (weeks + (weeks == 1 ? " Woche" : " Wochen")) : "";
        Span label = new Span(labelText);
        label.getStyle().set("color", "#555");

        Div lineWithLabel;
        if (orientation == Orientation.HORIZONTAL) {
            line.setHeight("2px");
            line.setWidth(px + "px");

            lineWithLabel = new Div(label, line); // Label über der Linie
            lineWithLabel.getStyle().set("display", "flex");
            lineWithLabel.getStyle().set("flex-direction", "column-reverse"); // Linie unten, Text oben
            lineWithLabel.getStyle().set("align-items", "center");
            lineWithLabel.getStyle().set("justify-content", "center");
            lineWithLabel.getStyle().set("margin", "0 10px"); // Etwas Abstand zu den Karten
        } else {
            line.setWidth("2px");
            line.setHeight(px + "px");

            lineWithLabel = new Div(line, label); // Label rechts neben der vertikalen Linie
            lineWithLabel.getStyle().set("display", "flex");
            lineWithLabel.getStyle().set("flex-direction", "row");
            lineWithLabel.getStyle().set("align-items", "center");
            lineWithLabel.getStyle().set("justify-content", "center");
            lineWithLabel.getStyle().set("gap", "8px");
            lineWithLabel.getStyle().set("margin", "10px 0");
        }

        return lineWithLabel;
    }

    private TimeLineCard createCard(TimeLineCardConfig config) {
        // Das Entfernen aus der Liste sollte auch ein Neuzeichnen auslösen
        TimeLineCard card = new TimeLineCard(config, t -> {
            itemList.remove(t);
            refresh(); // Statt manuell Komponenten zu entfernen, einfach die View neu aufbauen
        },
                t2 -> {
                    // Hier können Sie die Logik für den Klick-Handler hinzufügen
                    TreatmentDetailDialog dialog = new TreatmentDetailDialog(t2.getTreatment(),
                            context.getBean(TreatmentPlanService.class));
                    dialog.open();
                });
        return card;
    }

    /**
     * Findet die nächste anstehende Behandlung und scrollt zu ihr
     */
    private void scrollToNextTreatment() {
        LocalDate now = LocalDate.now();
        
        // Finde die nächste Behandlung
        TimeLineCardConfig nextTreatment = itemList.stream()
            .filter(item -> !item.isFirst()) // Ignoriere den Start-Marker
            .filter(item -> item.getTreatmentDate() != null)
            .min(Comparator.comparing(item -> {
                long daysUntil = ChronoUnit.DAYS.between(now, item.getTreatmentDate());
                // Vergangene Behandlungen werden mit einem sehr hohen Wert versehen
                return daysUntil < 0 ? Long.MAX_VALUE : daysUntil;
            }))
            .orElse(null);

        if (nextTreatment != null) {
            TimeLineCard card = configToComponentMap.get(nextTreatment);
            if (card != null) {
                // Markiere die Karte als "next"
                card.addClassName("next");
                
                // Scrolle zur nächsten Behandlung (mit etwas Verzögerung für die Animation)
                card.getElement().executeJs(
                    "setTimeout(() => {" +
                    "  const card = this;" +
                    "  card.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });" +
                    "}, 500);");
            }
        }
    }

    /**
     * Aktualisiert den nächsten Behandlungs-Status für alle Karten
     */
    private void updateNextTreatmentStatus() {
        // Entferne zuerst alle "next" Klassen
        configToComponentMap.values().forEach(card -> card.removeClassName("next"));
        
        // Scrolle zur nächsten Behandlung
        scrollToNextTreatment();
    }

    /**
     * Setzt die Ausrichtung der Timeline und aktualisiert Layout und Scrollrichtung.
     */
    public void setOrientation(Orientation newOrientation) {
        if (newOrientation == null) {
            return;
        }
        if (this.orientation != newOrientation) {
            this.orientation = newOrientation;
            rebuildOuterLayout();
            applyOrientationStylesAndBehavior();
            refresh();
        }
    }

    /**
     * Setzt die sichtbare Höhe des internen Scrollers (z.B. "300px").
     */
    public void setTimelineHeight(String height) {
        scroller.setHeight(height);
    }

    private void rebuildOuterLayout() {
        removeAll();
        if (orientation == Orientation.HORIZONTAL) {
            HorizontalLayout container = new HorizontalLayout(prevButton, scroller, nextButton);
            container.setWidthFull();
            container.setAlignItems(Alignment.CENTER);
            container.expand(scroller);
            add(container);
        } else {
            VerticalLayout container = new VerticalLayout();
            container.setSpacing(false);
            container.setPadding(false);
            container.setWidthFull();
            container.setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
            container.add(prevButton);
            container.add(scroller);
            container.add(nextButton);
            add(container);
        }
    }

    private void applyOrientationStylesAndBehavior() {
        // Update timeline content flex direction
        if (orientation == Orientation.HORIZONTAL) {
            timelineLayout.getStyle().set("flex-direction", "row");
            scroller.setScrollDirection(ScrollDirection.HORIZONTAL);
            scroller.getStyle().set("overflow-x", "scroll");
            scroller.getStyle().set("overflow-y", "hidden");
            // Update button icons
            prevButton.setIcon(new Icon(VaadinIcon.ANGLE_LEFT));
            nextButton.setIcon(new Icon(VaadinIcon.ANGLE_RIGHT));
        } else {
            timelineLayout.getStyle().set("flex-direction", "column");
            scroller.setScrollDirection(ScrollDirection.VERTICAL);
            scroller.getStyle().set("overflow-y", "scroll");
            scroller.getStyle().set("overflow-x", "hidden");
            prevButton.setIcon(new Icon(VaadinIcon.ANGLE_UP));
            nextButton.setIcon(new Icon(VaadinIcon.ANGLE_DOWN));
        }

        // Update root element orientation classes (for CSS hooks)
        getElement().getClassList().remove("horizontal", "vertical");
        getElement().getClassList().add(orientation == Orientation.HORIZONTAL ? "horizontal" : "vertical");

        // Ensure scrollbar space is reserved and visible
        scroller.getStyle().set("scrollbar-gutter", "stable both-edges");
    }

    private void scrollPrev() {
        int delta = -400; // px
        if (orientation == Orientation.HORIZONTAL) {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\"content\\"]') || this; c.scrollBy({ left: $0, behavior: 'smooth' })",
                delta);
        } else {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\"content\\"]') || this; c.scrollBy({ top: $0, behavior: 'smooth' })",
                delta);
        }
    }

    private void scrollNext() {
        int delta = 400; // px
        if (orientation == Orientation.HORIZONTAL) {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\"content\\"]') || this; c.scrollBy({ left: $0, behavior: 'smooth' })",
                delta);
        } else {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\"content\\"]') || this; c.scrollBy({ top: $0, behavior: 'smooth' })",
                delta);
        }
    }
}