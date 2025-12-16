package de.bbajor.pvs.intravitreal.treatment.model;

import java.time.LocalDateTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class TreatmentAuditLog extends BasicEntity<Long> {

    public enum ActionType {
        CREATE,
        UPDATE,
        APPROVE,
        APPROVE_SECOND,
        DELETE
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @jakarta.persistence.JoinColumn(name = "treatment_id", nullable = true)
    private Treatment treatment; // Nullable für gelöschte Treatments

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private LocalDateTime actionTimestamp;

    @Column(length = 64)
    private String actorUserId;

    @Column(length = 128)
    private String actorUserName;

    @Column(length = 512)
    private String details;
}
