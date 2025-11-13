package de.bbajor.pvs.medication.controller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.service.IntravitrealMedicationImportService;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.medication.service.MedicationFavouriteService;

@Component
public class MedicationViewPresenter {

    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private IntravitrealMedicationService medicationService;
    @Autowired
    private IntravitrealMedicationImportService importService;
    @Autowired
    private MedicationFavouriteService medicationFavouriteService;

    public List<Medication> findAllBy(String searchString) {
        return medicationService.findIntravitrealMedication(searchString);
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

            // Parse CSV records
            Iterable<CSVRecord> records = csvFormat.parse(in);
            
            List<Medication> newMedicationEntityList = new ArrayList<>();
            int recordCount = 0;
            boolean firstDataRow = true;

            for (CSVRecord record : records) {
                recordCount++;
                
                // Skip first data row (after header) due to line break issues in CSV generation
                if (firstDataRow) {
                    LOGGER.debug("Skipping first data row at line " + recordCount + " due to CSV generation line break issues");
                    firstDataRow = false;
                    continue;
                }
                
                // Skip empty records
                if (record.size() <= 1) {
                    LOGGER.debug("Skipping empty record at line " + recordCount);
                    continue;
                }

                try {
                    // Clean Eingangsnummer (remove quotes and leading/trailing spaces)
                    String eingangsnummer = record.get("Eingangsnummer")
                            .replace("'", "")
                            .replace("\"", "")
                            .trim();
                    
                    // Get Zulassungs-/Reg.-Nr. for matching existing records
                    String zulassungsNr = record.get("Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer")
                            .replace("'", "")
                            .replace("\"", "")
                            .trim();

                    Medication drug = new Medication()
                            .setEingangsnummer(eingangsnummer)
                            .setZulassungsNr(zulassungsNr.isEmpty() ? eingangsnummer : zulassungsNr) // Use eingangsnummer as fallback
                            .setArzneimittelbezeichnung(record.get("Arzneimittelbezeichnung"))
                            .setDarreichungsform(record.get("Darreichungsform"))
                            .setZielgruppe(record.get("Zielgruppe"))
                            .setAnwendungsart(record.get("Anwendungsart"))
                            .setAnwendungsgebiete(record.get("Anwendungsgebiete"))
                            .setIndikationAtc(record.get("Indikation/ATC"))
                            .setBescheiddatumZulassung(record.get("Bescheiddatum der Zulassung"))
                            .setZulassungsstatus(record.get("Zulassungsstatus"))
                            .setZulassungsRegNrOderKennziffer(zulassungsNr)
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
                            " (Eingangsnummer: " + drug.getEingangsnummer() + 
                            ", ZulassungsNr: " + drug.getZulassungsNr() + ")");

                    newMedicationEntityList.add(drug);
                } catch (Exception e) {
                    LOGGER.warn("Error parsing record at line " + recordCount + ": " + e.getMessage(), e);
                    // Continue with next record
                }
            }

            LOGGER.debug("Anzahl gefundener Datensätze: " + newMedicationEntityList.size());

            return importService.importNewIntravitrealMedications(newMedicationEntityList);

        } catch (Exception e) {
            LOGGER.debug("CSV Parse Error: " + e.getMessage());
            LOGGER.warn(e);
            throw new RuntimeException("Fehler beim Einlesen der CSV: " + e.getMessage(), e);
        }
    }

    public List<Medication> getAll() {
        return medicationService.findAll();
    }

    public Medication save(Medication medication) {
        return medicationService.save(medication);
    }

    public List<MedicationFavourite> getActiveFavouritesForCurrentInstitution() {
        return medicationFavouriteService.getActiveFavouritesForCurrentInstitution();
    }

    public MedicationFavourite addFavourite(Long medicationId) {
        return medicationFavouriteService.addFavouriteForCurrentInstitution(medicationId, null);
    }

    public void removeFavourite(Long favouriteId) {
        medicationFavouriteService.deactivateFavourite(favouriteId);
    }

}
