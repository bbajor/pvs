package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PatientAddress extends BasicEntity<Integer>{

    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String country;

}
