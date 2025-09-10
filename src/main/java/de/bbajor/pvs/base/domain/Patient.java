package de.bbajor.pvs.base.domain;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Patient extends BasicEntity<Integer> {

    @ManyToOne
    private Salutation salutation;
    private Title title;
    private String firstName;
    private String lastName;
    private Date birth;
    @ManyToOne
    private PatientAddress PatientAddress;
    private String phone;
    @Email
    private String email;
    @ManyToOne
    private HealthInsuranceCard healthInsuranceCard;
    @OneToOne
    private PatientHistory patientHistory;

}