package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.institution.repository.InstitutionAwareRepository;

public interface TreatmentPlanRepository
                extends InstitutionAwareRepository<TreatmentPlan, Long>, JpaSpecificationExecutor<TreatmentPlan> {

        /**
         * Find all treatment plans for a institution with pagination.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Query("SELECT tp FROM TreatmentPlan tp WHERE " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        Slice<TreatmentPlan> findAllByInstitutionId(@Param("institutionId") Long institutionId, Pageable pageable);

        /**
         * Find treatment plans by patient ID within institution.
         * IMPORTANT: Ensures cross-tenant access is prevented.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Query("SELECT tp FROM TreatmentPlan tp WHERE " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId " +
               "AND tp.patient.id = :patientId")
        List<TreatmentPlan> findByInstitutionAndPatientId(
                @Param("institutionId") Long institutionId,
                @Param("patientId") Integer patientId);

        /**
         * Find all treatment plans with patient and diagnosis for institution.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Query("""
                SELECT DISTINCT tp FROM TreatmentPlan tp
                LEFT JOIN FETCH tp.patient p
                LEFT JOIN FETCH p.address a
                LEFT JOIN FETCH tp.diagnosis d
                WHERE p.location IS NOT NULL AND p.location.institution.id = :institutionId
                """)
        List<TreatmentPlan> findAllTreatmentPlansWithPatientDiagnosisForInstitution(@Param("institutionId") Long institutionId);

        /**
         * Find treatment plan by ID with patient and diagnosis, ensuring institution access.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Query("""
                SELECT DISTINCT tp FROM TreatmentPlan tp
                LEFT JOIN FETCH tp.patient p
                LEFT JOIN FETCH p.address a
                LEFT JOIN FETCH tp.diagnosis d
                LEFT JOIN FETCH p.location loc
                LEFT JOIN FETCH loc.institution inst
                WHERE tp.id = :id 
AND loc IS NOT NULL AND inst.id = :institutionId
                """)
        Optional<TreatmentPlan> findTreatmentPlanByIdAndInstitutionWithPatientDiagnosis(
                @Param("id") Long id,
                @Param("institutionId") Long institutionId);
        
        /**
         * Find all treatment plans for a institution.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Override
        @Query("SELECT tp FROM TreatmentPlan tp WHERE " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        List<TreatmentPlan> findByInstitutionId(@Param("institutionId") Long institutionId);
        
        /**
         * Find treatment plan by ID and institution (institution-safe access).
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Override
        @Query("SELECT tp FROM TreatmentPlan tp WHERE tp.id = :id AND " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        Optional<TreatmentPlan> findByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
        
        /**
         * Count treatment plans for a institution.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Override
        @Query("SELECT COUNT(tp) FROM TreatmentPlan tp WHERE " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        long countByInstitutionId(@Param("institutionId") Long institutionId);
        
        /**
         * Check if treatment plan exists for institution.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Override
        @Query("SELECT CASE WHEN COUNT(tp) > 0 THEN true ELSE false END FROM TreatmentPlan tp WHERE tp.id = :id AND " +
                     "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        boolean existsByIdAndInstitutionId(@Param("id") Long id, @Param("institutionId") Long institutionId);
        
        /**
         * Delete all treatment plans for a institution.
         * USE WITH CAUTION - for institution deletion/cleanup only.
         * <p>
         * Data isolation: All filtering is done via institution.
         * TreatmentPlan → Patient → Location → Institution (primary path).
         * </p>
         */
        @Modifying
        @Query("DELETE FROM TreatmentPlan tp WHERE " +
               "tp.patient.location IS NOT NULL AND tp.patient.location.institution.id = :institutionId")
        void deleteByInstitutionId(@Param("institutionId") Long institutionId);
}
