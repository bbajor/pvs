package de.bbajor.pvs.icd10.domain;

import java.sql.Date;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
public class IcdVersion {
 
    private int id;
    private String version;
    private String description;
    private Date validFrom;
    private Date validTo;
}
