package de.bbajor.pvs.egk.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZusatzInfos {

    @XmlElement(name = "ZusatzinfosGKV", namespace = "http://ws.gematik.de/fa/vsdm/vsd/v5.2")
    private ZusatzInfosGKV gkv;

    public ZusatzInfosGKV getGkv() {
        return gkv;
    }

    public void setGkv(ZusatzInfosGKV gkv) {
        this.gkv = gkv;
    }
}
