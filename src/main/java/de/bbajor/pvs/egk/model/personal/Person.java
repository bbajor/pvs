package de.bbajor.pvs.egk.model.personal;

import java.time.LocalDate;

import de.bbajor.pvs.base.misc.LocalDateAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Person {

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlElement(name = "Geburtsdatum", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private LocalDate geburtsdatum;

    @XmlElement(name = "Vorname", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String vorname;

    @XmlElement(name = "Nachname", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String nachname;

    @XmlElement(name = "Geschlecht", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String geschlecht;

    @XmlElement(name = "StrassenAdresse", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Adresse strassenAdresse;

    public LocalDate getGeburtsdatum() {
        return geburtsdatum;
    }

    public void setGeburtsdatum(LocalDate geburtsdatum) {
        this.geburtsdatum = geburtsdatum;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getGeschlecht() {
        return geschlecht;
    }

    public void setGeschlecht(String geschlecht) {
        this.geschlecht = geschlecht;
    }

    public Adresse getStrassenAdresse() {
        return strassenAdresse;
    }

    public void setStrassenAdresse(Adresse strassenAdresse) {
        this.strassenAdresse = strassenAdresse;
    }
}
