package de.bbajor.pvs.ai.domain;

import java.time.LocalDateTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "ai_usage_log")
public class AiUsageLog extends BasicEntity<Long> {

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String provider; // "local-whisper", "aleph-alpha", etc.

    @Column(nullable = false)
    private String requestType; // "transcription", "extraction", etc.

    @Column
    private Long tokenCount;

    @Column
    private String status; // "success", "error"

    @Column(length = 1000)
    private String errorMessage;

}

