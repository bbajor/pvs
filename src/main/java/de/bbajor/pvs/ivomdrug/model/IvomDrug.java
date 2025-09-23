package de.bbajor.pvs.ivomdrug.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class IvomDrug extends BasicEntity<Long> {

    @Column(length = 50)
    private String eingangsnummer;
    
    @Column(length = 500)
    private String arzneimittelbezeichnung;
    
    @Column(length = 500)
    private String darreichungsform;
    
    @Column(length = 100)
    private String zielgruppe;
    
    @Column(length = 200)
    private String anwendungsart;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String anwendungsgebiete;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String indikationAtc;
    
    @Column(length = 50)
    private String BescheiddatumZulassung;
    
    @Column(length = 100)
    private String zulassungsstatus;
    
    @Column(length = 100)
    private String zulassungsNr;
    
    @Column(length = 50)
    private String verkehrsfaehigkeit;

    @Column(length = 500)
    private String zulassungsRegNrOderKennziffer;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String parallelimportinformationen;
    
    @Column(length = 100)
    private String euVerfahrensnummer;
    
    @Column(length = 500)
    private String zulassungsinhaber;
    
    @Column(length = 500)
    private String herstellerEndfreigabe;
    
    @Column(length = 500)
    private String vertreiber;
    
    @Column(length = 500)
    private String oertlicherVertreter;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String wirkstoffe;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String packungsgroessenGruppe;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String amKlassifikationen;
    
    @Column(length = 1000)
    private String description;

    private boolean isFavourite;
    
    private LocalDate validFrom;
    private LocalDate validUntil;
}
