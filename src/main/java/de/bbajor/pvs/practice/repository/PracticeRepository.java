package de.bbajor.pvs.practice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.bbajor.pvs.practice.model.Practice;

public interface PracticeRepository extends JpaRepository<Practice, Long> {
    
    /**
     * Finds the first (and only) practice in the system.
     * Since the system should only have one practice, this retrieves it.
     */
    Optional<Practice> findFirstByOrderByIdAsc();
}


