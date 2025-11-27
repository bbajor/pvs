package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.SideOfEyeConverter;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Treatment extends BasicEntity<Long> {

    // Tenant isolation is ensured via treatmentPlan.patient.practice.tenant relationship

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_plan_id")
    private TreatmentPlan treatmentPlan;
    @Convert(converter = SideOfEyeConverter.class)
    private SideOfEye sideOfEye;
    @ManyToOne(fetch = FetchType.EAGER)
    private SurgicalCenterTimeSlot surgicalCenterTimeSlot;
    private LocalDate approvalDate;

    // Extended approval metadata
    private LocalDateTime approvalDateTime;
    private String approvedByUserId;
    private String approvedByUserName;
    // Optional second approval (four-eyes principle)
    private LocalDateTime secondApprovalDateTime;
    private String secondApprovedByUserId;
    private String secondApprovedByUserName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medication_favourite_id")
    private MedicationFavourite medicationFavourite;
    private String frequency;
    private String dosage;
    private String billId;
    private String additionalInfo;
    
    /**
     * Indicates whether the patient appeared for this treatment appointment.
     * If false, this treatment should not be counted in "actual treatments" statistics.
     * Used to distinguish between planned and actual treatments.
     * @deprecated Use treatmentStatus instead. Kept for backward compatibility.
     */
    @Deprecated
    private Boolean patientAppeared;
    
    /**
     * Status der Behandlung nach der Überprüfung.
     * Definiert verschiedene Zustände, die nach einer Behandlung auftreten können.
     */
    @Enumerated(EnumType.STRING)
    private TreatmentStatus treatmentStatus;

    /**
     * Treating doctors assigned to this treatment.
     * Allows selection of one or more doctors who will perform the treatment.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "treatment_doctor",
        joinColumns = @JoinColumn(name = "treatment_id"),
        inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    private Set<UserAccount> treatingDoctors = new HashSet<>();
    
    /**
     * Bemerkungen für diese Behandlung.
     * Kann Standardbemerkungen oder benutzerdefinierte Bemerkungen enthalten.
     */
    @OneToMany(mappedBy = "treatment", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE }, orphanRemoval = true)
    private List<de.bbajor.pvs.taskmanagement.domain.TreatmentRemark> remarks = new ArrayList<>();

    public String getSurgicalCenterString() {
        if (surgicalCenterTimeSlot != null && surgicalCenterTimeSlot.getSurgicalCenter() != null) {
            return surgicalCenterTimeSlot.getSurgicalCenter().getName();
        }
        return "";
    }

    public LocalDate getDate() {
        return surgicalCenterTimeSlot.getDate();
    }

    public String getPatientInfo() {
        return treatmentPlan.getPatient().toString();
    }

    public Medication getMedication() {
        return medicationFavourite != null ? medicationFavourite.getMedication() : null;
    }
}
