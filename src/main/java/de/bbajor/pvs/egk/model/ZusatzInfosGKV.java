package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZusatzInfosGKV {

    @XmlElement(name = "Versichertenart", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String versichertenart;

    @XmlElement(name = "Zusatzinfos_Abrechnung_GKV", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private ZusatzInfosAbrechnungGKV abrechnung;

    public String getVersichertenart() {
        return versichertenart;
    }

    public void setVersichertenart(String versichertenart) {
        this.versichertenart = versichertenart;
    }

    public ZusatzInfosAbrechnungGKV getAbrechnung() {
        return abrechnung;
    }

    public void setAbrechnung(ZusatzInfosAbrechnungGKV abrechnung) {
        this.abrechnung = abrechnung;
    }
}

