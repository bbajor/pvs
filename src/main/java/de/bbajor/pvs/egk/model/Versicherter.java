package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Versicherter {

    @XmlElement(name = "Versicherungsschutz", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Versicherungsschutz versicherungsschutz;

    @XmlElement(name = "Zusatzinfos", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private ZusatzInfos zusatzInfos;

    public Versicherungsschutz getVersicherungsschutz() {
        return versicherungsschutz;
    }

    public void setVersicherungsschutz(Versicherungsschutz versicherungsschutz) {
        this.versicherungsschutz = versicherungsschutz;
    }

    public ZusatzInfos getZusatzInfos() {
        return zusatzInfos;
    }

    public void setZusatzInfos(ZusatzInfos zusatzInfos) {
        this.zusatzInfos = zusatzInfos;
    }
}

