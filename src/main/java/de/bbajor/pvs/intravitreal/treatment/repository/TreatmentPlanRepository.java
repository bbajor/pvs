package de.bbajor.pvs.intravitreal.treatment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;

public interface TreatmentPlanRepository
                extends JpaRepository<TreatmentPlan, Long>, JpaSpecificationExecutor<TreatmentPlan> {

        Slice<TreatmentPlan> findAllBy(Pageable pageable);

        List<TreatmentPlan> findByPatientId(Integer patientId);

        @Query("""
                            SELECT DISTINCT tp FROM TreatmentPlan tp
                            LEFT JOIN FETCH tp.patient p
                            LEFT JOIN FETCH p.address a
                            LEFT JOIN FETCH tp.diagnosis d
                        """)
        List<TreatmentPlan> findAllTreatmentPlansWithPatientDiagnosis();

        @Query("""
                            SELECT DISTINCT tp FROM TreatmentPlan tp
                            LEFT JOIN FETCH tp.patient p
                            LEFT JOIN FETCH p.address a
                            LEFT JOIN FETCH tp.diagnosis d
                            WHERE tp.id = :id
                        """)
        Optional<TreatmentPlan> findTreatmentPlanByIdWithPatientDiagnosis(@Param("id") Long id);
}
