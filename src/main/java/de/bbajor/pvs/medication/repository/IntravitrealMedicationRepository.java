package de.bbajor.pvs.medication.repository;

import java.util.Collection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.medication.model.IntravitrealMedication;

public interface IntravitrealMedicationRepository
        extends JpaRepository<IntravitrealMedication, Long>, JpaSpecificationExecutor<IntravitrealMedication> {

    Slice<IntravitrealMedication> findAllBy(Pageable pageable);

    Collection<IntravitrealMedication> findAllByIsFavouriteTrue();
}
