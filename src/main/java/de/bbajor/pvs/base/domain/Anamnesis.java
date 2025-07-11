package de.bbajor.pvs.base.domain;

import java.util.List;

import de.bbajor.pvs.icd10.domain.IcdEntry;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class Anamnesis {
    private int id;
    private List<IcdEntry> knownDiseases;
    private String additionalInformation;
}
