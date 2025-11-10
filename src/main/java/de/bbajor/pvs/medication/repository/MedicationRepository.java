package de.bbajor.pvs.medication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.medication.model.Medication;

public interface MedicationRepository
        extends JpaRepository<Medication, Long>, JpaSpecificationExecutor<Medication> {

    Slice<Medication> findAllBy(Pageable pageable);

    Optional<Medication> findFirstByZulassungsNrAndValidUntilIsNull(String zulassungsNr);

    Optional<Medication> findFirstByEingangsnummerAndValidUntilIsNull(String eingangsnummer);
}
