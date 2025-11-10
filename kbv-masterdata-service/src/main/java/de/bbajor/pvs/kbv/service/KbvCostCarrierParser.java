package de.bbajor.pvs.kbv.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;

/**
 * Parser for Kostenträgerstammdatei XML files from KBV.
 */
@Component
public class KbvCostCarrierParser {

    private static final Logger log = LoggerFactory.getLogger(KbvCostCarrierParser.class);

    public List<KbvCostCarrier> parse(InputStream xmlStream, String quarter, String version) {
        List<KbvCostCarrier> carriers = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlStream);
            doc.getDocumentElement().normalize();

            NodeList entryNodes = findEntryNodes(doc);
            LocalDate validFrom = new KbvHistoricizationService(null, null, null).calculateValidFrom(quarter);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Node node = entryNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    KbvCostCarrier carrier = parseEntry((Element) node, quarter, version, validFrom);
                    if (carrier != null) {
                        carriers.add(carrier);
                    }
                }
            }

            log.info("Parsed {} cost carriers from XML", carriers.size());
        } catch (Exception e) {
            log.error("Error parsing cost carrier XML", e);
            throw new RuntimeException("Failed to parse cost carrier XML", e);
        }
        return carriers;
    }

    private NodeList findEntryNodes(Document doc) {
        NodeList entries = doc.getElementsByTagName("entry");
        if (entries.getLength() > 0) return entries;

        entries = doc.getElementsByTagName("cost_carrier");
        if (entries.getLength() > 0) return entries;

        entries = doc.getElementsByTagName("kostentraeger");
        if (entries.getLength() > 0) return entries;

        entries = doc.getElementsByTagName("carrier");
        if (entries.getLength() > 0) return entries;

        Element root = doc.getDocumentElement();
        return root.getChildNodes();
    }

    private KbvCostCarrier parseEntry(Element element, String quarter, String version, LocalDate validFrom) {
        try {
            KbvCostCarrier carrier = new KbvCostCarrier();
            carrier.setQuarter(quarter);
            carrier.setVersion(version);
            carrier.setValidFrom(validFrom);

            String code = getElementText(element, "code", "id", "identifier", "kennung");
            if (code == null || code.isBlank()) {
                log.warn("Skipping cost carrier without code");
                return null;
            }
            carrier.setCode(code.trim());

            String name = getElementText(element, "name", "text", "bezeichnung", "label", "text_content");
            if (name == null || name.isBlank()) {
                name = code; // Fallback
            }
            carrier.setName(name.trim());

            return carrier;
        } catch (Exception e) {
            log.warn("Error parsing cost carrier element", e);
            return null;
        }
    }

    private String getElementText(Element parent, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList nodes = parent.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    return node.getTextContent();
                }
            }
            if (parent.hasAttribute(tagName)) {
                return parent.getAttribute(tagName);
            }
        }
        return null;
    }
}
