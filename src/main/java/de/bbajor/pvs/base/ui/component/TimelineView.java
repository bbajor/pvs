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
        
        // Verhindere horizontales Overflow in der Root-Komponente
        setWidthFull();
        getStyle().set("overflow-x", "hidden");
        getStyle().set("overflow-y", "hidden");
        getStyle().set("max-width", "100%"); // Stelle sicher, dass nicht breiter als Container

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
        // Nicht setWidthFull() - Content sollte auf Basis der Kinder skaliert werden
        timelineLayout.getStyle().set("display", "flex");
        timelineLayout.getStyle().set("align-items", "center");
        timelineLayout.getStyle().set("padding", "20px 0");
        timelineLayout.getStyle().set("flex-wrap", "nowrap");
        timelineLayout.getStyle().set("gap", "20px");
        timelineLayout.getStyle().set("min-width", "100%"); // Mindestens Container-Breite
        timelineLayout.getStyle().set("flex-shrink", "0"); // Verhindere Schrumpfen

        // Scroller hosting the timeline
        scroller = new Scroller(timelineLayout);
        scroller.addClassName("timeline-scroller");
        scroller.setScrollDirection(ScrollDirection.HORIZONTAL);
        // Keine feste Breite hier setzen - wird vom Layout bestimmt

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
        this.itemList = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
        refresh(); // UI mit den neuen Daten aktualisieren
    }

    /**
     * NEU: Zentrale Methode, die die Timeline basierend auf den aktuellen Daten und
     * Einstellungen neu aufbaut.
     */
    public void refresh() {
        LOG.info(() -> String.format("refresh called: %d items", itemList.size()));
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
        List<TimeLineCardConfig> result = new ArrayList<>(this.itemList);
        
        // Füge nur die "Erste"-Karte hinzu, wenn sie noch nicht vorhanden ist
        boolean hasFirstCard = result.stream().anyMatch(TimeLineCardConfig::isFirst);
        if (!hasFirstCard) {
            result.add(0, new TimeLineCardConfig().setFirst(true).setFirstDate(startOfTreatmentPlan));
        }

        if (isOnlyShowFutureAndPresentCards) {
            return result.stream()
                    .filter(item -> item.getTreatmentDate() != null && !item.getTreatmentDate().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());
        }

        return result;
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
            // Try to delete via service (secured by roles)
            try {
                if (t.getTreatment() != null && t.getTreatment().getId() != null) {
                    context.getBean(TreatmentPlanService.class).deleteTreatment(t.getTreatment().getId());
                }
            } catch (Exception ex) {
                // ignore to keep UI responsive; server will enforce permissions
            }
            itemList.remove(t);
            refresh();
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
                
                // Scrolle nur innerhalb des Scrollers zur nächsten Behandlung (nicht die gesamte View)
                if (orientation == Orientation.HORIZONTAL) {
                    scroller.getElement().executeJs(
                        "setTimeout(() => {" +
                        "  const scroller = this.shadowRoot && this.shadowRoot.querySelector('[part=\"content\"]') || this;" +
                        "  const card = $0;" +
                        "  if (scroller && card) {" +
                        "    const cardRect = card.getBoundingClientRect();" +
                        "    const scrollerRect = scroller.getBoundingClientRect();" +
                        "    const scrollLeft = scroller.scrollLeft + cardRect.left - scrollerRect.left - (scrollerRect.width / 2) + (cardRect.width / 2);" +
                        "    scroller.scrollTo({ left: scrollLeft, behavior: 'smooth' });" +
                        "  }" +
                        "}, 500);",
                        card.getElement());
                } else {
                    scroller.getElement().executeJs(
                        "setTimeout(() => {" +
                        "  const scroller = this.shadowRoot && this.shadowRoot.querySelector('[part=\"content\"]') || this;" +
                        "  const card = $0;" +
                        "  if (scroller && card) {" +
                        "    const cardRect = card.getBoundingClientRect();" +
                        "    const scrollerRect = scroller.getBoundingClientRect();" +
                        "    const scrollTop = scroller.scrollTop + cardRect.top - scrollerRect.top - (scrollerRect.height / 2) + (cardRect.height / 2);" +
                        "    scroller.scrollTo({ top: scrollTop, behavior: 'smooth' });" +
                        "  }" +
                        "}, 500);",
                        card.getElement());
                }
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
            // Nur Layout neu aufbauen, ohne refresh() zu rufen, um Duplikate zu vermeiden
            rebuildOuterLayout();
            applyOrientationStylesAndBehavior();
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
            // Buttons außerhalb des Scrollers, damit sie immer sichtbar bleiben
            HorizontalLayout container = new HorizontalLayout(prevButton, scroller, nextButton);
            container.setWidthFull();
            container.setAlignItems(Alignment.CENTER);
            container.setSpacing(false);
            container.setPadding(false);
            container.expand(scroller);
            
            // Sicherstellen, dass Scroller die verfügbare Breite nutzt
            scroller.setWidthFull();
            scroller.setMinWidth("0px");
            
            // Buttons sollten nicht schrumpfen
            prevButton.getStyle().set("flex-grow", "0");
            prevButton.getStyle().set("flex-shrink", "0");
            nextButton.getStyle().set("flex-grow", "0");
            nextButton.getStyle().set("flex-shrink", "0");
            
            add(container);
        } else {
            VerticalLayout container = new VerticalLayout();
            container.setSpacing(false);
            container.setPadding(false);
            container.setWidthFull();
            container.setDefaultHorizontalComponentAlignment(VerticalLayout.Alignment.STRETCH);
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
            timelineLayout.getStyle().set("width", "max-content");
            timelineLayout.getStyle().set("min-width", "100%");
            scroller.setScrollDirection(ScrollDirection.HORIZONTAL);
            scroller.getStyle().set("overflow-x", "scroll"); // Immer Scrollbar anzeigen
            scroller.getStyle().set("overflow-y", "hidden");
            // Update button icons
            prevButton.setIcon(new Icon(VaadinIcon.ANGLE_LEFT));
            nextButton.setIcon(new Icon(VaadinIcon.ANGLE_RIGHT));
        } else {
            timelineLayout.getStyle().set("flex-direction", "column");
            timelineLayout.getStyle().set("height", "max-content");
            timelineLayout.getStyle().set("min-height", "100%");
            scroller.setScrollDirection(ScrollDirection.VERTICAL);
            scroller.getStyle().set("overflow-y", "auto");
            scroller.getStyle().set("overflow-x", "hidden");
            scroller.getStyle().set("width", "100%");
            prevButton.setIcon(new Icon(VaadinIcon.ANGLE_UP));
            nextButton.setIcon(new Icon(VaadinIcon.ANGLE_DOWN));
        }

        // Update root element orientation classes (for CSS hooks)
        getElement().getClassList().remove("horizontal");
        getElement().getClassList().remove("vertical");
        getElement().getClassList().add(orientation == Orientation.HORIZONTAL ? "horizontal" : "vertical");

        // Ensure scrollbar space is reserved and visible
        scroller.getStyle().set("scrollbar-gutter", "stable both-edges");
    }

    private void scrollPrev() {
        int delta = -400; // px
        if (orientation == Orientation.HORIZONTAL) {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\\"content\\\"]') || this; c.scrollBy({ left: $0, behavior: 'smooth' })",
                delta);
        } else {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\\"content\\\"]') || this; c.scrollBy({ top: $0, behavior: 'smooth' })",
                delta);
        }
    }

    private void scrollNext() {
        int delta = 400; // px
        if (orientation == Orientation.HORIZONTAL) {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\\"content\\\"]') || this; c.scrollBy({ left: $0, behavior: 'smooth' })",
                delta);
        } else {
            scroller.getElement().executeJs(
                "const c=this.shadowRoot && this.shadowRoot.querySelector('[part=\\\"content\\\"]') || this; c.scrollBy({ top: $0, behavior: 'smooth' })",
                delta);
        }
    }
}