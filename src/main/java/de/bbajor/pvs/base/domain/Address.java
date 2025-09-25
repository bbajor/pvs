package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class Address extends BasicEntity<Long> {

    private String street;
    private String houseNumber;
    private Double postalCode;
    private String city;
    private String country;

}
