package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZusatzInfosAbrechnungGKV {

    @XmlElement(name = "WOP", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String wop;

    public String getWop() {
        return wop;
    }

    public void setWop(String wop) {
        this.wop = wop;
    }
}
