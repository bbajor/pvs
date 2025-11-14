package de.bbajor.pvs.analytics.dto;

import java.util.Objects;

/**
 * DTO für Zeitreihen-Daten (z.B. Behandlungen pro Monat/Jahr).
 */
public record TimeSeriesData(
    String label,
    Long value
) {
    public TimeSeriesData {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}


