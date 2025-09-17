package de.bbajor.pvs.egk.model.personal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "UC_PersoenlicheVersichertendatenXML", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
@XmlAccessorType(XmlAccessType.FIELD)
public class UcPersoenlicheVersichertenDatenXml {

    @XmlElement(name = "Versicherter", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private VersicherterPersoenlich versicherter;

    public VersicherterPersoenlich getVersicherter() {
        return versicherter;
    }

    public void setVersicherter(VersicherterPersoenlich versicherter) {
        this.versicherter = versicherter;
    }
}
