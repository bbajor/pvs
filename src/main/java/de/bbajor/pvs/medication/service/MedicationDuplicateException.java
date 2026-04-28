package de.bbajor.pvs.medication.service;

/**
 * Wird geworfen, wenn ein manuell angelegtes Medikament mit bestehenden
 * eindeutigen Kennungen kollidiert.
 */
public class MedicationDuplicateException extends IllegalStateException {

    public MedicationDuplicateException(String message) {
        super(message);
    }
}
