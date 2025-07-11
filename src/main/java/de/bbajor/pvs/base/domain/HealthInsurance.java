package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class HealthInsurance {

    private int id;
    private String name;
    private long healthInsuranceNumber;
    
}