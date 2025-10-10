package de.bbajor.pvs.egk.reader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.egk.config.EgkToolProperties;
import de.bbajor.pvs.egk.model.AbrechnenderKostentraeger;
import de.bbajor.pvs.egk.model.Kostentraeger;
import de.bbajor.pvs.egk.model.UC_AllgemeineVersicherungsdatenXML;
import de.bbajor.pvs.egk.model.Versicherter;
import de.bbajor.pvs.egk.model.Versicherungsschutz;
import de.bbajor.pvs.egk.model.ZusatzInfos;
import de.bbajor.pvs.egk.model.ZusatzInfosAbrechnungGKV;
import de.bbajor.pvs.egk.model.personal.Adresse;
import de.bbajor.pvs.egk.model.personal.Person;
import de.bbajor.pvs.egk.model.personal.UcPersoenlicheVersichertenDatenXml;
import de.bbajor.pvs.egk.model.personal.VersicherterPersoenlich;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;

@Component
public class EgkReader {

    private static final Logger LOG = LogManager.getLogger();

    private final EgkToolProperties properties;
    private static final Map<String, String> EGK_TO_ISO = new HashMap<>();

    static {
        EGK_TO_ISO.put("D", "DE");
        EGK_TO_ISO.put("A", "AT");
        EGK_TO_ISO.put("CH", "CH");
        EGK_TO_ISO.put("F", "FR");
        EGK_TO_ISO.put("I", "IT");
        EGK_TO_ISO.put("USA", "US");
        // weitere Codes nach eGK-Spezifikation ergänzen
    }

    public EgkReader(EgkToolProperties properties) {
        this.properties = properties;
    }

    public Patient readPatientFromCard() throws Exception {
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

    private static Patient mapToDto(UcPersoenlicheVersichertenDatenXml data) {
        VersicherterPersoenlich v = data.getVersicherter();
        Person p = v.getPerson();
        Adresse adr = p.getStrassenAdresse();

        Patient patient = new Patient();
        patient.setInsuranceNumber(v.getVersichertenId());
        patient.setFirstName(p.getVorname());
        patient.setLastName(p.getNachname());
        patient.setBirth(p.getGeburtsdatum());
        patient.setGender(p.getGeschlecht());

        Address address = new Address();
        try {
            address.setStreet(adr == null ? "" : adr.getStrasse())
                    .setHouseNo(adr == null ? "" : adr.getHausnummer())
                    .setPostalCode(adr == null ? 00000 : Integer.parseInt(adr.getPostleitzahl()))
                    .setCity(adr == null ? "" : adr.getOrt());
            address.setCountry(EgkReader.toLocale(adr.getLand().getWohnsitzlaendercode()).getCountry());
        } catch (NullPointerException e) {
            LOG.warn("Land in Adresse nicht gesetzt");
        } catch (NumberFormatException e) {
            LOG.warn("Fehler beim Parsen der Postleitzahl von String zu Integer", e);
        }
        patient.setAddress(address);
        return patient;
    }

    public static Locale toLocale(String egkCode) {
        if (egkCode == null) {
            return null;
        }
        String iso = EGK_TO_ISO.getOrDefault(egkCode.toUpperCase(), "ZZ");
        return Locale.of("", iso);
    }

    public HealthInsurance readHealthInsuranceFromCard() throws Exception {
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

    private static HealthInsurance mapToDto(UC_AllgemeineVersicherungsdatenXML data) {

        assert (data != null);

        Versicherter v = data.getVersicherter();
        if (v == null || v.getVersicherungsschutz() == null) {
            return new HealthInsurance();
        }

        HealthInsurance dto = new HealthInsurance();
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
