package de.bbajor.pvs.analytics.repository;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository für Analytics-Queries.
 * Alle Queries filtern automatisch nach Institution über die Beziehung:
 * Treatment → TreatmentPlan → Patient → Institution
 */
public interface AnalyticsRepository extends JpaRepository<Object, Long> {

    /**
     * Lädt alle Behandlungen mit Zeitslot für eine Institution.
     */
    @Query("""
        SELECT t FROM Treatment t
        JOIN t.treatmentPlan tp
        JOIN tp.patient p
        JOIN p.institution i
        LEFT JOIN FETCH t.surgicalCenterTimeSlot ts
        WHERE i.id = :institutionId
        AND ts.date IS NOT NULL
        ORDER BY ts.date, ts.startTime
        """)
    List<Treatment> findAllTreatmentsWithTimeSlot(@Param("institutionId") Long institutionId);

    /**
     * Lädt alle Behandlungen mit Medikament für eine Institution.
     */
    @Query("""
        SELECT t FROM Treatment t
        JOIN t.treatmentPlan tp
        JOIN tp.patient p
        JOIN p.institution i
        LEFT JOIN FETCH t.medicationFavourite mf
        LEFT JOIN FETCH mf.medication
        LEFT JOIN FETCH t.surgicalCenterTimeSlot ts
        WHERE i.id = :institutionId
        AND ts.date IS NOT NULL
        ORDER BY ts.date
        """)
    List<Treatment> findAllTreatmentsWithMedication(@Param("institutionId") Long institutionId);

    /**
     * Lädt alle Patienten für eine Institution (für Altersgruppen-Berechnung im Service).
     */
    @Query("""
        SELECT p FROM Patient p
        JOIN p.institution i
        WHERE i.id = :institutionId
        """)
    List<Patient> findAllPatientsByInstitution(@Param("institutionId") Long institutionId);
}

