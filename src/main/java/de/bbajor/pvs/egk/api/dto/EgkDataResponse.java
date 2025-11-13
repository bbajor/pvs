package de.bbajor.pvs.egk.api.dto;

import de.bbajor.pvs.patient.model.Patient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response-DTO für eGK-Daten-Verarbeitung.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EgkDataResponse {
    private Patient patient;
    private String error;
}
