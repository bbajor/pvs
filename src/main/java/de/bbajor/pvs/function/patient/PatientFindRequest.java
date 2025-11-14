package de.bbajor.pvs.function.patient;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request to find a patient by ID.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientFindRequest extends PatientFunctionRequest {
    private Integer patientId;
}


