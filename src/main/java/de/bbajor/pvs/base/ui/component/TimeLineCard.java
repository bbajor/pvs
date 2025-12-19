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

import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

public class TimeLineCard extends Card {

    public TimeLineCard(TimeLineCardConfig config, Consumer<TimeLineCardConfig> onDelete,
            Consumer<TimeLineCardConfig> onClick) {
        this(config, onDelete, null, onClick, null, false);
    }

    public TimeLineCard(TimeLineCardConfig config, Consumer<TimeLineCardConfig> onDelete,
            Consumer<TimeLineCardConfig> onClick, Runnable onBookNextTreatment, boolean isLastTreatment) {
        this(config, onDelete, null, onClick, onBookNextTreatment, isLastTreatment);
    }
    
    public TimeLineCard(TimeLineCardConfig config, Consumer<TimeLineCardConfig> onDelete,
            Consumer<TimeLineCardConfig> onCancel, Consumer<TimeLineCardConfig> onClick, 
            Runnable onBookNextTreatment, boolean isLastTreatment) {
        addClassName("timeline-card");

        if (config == null) {
            // Return early if config is null - used for testing
            return;
        }

        LocalDate now = LocalDate.now();

        if (!config.isFirst()) {
            if (config.getTreatmentDate().isBefore(now)) {
                addClassName("past");
                // Abgelaufene Termine ausgrauen
                getStyle().set("opacity", "0.6");
                getStyle().set("filter", "grayscale(30%)");
            } else if (config.getTreatmentDate().isAfter(now)) {
                addClassName("future");
                // Noch nicht wahrgenommene Termine optisch hervorheben (passende Farbe)
                getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
                getStyle().set("border", "2px solid var(--lumo-primary-color)");
                getStyle().set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.1)");
            } else {
                addClassName("current");
                // Heutige Termine ebenfalls hervorheben
                getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
                getStyle().set("border", "2px solid var(--lumo-primary-color)");
            }
            SurgicalCenterTimeSlot timeSlot = config.getTreatment().getSurgicalCenterTimeSlot();
            LocalDate treatmentDate = timeSlot.getDate();
            String wochentagString = treatmentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());
            setTitle(
                    new Div(("Behandlung am: ") + DateAndTimeUtils.getGermanDateTimeFormatter().format(treatmentDate)));
            setSubtitle(new Div("Wochentag: " + wochentagString));

            if (!config.isFirst()) {
                // Nur Medikament anzeigen (statt Behandlungsort, Auge, Uhrzeit)
                if (config.getTreatment().getMedicationFavourite() != null 
                        && config.getTreatment().getMedicationFavourite().getMedication() != null) {
                    String medicationName = config.getTreatment().getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                    add(new Paragraph("Medikament: " + medicationName));
                }
                // Visus anzeigen, falls vorhanden
                if (config.getTreatment().getVisualAcuity() != null && !config.getTreatment().getVisualAcuity().trim().isEmpty()) {
                    add(new Paragraph("Visus: " + config.getTreatment().getVisualAcuity()));
                }
                String additionalInfo = config.getAdditionalInfo();
                if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                    add(new Paragraph(additionalInfo));
                }
                // Löschen/Absagen erlauben, wenn Termin in der Zukunft liegt und nicht genehmigt
                LocalDate today = LocalDate.now();
                boolean isFuture = treatmentDate != null && !treatmentDate.isBefore(today);
                boolean canDeleteOrCancel = isFuture && !config.isApproved() && (onDelete != null || onCancel != null);
                
                if (canDeleteOrCancel) {
                    // Prüfe, ob Termin mindestens 2 Tage in der Zukunft liegt
                    long daysUntil = treatmentDate != null ? java.time.temporal.ChronoUnit.DAYS.between(today, treatmentDate) : -1;
                    boolean canDelete = daysUntil >= 2;
                    
                    if (canDelete && onDelete != null) {
                        // Löschen-Button (mindestens 2 Tage in der Zukunft)
                        Button delete = new Button("Löschen", e -> onDelete.accept(config));
                        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
                        add(delete);
                    } else if (!canDelete && onCancel != null) {
                        // Absagen-Button (weniger als 2 Tage in der Zukunft)
                        Button cancel = new Button("Absagen", e -> onCancel.accept(config));
                        cancel.addThemeVariants(ButtonVariant.LUMO_WARNING);
                        add(cancel);
                    }
                }
                if (onClick != null && config.getTreatment() != null) {
                    Button detailsButton = new Button("Details", event -> onClick.accept(config));
                    detailsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    add(detailsButton);
                }
            }
        } else {
            // Erste Card: Andere Darstellung, damit klar ist, dass es keine Behandlung ist
            setTitle(new Div("Behandlungsplan"));
            String wochentagString = config.getFirstDate().getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());
            setSubtitle(new Div("Start: " + DateAndTimeUtils.getGermanDateTimeFormatter().format(config.getFirstDate()) + " (" + wochentagString + ")"));
            
            // Statistiken anzeigen, falls vorhanden
            if (config.getTreatmentCount() != null && config.getTreatmentCount() > 0) {
                add(new Paragraph("Anzahl Behandlungen: " + config.getTreatmentCount()));
            }
            if (config.getMostCommonInterval() != null) {
                add(new Paragraph("Häufigstes Intervall: " + config.getMostCommonInterval() + " Wochen"));
            }
            
            String additionalInfo = config.getAdditionalInfo();
            if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                add(new Paragraph(additionalInfo));
            }
            
            addClassName("start");
            // Zusätzliches Styling für die erste Card, um sie von Behandlungen zu unterscheiden
            getStyle().set("border-style", "dashed");
            getStyle().set("border-width", "2px");
        }
        // Styling (optional)
        getStyle().set("border", "1px solid #ddd");
        getStyle().set("padding", "0.5rem");
        getStyle().set("margin-bottom", "0.4rem");
        getStyle().set("width", "fit-content"); // Minimale benötigte Breite
        getStyle().set("min-width", "fit-content"); // Keine Mindestbreite
        getStyle().set("max-width", "fit-content"); // Maximale Breite = benötigte Breite
        getStyle().set("flex-shrink", "0");
    }
}
