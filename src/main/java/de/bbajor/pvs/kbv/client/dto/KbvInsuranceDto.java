package de.bbajor.pvs.kbv.client.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class KbvInsuranceDto {
    private Long id;
    private String code;
    private String name;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String quarter;
    private String version;
}
