package de.bbajor.pvs.analytics.dto;

import java.util.Map;

/**
 * DTO für Altersgruppen-Statistiken.
 */
public record AgeGroupStatistics(
    Map<String, Long> ageGroups
) {
}







