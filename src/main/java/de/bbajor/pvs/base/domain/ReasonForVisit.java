package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "reason_for_visit")
public class ReasonForVisit {
    
    private int id;
    private String reason;
    private String description;
}
