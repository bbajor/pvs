package de.bbajor.pvs.base.domain;

import java.sql.Date;

import org.jspecify.annotations.Nullable;

import com.helger.commons.email.EmailAddress;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import lombok.Data;

@Data
@Entity
public class Patient{

    private int id;
    private Salutation salutation;
    private Title title;
    private String firstName;
    private String lastName;
    private Date birth;
    private PatientAddress PatientAddress;
    private String phone;
    private EmailAddress email;
    private HealthInsuranceCard healthInsuranceCard;
    private PatientHistory patientHistory;

}