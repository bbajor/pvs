package de.bbajor.pvs.function.patient;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request to search for patients by name.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientSearchRequest extends PatientFunctionRequest {
    private String searchTerm;
}


