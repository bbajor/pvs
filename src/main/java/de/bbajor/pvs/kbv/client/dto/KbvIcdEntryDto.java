package de.bbajor.pvs.kbv.client.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class KbvIcdEntryDto {
    private Long id;
    private String code;
    private String textContent;
    private Integer codingType;
    private Integer printIndicator;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String quarter;
    private String version;
}
