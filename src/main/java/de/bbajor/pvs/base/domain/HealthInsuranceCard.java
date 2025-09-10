package de.bbajor.pvs.base.domain;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class HealthInsuranceCard extends BasicEntity<Integer> {

    @ManyToOne
    private HealthInsurance healthInsurance;
    private String socialInsuranceNumber;
    private String lastName;
    private String firstName;
    private Date dateOfBirth;
    private String personalIdentifier;
    private String cardNumber;
    private String expirationDate;

}
