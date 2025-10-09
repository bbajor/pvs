package de.bbajor.pvs.base.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateAndTimeUtils {

    private static final DateTimeFormatter germanDateTimeFormatter = DateTimeFormatter
            .ofPattern("dd.MM.yyyy") // klassisch deutsch
            .withLocale(Locale.GERMAN);

    public static DateTimeFormatter getGermanDateTimeFormatter() {
        return germanDateTimeFormatter;
    }

}
