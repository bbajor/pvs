package de.bbajor.pvs.patientsearch.dto;

import java.util.List;

import de.bbajor.pvs.base.domain.PatientRecord;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PatientHistoryDto {

    private Long id;
    private Long version;
    private List<PatientRecord> patientRecords;
}
