package de.bbajor.pvs.patient.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Address extends BasicEntity<Long> {

    private String street;
    private String houseNo;
    private Integer postalCode;
    private String city;
    private String country;

}
