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

import de.bbajor.pvs.kbv.model.KbvIcdEntry;

/**
 * Parser for ICD-10-GM XML files from KBV.
 */
@Component
public class KbvIcdParser {

    private static final Logger log = LoggerFactory.getLogger(KbvIcdParser.class);

    public List<KbvIcdEntry> parse(InputStream xmlStream, String quarter, String version) {
        List<KbvIcdEntry> entries = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlStream);
            doc.getDocumentElement().normalize();

            // KBV XML structure may vary, but typically has entries in a list
            // Common patterns: <entries>, <icd>, <code>, <entry>
            NodeList entryNodes = findEntryNodes(doc);

            LocalDate validFrom = new KbvHistoricizationService(null, null, null).calculateValidFrom(quarter);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Node node = entryNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    KbvIcdEntry entry = parseEntry((Element) node, quarter, version, validFrom);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }

            log.info("Parsed {} ICD entries from XML", entries.size());
        } catch (Exception e) {
            log.error("Error parsing ICD XML", e);
            throw new RuntimeException("Failed to parse ICD XML", e);
        }
        return entries;
    }

    private NodeList findEntryNodes(Document doc) {
        // Try common KBV XML patterns
        NodeList entries = doc.getElementsByTagName("entry");
        if (entries.getLength() > 0) return entries;

        entries = doc.getElementsByTagName("icd");
        if (entries.getLength() > 0) return entries;

        entries = doc.getElementsByTagName("code");
        if (entries.getLength() > 0) return entries;

        // Fallback: get all child elements of root
        Element root = doc.getDocumentElement();
        return root.getChildNodes();
    }

    private KbvIcdEntry parseEntry(Element element, String quarter, String version, LocalDate validFrom) {
        try {
            KbvIcdEntry entry = new KbvIcdEntry();
            entry.setQuarter(quarter);
            entry.setVersion(version);
            entry.setValidFrom(validFrom);

            // Extract code (common field names: code, id, identifier, kode)
            String code = getElementText(element, "code", "id", "identifier", "kode");
            if (code == null || code.isBlank()) {
                log.warn("Skipping entry without code");
                return null;
            }
            entry.setCode(code.trim());

            // Extract text content (common field names: text, text_content, name, bezeichnung)
            String text = getElementText(element, "text", "text_content", "name", "bezeichnung", "label");
            if (text == null || text.isBlank()) {
                text = code; // Fallback
            }
            entry.setTextContent(text.trim());

            // Extract coding type (optional)
            String codingTypeStr = getElementText(element, "coding_type", "type", "typ");
            if (codingTypeStr != null && !codingTypeStr.isBlank()) {
                try {
                    entry.setCodingType(Integer.parseInt(codingTypeStr.trim()));
                } catch (NumberFormatException e) {
                    log.debug("Invalid coding_type: {}", codingTypeStr);
                }
            }

            // Extract print indicator (optional)
            String printIndicatorStr = getElementText(element, "print_indicator", "print", "druck");
            if (printIndicatorStr != null && !printIndicatorStr.isBlank()) {
                try {
                    entry.setPrintIndicator(Integer.parseInt(printIndicatorStr.trim()));
                } catch (NumberFormatException e) {
                    log.debug("Invalid print_indicator: {}", printIndicatorStr);
                }
            }

            return entry;
        } catch (Exception e) {
            log.warn("Error parsing entry element", e);
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
            // Also check attributes
            if (parent.hasAttribute(tagName)) {
                return parent.getAttribute(tagName);
            }
        }
        return null;
    }
}
