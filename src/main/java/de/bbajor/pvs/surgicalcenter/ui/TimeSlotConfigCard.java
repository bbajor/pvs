package de.bbajor.pvs.surgicalcenter.ui;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;

import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;

public class TimeSlotConfigCard extends Card {

    public TimeSlotConfigCard(TimeSlotConfig config, Consumer<TimeSlotConfig> onDelete) {
        setClassName("konfig-card");
        Button delete = new Button("löschen", e -> onDelete.accept(config));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        if (config != null) {

            TimePeriod selectedTimePeriod = config.getTimePeriod();
            TimeSlotRepetition selectedTimeSlotRepetition = config.getTimeSlotRepetition();
            LocalTime selectedTimeSlotBegin = config.getStartTime();
            LocalTime selectedTimeSlotEnd = config.getEndTime();
            DayOfWeek selectedDayOfWeek = config.getDayOfWeek();
            boolean isSingleAppointment = config.isSingleAppointment;
            String wochentagString = selectedDayOfWeek != null
                    ? selectedDayOfWeek.getDisplayName(TextStyle.FULL, getLocale())
                    : "-";

            setTitle(new Div(
                    isSingleAppointment ? "Einzeltermin anlegen" : "Zeitraum: " + selectedTimePeriod.toString()));

            setSubtitle(new Div("Wochentag: " + wochentagString));

            String detailString = isSingleAppointment ? "Erzeugt einen einzigen Termin für einen OP-Slot."
                    : "Erzeugt neue OP-Slots über folgenden Zeitraum: " + selectedTimePeriod.toString();
            add(new Paragraph(detailString));

            add(new Paragraph("Die Slots werden an folgendem Wochentag erstellt: " + wochentagString));
            String repetitionString = "Wiederholung: " + (null == selectedTimeSlotRepetition ? "-"
                    : selectedTimeSlotRepetition.toString());
            add(new Paragraph(repetitionString));
            add(createTimeDiv(selectedTimeSlotBegin, selectedTimeSlotEnd));
        }
        add(delete);
        // Styling (optional)
        getStyle().set("border", "1px solid #ddd");
        getStyle().set("padding", "0.5rem");
        getStyle().set("margin-bottom", "0.4rem");
        setWidthFull();
    }

    private static Div createTimeDiv(LocalTime selectedTimeSlotBegin, LocalTime selectedTimeSlotEnd) {
        // TODO Replace with real application logo and name
        var appLogo = VaadinIcon.CALENDAR.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span(selectedTimeSlotBegin + " - " + selectedTimeSlotEnd + " Uhr");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }
}
