package de.bbajor.pvs.cost.model;

/**
 * Preismodell für OP-Saal-Kostenberechnung.
 */
public enum PricingModel {
    /**
     * Miete: Fixpreis pro Zeitslot oder pro Stunde
     */
    RENTAL,
    
    /**
     * Eigener OP-Saal: Laufende Kosten (monatlich) + variable Kosten
     */
    OWNED
}

