package de.bbajor.pvs.function.patient;

import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request to update an existing patient.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientUpdateRequest extends PatientFunctionRequest {
    private Integer patientId;
    private Patient patient;
}


