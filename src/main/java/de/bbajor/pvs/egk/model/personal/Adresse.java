package de.bbajor.pvs.egk.model.personal;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Adresse {

    @XmlElement(name = "Postleitzahl", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String postleitzahl;

    @XmlElement(name = "Ort", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String ort;

    @XmlElement(name = "Strasse", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String strasse;

    @XmlElement(name = "Hausnummer", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String hausnummer;

    @XmlElement(name = "Land", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Land land;

    public String getPostleitzahl() {
        return postleitzahl;
    }

    public void setPostleitzahl(String postleitzahl) {
        this.postleitzahl = postleitzahl;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getHausnummer() {
        return hausnummer;
    }

    public void setHausnummer(String hausnummer) {
        this.hausnummer = hausnummer;
    }

    public Land getLand() {
        return land;
    }

    public void setLand(Land land) {
        this.land = land;
    }
}
