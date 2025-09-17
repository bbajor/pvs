package de.bbajor.pvs.egk.model.personal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class VersicherterPersoenlich {

    @XmlElement(name = "Versicherten_ID", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private String versichertenId;

    @XmlElement(name = "Person", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private Person person;

    public String getVersichertenId() {
        return versichertenId;
    }

    public void setVersichertenId(String versichertenId) {
        this.versichertenId = versichertenId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
