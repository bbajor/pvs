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

    Slice<Task> findAllBy(Pageable pageable);

    Slice<Task> findAllByCompleted(boolean completed, Pageable pageable);

    @Query("""
            SELECT t FROM Task t
            WHERE t.timeSlot.date <= :now
            AND EXISTS (
                SELECT 1 FROM Treatment tr
                WHERE tr.surgicalCenterTimeSlot = t.timeSlot
                AND tr.approvalDate is NULL
            )
            """)
    List<Task> getTasksWhereExistsNotApprovedTreatment(@Param("now") LocalDate now);
}
