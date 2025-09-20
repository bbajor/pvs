package de.bbajor.pvs.egk.model;

import java.time.LocalDate;

import de.bbajor.pvs.base.misc.LocalDateAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Versicherungsschutz {

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    @XmlElement(name = "Beginn", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private LocalDate beginn;

    @XmlElement(name = "Kostentraeger", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Kostentraeger kostentraeger;

    public LocalDate getBeginn() {
        return beginn;
    }

    public void setBeginn(LocalDate beginn) {
        this.beginn = beginn;
    }

    public Kostentraeger getKostentraeger() {
        return kostentraeger;
    }

    public void setKostentraeger(Kostentraeger kostentraeger) {
        this.kostentraeger = kostentraeger;
    }
}
