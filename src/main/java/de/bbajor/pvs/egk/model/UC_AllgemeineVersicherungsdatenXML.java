package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAccessType;

@XmlRootElement(name = "UC_AllgemeineVersicherungsdatenXML", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
@XmlAccessorType(XmlAccessType.FIELD)
public class UC_AllgemeineVersicherungsdatenXML {

    @XmlElement(name = "Versicherter", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Versicherter versicherter;

    public Versicherter getVersicherter() {
        return versicherter;
    }

    public void setVersicherter(Versicherter versicherter) {
        this.versicherter = versicherter;
    }
}

