package de.bbajor.pvs.intravitreal.treatment.model;

/**
 * Status einer Behandlung nach der Überprüfung.
 * Definiert verschiedene Zustände, die nach einer Behandlung auftreten können.
 */
public enum TreatmentStatus {
    /**
     * Patient ist erschienen und wurde erfolgreich behandelt.
     * Grün - nächstes Intervall vorauswählen.
     */
    PATIENT_APPEARED_SUCCESSFUL(
        "Erfolgreich behandelt",
        "Patient ist erschienen und wurde erfolgreich behandelt",
        StatusColor.GREEN
    ),
    
    /**
     * Patient ist erschienen, wurde behandelt, muss jedoch erneut behandelt werden.
     * Rot - nächstmöglicher Termin.
     */
    PATIENT_APPEARED_NEEDS_RETREATMENT(
        "Erneute Behandlung nötig",
        "Patient ist erschienen, wurde behandelt, muss jedoch erneut behandelt werden",
        StatusColor.RED
    ),
    
    /**
     * Patient erschienen, Behandlung nicht durchgeführt.
     * Gelb - nächstmöglicher Termin.
     */
    PATIENT_APPEARED_NO_TREATMENT(
        "Behandlung nicht durchgeführt",
        "Patient erschienen, Behandlung nicht durchgeführt",
        StatusColor.YELLOW
    ),
    
    /**
     * Patient hat abgesagt.
     * Gelb - nächstmöglicher Termin.
     */
    PATIENT_CANCELLED(
        "Abgesagt",
        "Patient hat abgesagt",
        StatusColor.YELLOW
    ),
    
    /**
     * Patient ist nicht erschienen und hat nicht abgesagt.
     * Rot - nächstmöglicher Termin.
     */
    PATIENT_NO_SHOW(
        "Nicht erschienen",
        "Patient ist nicht erschienen und hat nicht abgesagt",
        StatusColor.RED
    ),
    
    /**
     * Patient hat fristgerecht abgesagt.
     * Gelb - nächstmöglicher Termin.
     */
    PATIENT_CANCELLED_ON_TIME(
        "Fristgerecht abgesagt",
        "Patient hat fristgerecht abgesagt",
        StatusColor.YELLOW
    );
    
    /**
     * Farbe für die Ampel-Darstellung.
     */
    public enum StatusColor {
        GREEN, YELLOW, RED
    }
    
    private final String shortLabel;
    private final String fullDescription;
    private final StatusColor color;
    
    TreatmentStatus(String shortLabel, String fullDescription, StatusColor color) {
        this.shortLabel = shortLabel;
        this.fullDescription = fullDescription;
        this.color = color;
    }
    
    /**
     * Kurze Bezeichnung für die Combobox.
     */
    public String getShortLabel() {
        return shortLabel;
    }
    
    /**
     * Ausführliche Beschreibung für Tooltip.
     */
    public String getFullDescription() {
        return fullDescription;
    }
    
    /**
     * Farbe für die Ampel-Darstellung.
     */
    public StatusColor getColor() {
        return color;
    }
    
    /**
     * Prüft, ob für diesen Status das normale Intervall verwendet werden soll.
     * Nur bei erfolgreicher Behandlung (grün).
     */
    public boolean shouldUseNormalInterval() {
        return color == StatusColor.GREEN;
    }
}

