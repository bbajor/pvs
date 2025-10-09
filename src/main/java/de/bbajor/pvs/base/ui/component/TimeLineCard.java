package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

import de.bbajor.pvs.base.util.SideOfEye;

public class TimeLineCard extends Card {

    public TimeLineCard(TimeLineCardConfig config, Consumer<TimeLineCardConfig> onDelete) {
        setClassName("timeline-card");

        if (config != null) {

            LocalDate treatmentDate = config.getTreatmentDate();
            String wochentagString = treatmentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());
            setTitle(new Div(
                    (config.isFirst() ? "Start der Behandlung: " : "Behandlung am: ") + treatmentDate.toString()));
            setSubtitle(new Div("Wochentag: " + wochentagString));
            String additionalInfo = config.getAdditionalInfo();
            if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                add(new Paragraph(additionalInfo));
            }

            if (!config.isFirst()) {
                LocalTime startTime = config.getStartTime();
                String locationInfo = config.getLocationInfo();
                SideOfEye sideOfEye = config.getSideOfEye();
                add(new Paragraph("Uhrzeit: " + startTime.toString()));
                add(new Paragraph("Ort: " + locationInfo));
                add(new Paragraph(sideOfEye.toString()));
                if (config.getTreatmentDate().isAfter(LocalDate.now()) && onDelete != null) {
                    Button delete = new Button("löschen", e -> onDelete.accept(config));
                    delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    add(delete);
                }
            }
        }
        // Styling (optional)
        getStyle().set("border", "1px solid #ddd");
        getStyle().set("padding", "0.5rem");
        getStyle().set("margin-bottom", "0.4rem");
        getStyle().set("min-width", "120px");
        getStyle().set("flex-shrink", "0");
    }
}
