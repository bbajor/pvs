package de.bbajor.pvs.kbv.events;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Event signaling that KBV-Stammdaten sollen für eine Institution ausgerollt werden.
 *
 * @param institutionCode eindeutiger Institutionscode
 * @param quarter         Quartal der Stammdaten (z. B. 2025-Q1)
 * @param version         optionale Versionskennung des Imports
 * @param requestedAt     Zeitpunkt der Anforderung (UTC)
 */
public record KbvTenantDistributionEvent(
        String institutionCode,
        String quarter,
        String version,
        OffsetDateTime requestedAt) {

    public KbvTenantDistributionEvent(String institutionCode, String quarter, String version) {
        this(institutionCode, quarter, version, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
