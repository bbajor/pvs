package de.bbajor.pvs.patientsearch.presenter;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

import de.bbajor.pvs.patientsearch.dto.PatientAddressDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

public class EgkXmlMapper {

    public static PatientDto map(String xml) throws Exception {
        PatientDto patient = new PatientDto();

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes("ISO-8859-15")));
        doc.getDocumentElement().normalize();

        Node versicherterNode = doc.getElementsByTagName("Versicherter").item(0);
        if (versicherterNode.getNodeType() == Node.ELEMENT_NODE) {
            Element versicherter = (Element) versicherterNode;

            Element person = (Element) versicherter.getElementsByTagName("Person").item(0);
            patient.setFirstName(getTextContent(person, "Vorname"));
            patient.setLastName(getTextContent(person, "Nachname"));
            patient.setBirth(LocalDate.parse(getTextContent(person, "Geburtsdatum"), java.time.format.DateTimeFormatter.BASIC_ISO_DATE));

            Element address = (Element) person.getElementsByTagName("StrassenAdresse").item(0);
            PatientAddressDto addrDto = new PatientAddressDto();
            addrDto.setStreet(getTextContent(address, "Strasse"));
            addrDto.setHouseNumber(getTextContent(address, "Hausnummer"));
            addrDto.setPostalCode(getTextContent(address, "Postleitzahl"));
            addrDto.setCity(getTextContent(address, "Ort"));
            addrDto.setCountry(getTextContent((Element) address.getElementsByTagName("Land").item(0), "Wohnsitzlaendercode"));
            
            patient.setHealthInsuranceNumber(getTextContent(versicherter, "Versicherten_ID"));
            patient.setPatientAddress(addrDto);
        }

        return patient;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() > 0) return nl.item(0).getTextContent();
        return null;
    }
}
