package de.bbajor.pvs.base.domain;

import java.time.LocalDate;

import jakarta.persistence.CascadeType;
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

    private String salutation;
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate birth;
    @ManyToOne(cascade = CascadeType.ALL)
    private Address address;
    private String phone;
    @Email
    private String email;
    @ManyToOne
    private HealthInsurance healthInsurance;
    private String insuranceId;
    @OneToOne
    private PatientHistory patientHistory;

}