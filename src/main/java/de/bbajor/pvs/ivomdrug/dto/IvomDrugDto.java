package de.bbajor.pvs.ivomdrug.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomDrugDto {

    private long id;
    private String eingangsnummer;
    private String arzneimittelbezeichnung;
    private String darreichungsform;
    private String zielgruppe;
    private String anwendungsart;
    private String anwendungsgebiete;
    private String indikationAtc;
    private String BescheiddatumZulassung;
    private String zulassungsstatus;
    private String zulassungsNr;
    private String verkehrsfaehigkeit;
    private String parallelimportinformationen;
    private String euVerfahrensnummer;
    private String zulassungsinhaber;
    private String herstellerEndfreigabe;
    private String vertreiber;
    private String oertlicherVertreter;
    private String wirkstoffe;
    private String packungsgroessenGruppe;
    private String amKlassifikationen;
    private String description;
    private LocalDate validFrom;
    private LocalDate validUntil;

    @Override
    public String toString() {
        return "Bezeichnung: " + arzneimittelbezeichnung + ", Wirkstoffe: " + wirkstoffe + ", " + "Indikation/ATC: "
                + indikationAtc;
    }
}
