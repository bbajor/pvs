package de.bbajor.pvs.function.patient;

import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base request for patient functions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientFunctionRequest extends FunctionRequest {
    // Institution ID is inherited from FunctionRequest
}


