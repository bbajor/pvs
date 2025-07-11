package de.bbajor.pvs.base.domain;

import java.sql.Date;

public class PatientRecord {

    private Date dateOfRecord;
    private String description;
    private boolean isActive;
    private ReasonForVisit reasonForVisit;
    private Anamnesis patientAnamnesis;
}