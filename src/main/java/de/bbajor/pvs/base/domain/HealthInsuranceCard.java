package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class HealthInsuranceCard {

    private int id;
    private HealthInsurance healthInsurance;
    private String socialInsuranceNumber;
    private String lastName;
    private String firstName;
    private Date dateOfBirth;
    private String personalIdentifier;
    private String cardNumber;
    private String expirationDate;

}
