package de.bbajor.pvs.egk.model.personal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class Land {

    @XmlElement(name = "Wohnsitzlaendercode", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String wohnsitzlaendercode;

    public String getWohnsitzlaendercode() {
        return wohnsitzlaendercode;
    }

    public void setWohnsitzlaendercode(String wohnsitzlaendercode) {
        this.wohnsitzlaendercode = wohnsitzlaendercode;
    }
}
