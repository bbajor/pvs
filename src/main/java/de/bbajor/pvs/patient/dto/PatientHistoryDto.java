package de.bbajor.pvs.patient.dto;

import java.util.List;

import de.bbajor.pvs.patient.model.PatientRecord;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientHistoryDto {

    private Long id;
    private Long version;
    private List<PatientRecord> patientRecords;
}
