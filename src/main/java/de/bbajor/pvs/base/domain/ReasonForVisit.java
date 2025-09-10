package de.bbajor.pvs.base.domain;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "reason_for_visit")
public class ReasonForVisit extends BasicEntity<Integer> {
    
    private Date dateOfVisit;
    private String reason;
    private String description;
    private String additionalInformation;
}
