package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class HealthInsurance extends BasicEntity<Integer> {

    private String name;
    private String healthInsuranceNumber;
    private String countryCode;
    private String insuranceType;
    
}