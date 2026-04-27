package de.bbajor.pvs.system.update;

import java.time.OffsetDateTime;

public record ApplicationUpdateStatus(
        boolean enabled,
        String currentVersion,
        String latestVersion,
        boolean updateAvailable,
        String message,
        OffsetDateTime checkedAt) {

    public static ApplicationUpdateStatus disabled(String currentVersion) {
        return new ApplicationUpdateStatus(
                false,
                currentVersion,
                "unbekannt",
                false,
                "App-Updates sind nicht aktiviert.",
                OffsetDateTime.now());
    }

    public static ApplicationUpdateStatus notConfigured(String currentVersion) {
        return new ApplicationUpdateStatus(
                true,
                currentVersion,
                "unbekannt",
                false,
                "Keine Update-Quelle konfiguriert.",
                OffsetDateTime.now());
    }

    public static ApplicationUpdateStatus available(String currentVersion, String latestVersion, boolean updateAvailable) {
        return new ApplicationUpdateStatus(
                true,
                currentVersion,
                latestVersion,
                updateAvailable,
                updateAvailable ? "Eine neue Version ist verfügbar." : "Die Anwendung ist aktuell.",
                OffsetDateTime.now());
    }

    public static ApplicationUpdateStatus error(String currentVersion, String message) {
        return new ApplicationUpdateStatus(
                true,
                currentVersion,
                "unbekannt",
                false,
                message,
                OffsetDateTime.now());
    }
}
