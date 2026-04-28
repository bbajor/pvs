package de.bbajor.pvs.medication.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class Medication extends BasicEntity<Long> {

    // Tenant isolation: Null for system-wide medications, otherwise via treatment.patient.practice.tenant
    // Note: Field order matches database column order to avoid Hibernate mapping issues

    @Column(name = "eingangsnummer", length = 50)
    private String eingangsnummer;

    @Column(name = "arzneimittelbezeichnung", length = 500)
    private String arzneimittelbezeichnung;

    @Column(name = "darreichungsform", length = 500)
    private String darreichungsform;

    @Column(name = "zielgruppe", length = 100)
    private String zielgruppe;

    @Column(name = "anwendungsart", length = 200)
    private String anwendungsart;

    @Lob
    @Column(name = "anwendungsgebiete", columnDefinition = "TEXT")
    private String anwendungsgebiete;

    @Lob
    @Column(name = "indikation_atc", columnDefinition = "TEXT")
    private String indikationAtc;

    @Column(name = "bescheiddatum_zulassung", length = 50)
    private String BescheiddatumZulassung;

    @Column(name = "zulassungsstatus", length = 100)
    private String zulassungsstatus;

    @Column(name = "zulassungs_nr", length = 100)
    private String zulassungsNr;

    @Column(name = "verkehrsfaehigkeit", length = 50)
    private String verkehrsfaehigkeit;

    @Column(name = "zulassungs_reg_nr_oder_kennziffer", length = 500)
    private String zulassungsRegNrOderKennziffer;

    @Lob
    @Column(name = "parallelimportinformationen", columnDefinition = "TEXT")
    private String parallelimportinformationen;

    @Column(name = "eu_verfahrensnummer", length = 100)
    private String euVerfahrensnummer;

    @Column(name = "zulassungsinhaber", length = 500)
    private String zulassungsinhaber;

    @Column(name = "hersteller_endfreigabe", length = 500)
    private String herstellerEndfreigabe;

    @Column(name = "vertreiber", length = 500)
    private String vertreiber;

    @Column(name = "oertlicher_vertreter", length = 500)
    private String oertlicherVertreter;

    @Lob
    @Column(name = "wirkstoffe", columnDefinition = "TEXT")
    private String wirkstoffe;

    @Lob
    @Column(name = "packungsgroessen_gruppe", columnDefinition = "TEXT")
    private String packungsgroessenGruppe;

    @Lob
    @Column(name = "am_klassifikationen", columnDefinition = "TEXT")
    private String amKlassifikationen;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "valid_from")
    private LocalDate validFrom;
    
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "additional_notes", length = 1000)
    private String additionalNotes;
}
