package de.bbajor.pvs.ivomdrug.controller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.misc.ModelToDtoMapper;
import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomdrug.model.IvomDrug;
import de.bbajor.pvs.ivomdrug.service.IvomDrugImportService;
import de.bbajor.pvs.ivomdrug.service.IvomDrugService;

@Component
public class IvomDrugViewPresenter {

    private final IvomDrugService ivomDrugService;
    private final IvomDrugImportService ivomDrugImportService;
    private final ModelToDtoMapper modelToDtoMapper;

    public IvomDrugViewPresenter(IvomDrugService ivomDrugService, IvomDrugImportService ivomDrugImportService,
            ModelToDtoMapper modelToDtoMapper) {
        this.ivomDrugService = ivomDrugService;
        this.ivomDrugImportService = ivomDrugImportService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public List<IvomDrugDto> findAllBy(String searchString) {
        List<IvomDrug> ivoms = ivomDrugService.findIvomDrugs(searchString);
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

            System.out.println("Headers gefunden: " + String.join(", ", firstRecord.getParser().getHeaderNames()));
            System.out.println("Erste Zeile: " + firstRecord.toString());

            List<IvomDrug> newIvomDrugEntities = new ArrayList<>();

            for (CSVRecord record : records) {
                // Skip empty records
                if (record.size() <= 1)
                    continue;

                // Clean Eingangsnummer (remove quotes and leading/trailing spaces)
                String eingangsnummer = record.get("Eingangsnummer")
                        .replace("'", "")
                        .trim();

                IvomDrug drug = new IvomDrug()
                        .setEingangsnummer(eingangsnummer)
                        .setArzneimittelbezeichnung(record.get("Arzneimittelbezeichnung"))
                        .setDarreichungsform(record.get("Darreichungsform"))
                        .setZielgruppe(record.get("Zielgruppe"))
                        .setAnwendungsart(record.get("Anwendungsart"))
                        .setAnwendungsgebiete(record.get("Anwendungsgebiete"))
                        .setIndikationAtc(record.get("Indikation/ATC"))
                        .setBescheiddatumZulassung(record.get("Bescheiddatum der Zulassung"))
                        .setZulassungsstatus(record.get("Zulassungsstatus"))
                        .setZulassungsNr(
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
                System.out.println("Parsed drug: " + drug.getArzneimittelbezeichnung() +
                        " (Eingangsnummer: " + drug.getEingangsnummer() + ")");

                newIvomDrugEntities.add(drug);
            }

            System.out.println("Anzahl gefundener Datensätze: " + newIvomDrugEntities.size());

            return ivomDrugImportService.importNewDrugs(newIvomDrugEntities);

        } catch (Exception e) {
            System.err.println("CSV Parse Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Fehler beim Einlesen der CSV: " + e.getMessage(), e);
        }
    }

    public List<IvomDrugDto> getAll() {
        return ivomDrugService.findAll().stream().map(this::mapToDto).toList();
    }

    private IvomDrugDto mapToDto(IvomDrug ivomDrug) {
        return modelToDtoMapper.toDto(ivomDrug);
    }

}
