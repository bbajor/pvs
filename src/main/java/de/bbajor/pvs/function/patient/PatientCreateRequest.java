package de.bbajor.pvs.function.patient;

import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request to create a new patient.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientCreateRequest extends PatientFunctionRequest {
    private Patient patient;
}


