package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

public class TimeLineCard extends Card {

    public TimeLineCard(TimeLineCardConfig config, Consumer<TimeLineCardConfig> onDelete) {
        setClassName("timeline-card");
        Button delete = new Button("löschen", e -> onDelete.accept(config));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        if (config != null) {

            LocalDate treatmentDate = config.getTreatmenDate();
            String wochentagString = treatmentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());

            setTitle(new Div(treatmentDate.toString()));
            setSubtitle(new Div("Wochentag: " + wochentagString));
            add(new Paragraph(config.getDescription()));
        }
        add(delete);
        // Styling (optional)
        getStyle().set("border", "1px solid #ddd");
        getStyle().set("padding", "0.5rem");
        getStyle().set("margin-bottom", "0.4rem");
        setWidthFull();
    }
}
