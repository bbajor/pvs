package de.bbajor.pvs.appointment.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.security.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Assignment of a scheduler to a user account or role.
 * This allows flexible assignment of schedulers to specific users or roles.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "scheduler_assignment")
public class SchedulerAssignment extends BasicEntity<Long> {

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private AppointmentScheduler scheduler;

    /**
     * Optional: Assignment to a specific user account (e.g., specific doctor)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private UserAccount userAccount;

    /**
     * Optional: Assignment to a role (e.g., all doctors, all MFAs)
     */
    @Column(length = 50)
    private String role;

    @Override
    public String toString() {
        if (userAccount != null) {
            return String.format("User: %s", userAccount.getFullName());
        } else if (role != null) {
            return String.format("Role: %s", role);
        }
        return "Unassigned";
    }
}
