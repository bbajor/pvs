package de.bbajor.pvs.taskmanagement.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    /**
     * Find all tasks for the current institution.
     * Filters by institution via Task → SurgicalCenterTimeSlot → SurgicalCenter → Institution.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.timeSlot IS NOT NULL
            AND t.timeSlot.surgicalCenter IS NOT NULL
            AND t.timeSlot.surgicalCenter.institution.id = :institutionId
            """)
    Slice<Task> findAllByInstitutionId(@Param("institutionId") Long institutionId, Pageable pageable);

    /**
     * Find all tasks by completion status for the current institution.
     * Filters by institution via Task → SurgicalCenterTimeSlot → SurgicalCenter → Institution.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.timeSlot IS NOT NULL
            AND t.timeSlot.surgicalCenter IS NOT NULL
            AND t.timeSlot.surgicalCenter.institution.id = :institutionId
            AND t.completed = :completed
            """)
    Slice<Task> findAllByInstitutionIdAndCompleted(@Param("institutionId") Long institutionId, 
            @Param("completed") boolean completed, Pageable pageable);

    /**
     * Find tasks with unapproved treatments for the current institution.
     * Filters by institution via Task → SurgicalCenterTimeSlot → SurgicalCenter → Institution.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.timeSlot IS NOT NULL
            AND t.timeSlot.surgicalCenter IS NOT NULL
            AND t.timeSlot.surgicalCenter.institution.id = :institutionId
            AND t.timeSlot.date <= :now
            AND EXISTS (
                SELECT 1 FROM Treatment tr
                WHERE tr.surgicalCenterTimeSlot = t.timeSlot
                AND tr.approvalDate is NULL
            )
            """)
    List<Task> getTasksWhereExistsNotApprovedTreatment(@Param("institutionId") Long institutionId, 
            @Param("now") LocalDate now);
    
    /**
     * Find a task by time slot ID.
     * Used to check if a task already exists for a time slot before creating a new one.
     */
    @Query("SELECT t FROM Task t WHERE t.timeSlot.id = :timeSlotId")
    java.util.Optional<Task> findByTimeSlotId(@Param("timeSlotId") Long timeSlotId);
    
    // Legacy methods without institution filter (for backward compatibility)
    // These should not be used in production code
    @Deprecated
    Slice<Task> findAllBy(Pageable pageable);

    @Deprecated
    Slice<Task> findAllByCompleted(boolean completed, Pageable pageable);
}
