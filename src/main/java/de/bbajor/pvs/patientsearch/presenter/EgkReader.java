package de.bbajor.pvs.patientsearch.presenter;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.config.EgkToolProperties;
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
        return EgkXmlMapper.map(xml);
    }

}
