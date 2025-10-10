package de.bbajor.pvs.patient.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.patient.dto.Salutation;
import de.bbajor.pvs.patient.dto.Title;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "patient", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "first_name", "last_name", "birth" }),
        @UniqueConstraint(columnNames = { "insurance_number" })
})
public class Patient extends BasicEntity<Integer> {

    private Salutation salutation;
    private Title title;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "birth", nullable = false)
    private LocalDate birth;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private Address address;
    private String gender;
    private String phone;
    @Email
    private String email;
    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private HealthInsurance healthInsurance;
    @Column(name = "insurance_number", unique = true, nullable = true)
    private String insuranceNumber;
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private PatientHistory patientHistory;
    private String description;

    public String getPatientInfo() {
        return String.format("%s %s, geb. %s, %s",
                firstName, lastName, birth, healthInsurance);
    }

}