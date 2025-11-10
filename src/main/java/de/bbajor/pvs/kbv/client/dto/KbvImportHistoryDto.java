package de.bbajor.pvs.kbv.client.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class KbvImportHistoryDto {
    private Long id;
    private String quarter;
    private String version;
    private String importType;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer recordsImported;
    private String errorMessage;
}
