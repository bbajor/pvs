package de.bbajor.pvs.function.patient;

import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Response for patient functions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientResponse extends FunctionResponse {
    private Patient patient;
    private List<Patient> patients;
}


