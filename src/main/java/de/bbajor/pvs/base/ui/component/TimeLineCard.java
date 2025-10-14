package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Objects;
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
        Objects.requireNonNull(config);
        addClassName("timeline-card");

        LocalDate now = LocalDate.now();

        if (!config.isFirst()) {
            if (config.getTreatmentDate().isBefore(now)) {
                addClassName("past");
            } else if (config.getTreatmentDate().isAfter(now)) {
                addClassName("future");
            } else {
                addClassName("current");
            }
            SurgicalCenterTimeSlot timeSlot = config.getTreatment().getSurgicalCenterTimeSlot();
            LocalDate treatmentDate = timeSlot.getDate();
            String wochentagString = treatmentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());
            setTitle(
                    new Div(("Behandlung am: ") + DateAndTimeUtils.getGermanDateTimeFormatter().format(treatmentDate)));
            setSubtitle(new Div("Wochentag: " + wochentagString));

            if (!config.isFirst()) {
                SideOfEye sideOfEye = config.getTreatment().getSideOfEye();
                String sideOfEyeText = "";
                if (treatmentDate.isAfter(now)) {
                    sideOfEyeText = "Geplantes ";
                } else if (treatmentDate.isEqual(now)) {
                    LocalTime nowTime = LocalTime.now();
                    if (timeSlot.getStartTime().isAfter(nowTime)) {
                        sideOfEyeText = "Geplantes ";
                    } else {
                        sideOfEyeText = "Heutiges ";
                    }
                } else {
                    sideOfEyeText = "Behandeltes ";
                }
                sideOfEyeText += "Auge: ";
                add(new Paragraph(sideOfEyeText + sideOfEye.toString()));
                String locationInfo = timeSlot.getSurgicalCenter().getName();
                add(new Paragraph("Behandlungsort: " + locationInfo));
                LocalTime startTime = timeSlot.getStartTime();
                add(new Paragraph("Uhrzeit: " + startTime.toString()));
                String additionalInfo = config.getAdditionalInfo();
                if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                    add(new Paragraph(additionalInfo));
                }
                if (timeSlot.getDate().isAfter(LocalDate.now()) && !config.isApproved() && onDelete != null) {
                    Button delete = new Button("löschen", e -> onDelete.accept(config));
                    delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
                    add(delete);
                }
                if (onClick != null && config.getTreatment() != null) {
                    Button detailsButton = new Button("Details", event -> onClick.accept(config));
                    detailsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    add(detailsButton);
                }
            }
        } else {
            setTitle(new Div("In Behandlung seit: "
                    + DateAndTimeUtils.getGermanDateTimeFormatter().format(config.getFirstDate())));
            String wochentagString = config.getFirstDate().getDayOfWeek().getDisplayName(TextStyle.FULL, getLocale());
            setSubtitle(new Div("Wochentag: " + wochentagString));
            String additionalInfo = config.getAdditionalInfo();
            if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                add(new Paragraph(additionalInfo));
            }
            addClassName("start");
        }
        // Styling (optional)
        getStyle().set("border", "1px solid #ddd");
        getStyle().set("padding", "0.5rem");
        getStyle().set("margin-bottom", "0.4rem");
        getStyle().set("min-width", "120px");
        getStyle().set("flex-shrink", "0");
    }
}
