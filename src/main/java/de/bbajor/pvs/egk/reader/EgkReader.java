package de.bbajor.pvs.egk.reader;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.egk.config.EgkToolProperties;
import de.bbajor.pvs.egk.model.AbrechnenderKostentraeger;
import de.bbajor.pvs.egk.model.Kostentraeger;
import de.bbajor.pvs.egk.model.UC_AllgemeineVersicherungsdatenXML;
import de.bbajor.pvs.egk.model.Versicherter;
import de.bbajor.pvs.egk.model.Versicherungsschutz;
import de.bbajor.pvs.egk.model.ZusatzInfos;
import de.bbajor.pvs.egk.model.ZusatzInfosAbrechnungGKV;
import de.bbajor.pvs.egk.model.personal.Person;
import de.bbajor.pvs.egk.model.personal.StrassenAdresse;
import de.bbajor.pvs.egk.model.personal.UcPersoenlicheVersichertenDatenXml;
import de.bbajor.pvs.egk.model.personal.VersicherterPersoenlich;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientAddressDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;

@Component
public class EgkReader {

    private final EgkToolProperties properties;

    public EgkReader(EgkToolProperties properties) {
        this.properties = properties;
    }

    public PatientDto readPatientFromCard() throws Exception {
        // egk-tool muss im PATH oder Pfad angegeben sein
        ProcessBuilder pb = new ProcessBuilder(properties.getToolPath(), "--pd");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // XML-Ausgabe einlesen
        StringBuilder xmlBuilder = new StringBuilder();
        boolean xmlStarted = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "ISO-8859-15"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Zeile, in der das XML beginnt
                if (line.trim().startsWith("<?xml")) {
                    xmlStarted = true;
                }
                if (xmlStarted) {
                    xmlBuilder.append(line).append("\n");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("egk-tool failed with exit code " + exitCode);
        }

        String xml = xmlBuilder.toString();
        UcPersoenlicheVersichertenDatenXml persoenlicheVersichertenDatenXml = XmlStringReader.fromXmlString(xml,
                UcPersoenlicheVersichertenDatenXml.class);
        return mapToDto(persoenlicheVersichertenDatenXml);
    }

    private static PatientDto mapToDto(UcPersoenlicheVersichertenDatenXml data) {
        VersicherterPersoenlich v = data.getVersicherter();
        Person p = v.getPerson();
        StrassenAdresse adr = p.getStrassenAdresse();

        PatientDto dto = new PatientDto();
        dto.setInsuranceId(v.getVersichertenId());
        dto.setFirstName(p.getVorname());
        dto.setLastName(p.getNachname());
        dto.setBirth(p.getGeburtsdatum());
        dto.setGender(p.getGeschlecht());

        PatientAddressDto addressDto = new PatientAddressDto();
        addressDto.setStreet(adr == null ? "" : adr.getStrasse())
                .setHouseNumber(adr == null ? "" : adr.getHausnummer())
                .setPostalCode(adr == null ? "" : adr.getPostleitzahl())
                .setCity(adr == null ? "" : adr.getOrt())
                .setCountryCode(adr == null || adr.getLand() == null ? "" : adr.getLand().getWohnsitzlaendercode());
        dto.setPatientAddress(addressDto);
        return dto;
    }

    public HealthInsuranceDto readHealthInsuranceFromCard() throws Exception {
        // egk-tool muss im PATH oder Pfad angegeben sein
        ProcessBuilder pb = new ProcessBuilder(properties.getToolPath(), "--vd");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // XML-Ausgabe einlesen
        StringBuilder xmlBuilder = new StringBuilder();
        boolean xmlStarted = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "ISO-8859-15"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Zeile, in der das XML beginnt
                if (line.trim().startsWith("<?xml")) {
                    xmlStarted = true;
                }
                if (xmlStarted) {
                    xmlBuilder.append(line).append("\n");
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("egk-tool failed with exit code " + exitCode);
        }

        String xml = xmlBuilder.toString();
        UC_AllgemeineVersicherungsdatenXML allgemeineVersichertendatenXml = XmlStringReader.fromXmlString(xml,
                UC_AllgemeineVersicherungsdatenXML.class);
        return mapToDto(allgemeineVersichertendatenXml);
    }

    private static HealthInsuranceDto mapToDto(UC_AllgemeineVersicherungsdatenXML data) {

        assert (data != null);

        Versicherter v = data.getVersicherter();
        if (v == null || v.getVersicherungsschutz() == null) {
            return new HealthInsuranceDto();
        }

        HealthInsuranceDto dto = new HealthInsuranceDto();
        Versicherungsschutz vs = v.getVersicherungsschutz();
        dto.setInsuranceStart(vs.getBeginn());

        ZusatzInfos zi = v.getZusatzInfos();
        if (zi != null && zi.getGkv() != null) {
            dto.setInsuranceType(zi.getGkv().getVersichertenart());
            ZusatzInfosAbrechnungGKV zia = zi.getGkv().getAbrechnung();
            if (zia != null) {
                dto.setWop(zi.getGkv().getAbrechnung().getWop());
            }
        }

        Kostentraeger kt = vs.getKostentraeger();
        if (kt != null) {
            dto.setCostCarrierId(kt.getKostentraegerkennung());
            dto.setCostCarrierName(kt.getName());
            dto.setCostCarrierCountryCode(kt.getLaendercode() == null ? "" : kt.getLaendercode());

            AbrechnenderKostentraeger akt = kt.getAbrechnender();
            if (akt != null) {
                dto.setBillingCarrierId(akt.getKostentraegerkennung());
                dto.setBillingCarrierName(akt.getName());
                dto.setBillingCarrierCountryCode(akt.getLaendercode() == null ? "" : akt.getLaendercode());
            }
        }

        return dto;
    }

}
