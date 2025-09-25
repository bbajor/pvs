package de.bbajor.pvs.medication.controller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
import de.bbajor.pvs.medication.service.IntravitrealMedicationImportService;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;

@Component
public class IntravitrealMedicationViewPresenter {

    private static final Logger LOGGER = LogManager.getLogger();

    private final IntravitrealMedicationService intraviteralMedicationService;
    private final IntravitrealMedicationImportService importService;
    private final ModelToDtoMapper modelToDtoMapper;

    public IntravitrealMedicationViewPresenter(IntravitrealMedicationService ivomDrugService,
            IntravitrealMedicationImportService ivomDrugImportService,
            ModelToDtoMapper modelToDtoMapper) {
        this.intraviteralMedicationService = ivomDrugService;
        this.importService = ivomDrugImportService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public List<IntravitrealMedicationDto> findAllBy(String searchString) {
        List<IntravitrealMedication> ivoms = intraviteralMedicationService.findIntravitrealMedication(searchString);
        return ivoms.stream()
                .map(this::mapToDto)
                .toList();
    }

    public int importCsv(Reader in) throws RuntimeException {

        try {
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setDelimiter(';')
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .setSkipHeaderRecord(true)
                    .setHeader("EmptyColumn", "Nr", "Eingangsnummer", "Arzneimittelbezeichnung", "Darreichungsform",
                            "Zielgruppe", "Anwendungsart", "Anwendungsgebiete", "Indikation/ATC",
                            "Bescheiddatum der Zulassung", "Zulassungsstatus",
                            "Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer",
                            "Verkehrsfähigkeit", "Parallelimportinformationen", "EU-Verfahrensnummer",
                            "Zulassungsinhaber", "Hersteller/Endfreigabe", "Vertreiber", "Örtlicher Vertreter",
                            "Wirkstoffe", "Packungsgrößen-Gruppe/Verkaufsabgrenzung", "AM-Klassifikationen")
                    .get();

            // Debug: Headers und erste Zeile ausgeben
            Iterable<CSVRecord> records = csvFormat.parse(in);
            records.iterator().next();
            CSVRecord firstRecord = records.iterator().next();

            LOGGER.debug("Headers gefunden: " + String.join(", ", firstRecord.getParser().getHeaderNames()));
            LOGGER.debug("Erste Zeile: " + firstRecord.toString());

            List<IntravitrealMedication> newIvomDrugEntities = new ArrayList<>();

            for (CSVRecord record : records) {
                // Skip empty records
                if (record.size() <= 1)
                    continue;

                // Clean Eingangsnummer (remove quotes and leading/trailing spaces)
                String eingangsnummer = record.get("Eingangsnummer")
                        .replace("'", "")
                        .trim();

                IntravitrealMedication drug = new IntravitrealMedication()
                        .setEingangsnummer(eingangsnummer)
                        .setArzneimittelbezeichnung(record.get("Arzneimittelbezeichnung"))
                        .setDarreichungsform(record.get("Darreichungsform"))
                        .setZielgruppe(record.get("Zielgruppe"))
                        .setAnwendungsart(record.get("Anwendungsart"))
                        .setAnwendungsgebiete(record.get("Anwendungsgebiete"))
                        .setIndikationAtc(record.get("Indikation/ATC"))
                        .setBescheiddatumZulassung(record.get("Bescheiddatum der Zulassung"))
                        .setZulassungsstatus(record.get("Zulassungsstatus"))
                        .setZulassungsRegNrOderKennziffer(
                                record.get("Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer"))
                        .setVerkehrsfaehigkeit(record.get("Verkehrsfähigkeit"))
                        .setParallelimportinformationen(record.get("Parallelimportinformationen"))
                        .setEuVerfahrensnummer(record.get("EU-Verfahrensnummer"))
                        .setZulassungsinhaber(record.get("Zulassungsinhaber"))
                        .setHerstellerEndfreigabe(record.get("Hersteller/Endfreigabe"))
                        .setVertreiber(record.get("Vertreiber"))
                        .setOertlicherVertreter(record.get("Örtlicher Vertreter"))
                        .setWirkstoffe(record.get("Wirkstoffe"))
                        .setPackungsgroessenGruppe(record.get("Packungsgrößen-Gruppe/Verkaufsabgrenzung"))
                        .setAmKlassifikationen(record.get("AM-Klassifikationen"));

                // Debug output
                LOGGER.debug("Parsed drug: " + drug.getArzneimittelbezeichnung() +
                        " (Eingangsnummer: " + drug.getEingangsnummer() + ")");

                newIvomDrugEntities.add(drug);
            }

            LOGGER.debug("Anzahl gefundener Datensätze: " + newIvomDrugEntities.size());

            return importService.importNewIntravitrealMedications(newIvomDrugEntities);

        } catch (Exception e) {
            LOGGER.debug("CSV Parse Error: " + e.getMessage());
            LOGGER.warn(e);
            throw new RuntimeException("Fehler beim Einlesen der CSV: " + e.getMessage(), e);
        }
    }

    public List<IntravitrealMedicationDto> getAll() {
        return intraviteralMedicationService.findAll().stream().map(this::mapToDto).toList();
    }

    private IntravitrealMedicationDto mapToDto(IntravitrealMedication ivomDrug) {
        return modelToDtoMapper.toDto(ivomDrug);
    }

    public void save(IntravitrealMedicationDto bean) {
        IntravitrealMedication ivomDrug = modelToDtoMapper.toEntity(bean);
        intraviteralMedicationService.save(ivomDrug);
    }

}
