package de.bbajor.pvs.base.misc;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

import com.vaadin.flow.component.ItemLabelGenerator;

public class DayOfWeekItemLabelGenerator implements ItemLabelGenerator<DayOfWeek> {

    private final Locale locale;

    public DayOfWeekItemLabelGenerator(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String apply(DayOfWeek dayOfWeek) {
        return dayOfWeek == null ? "" : dayOfWeek.getDisplayName(TextStyle.FULL, locale);
    }

}
