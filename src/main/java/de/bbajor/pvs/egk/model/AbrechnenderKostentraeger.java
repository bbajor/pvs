package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class AbrechnenderKostentraeger {

    @XmlElement(name = "Kostentraegerkennung", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String kostentraegerkennung;

    @XmlElement(name = "Kostentraegerlaendercode", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String laendercode;

    @XmlElement(name = "Name", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String name;

    public String getKostentraegerkennung() {
        return kostentraegerkennung;
    }

    public void setKostentraegerkennung(String kostentraegerkennung) {
        this.kostentraegerkennung = kostentraegerkennung;
    }

    public String getLaendercode() {
        return laendercode;
    }

    public void setLaendercode(String laendercode) {
        this.laendercode = laendercode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

