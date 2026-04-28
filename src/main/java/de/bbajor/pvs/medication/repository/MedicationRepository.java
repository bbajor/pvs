package de.bbajor.pvs.medication.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.bbajor.pvs.medication.model.Medication;

public interface MedicationRepository
        extends JpaRepository<Medication, Long>, JpaSpecificationExecutor<Medication> {

    Slice<Medication> findAllBy(Pageable pageable);

    Optional<Medication> findFirstByZulassungsNrAndValidUntilIsNull(String zulassungsNr);

    Optional<Medication> findFirstByEingangsnummerAndValidUntilIsNull(String eingangsnummer);

    @Query("SELECT COUNT(m) > 0 FROM Medication m WHERE m.validUntil IS NULL AND LOWER(m.eingangsnummer) = LOWER(:eingangsnummer)")
    boolean existsActiveByEingangsnummerIgnoreCase(@Param("eingangsnummer") String eingangsnummer);

    @Query("SELECT COUNT(m) > 0 FROM Medication m WHERE m.validUntil IS NULL AND LOWER(m.zulassungsNr) = LOWER(:zulassungsNr)")
    boolean existsActiveByZulassungsNrIgnoreCase(@Param("zulassungsNr") String zulassungsNr);

    @Query("SELECT COUNT(m) > 0 FROM Medication m WHERE m.validUntil IS NULL AND LOWER(m.euVerfahrensnummer) = LOWER(:eu)")
    boolean existsActiveByEuVerfahrensnummerIgnoreCase(@Param("eu") String euVerfahrensnummer);

    /**
     * Native SQL: Hibernate-JPQL erlaubt kein TRIM() auf {@code @Lob}-Feldern (CLOB-Mapping).
     * PostgreSQL TEXT unterstützt TRIM hier ohne weiteres.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM medication m
                WHERE m.valid_until IS NULL
                AND LOWER(TRIM(COALESCE(m.arzneimittelbezeichnung, ''))) = LOWER(TRIM(:bez))
                AND LOWER(TRIM(COALESCE(m.wirkstoffe, ''))) = LOWER(TRIM(:wirkstoffe))
            )
            """, nativeQuery = true)
    boolean existsActiveByBezeichnungAndWirkstoffIgnoreCase(
            @Param("bez") String arzneimittelbezeichnung,
            @Param("wirkstoffe") String wirkstoffe);
}
