package de.bbajor.pvs.patient.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reason_for_visit")
public class ReasonForVisit extends BasicEntity<Integer> {

    private LocalDate dateOfVisit;
    private String reason;
    private String description;
    private String additionalInformation;
}
