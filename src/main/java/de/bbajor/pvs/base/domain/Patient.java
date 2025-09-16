package de.bbajor.pvs.base.domain;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class Patient extends BasicEntity<Integer> {

    @ManyToOne
    private Salutation salutation;
    private Title title;
    private String firstName;
    private String lastName;
    private LocalDate birth;
    @ManyToOne
    private PatientAddress PatientAddress;
    private String phone;
    @Email
    private String email;
    @ManyToOne
    private HealthInsurance healthInsurance;
    private String healthInsuranceNumber;
    @OneToOne
    private PatientHistory patientHistory;

}