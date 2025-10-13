package de.bbajor.pvs.init;

import static org.instancio.Select.field;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.instancio.Instancio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterAddress;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

        private final TreatmentPlanService treatmentPlanService;

        private final PatientService patientService;
        private final IntravitrealMedicationService medicationService;
        private final SurgicalCenterService surgicalCenterService;
        private final IvomDiagnosisService diagnosisService;

        // Realistische Werte für die Testdaten
        private static final String[] MEDICATION_NAMES = {
                        "Eylea", "Lucentis", "Avastin", "Beovu", "Ozurdex", "Iluvien", "Jetrea", "Visudyne"
        };
        private static final String[] MEDICATION_WIRKSTOFFE = {
                        "Aflibercept", "Ranibizumab", "Bevacizumab", "Brolucizumab", "Dexamethason",
                        "Fluocinolonacetonid",
                        "Ocriplasmin", "Verteporfin"
        };
        private static final String[] DIAGNOSES_ICD = {
                        "H35.3", "H35.0", "H40.1", "H43.8", "H35.7", "H34.8", "H33.2", "H35.81"
        };
        private static final String[] DIAGNOSES_NAMES = {
                        "Feuchte altersbedingte Makuladegeneration", "Diabetisches Makulaödem", "Glaukom",
                        "Venenverschluss der Netzhaut", "Visusbedrohende Uveitis", "Retinale Ischämie",
                        "Netzhautablösung", "Zentrale seröse Chorioretinopathie"
        };
        private static final String[] STREET_NAMES = {
                        "Hauptstraße", "Gartenweg", "Bahnhofstraße", "Am Markt", "Feldweg", "Waldstraße", "Schulstraße",
                        "Kirchplatz", "Rosenweg", "Wiesenstraße"
        };
        private static final String[] CITIES = {
                        "Berlin", "Hamburg", "München", "Köln", "Frankfurt", "Stuttgart", "Düsseldorf", "Leipzig",
                        "Dortmund", "Essen", "Bremen", "Dresden"
        };
        private static final SideOfEye[] EYE_SIDES = { SideOfEye.LEFT, SideOfEye.RIGHT };
        private static final String[] SURGICAL_CENTER_NAMES = {
                        "Augenklinik Mitte", "MVZ Sehen und Mehr", "Augen-OP Zentrum", "Netzhautzentrum",
                        "Augenheilkunde Plus"
        };

        @Override
        public void run(String... args) {

                List<Patient> savedPatients = patientService.saveAll(createRealisticPatients(20));
                List<Medication> savedMedications = medicationService
                                .saveAll(createRealisticMedications(MEDICATION_NAMES.length));
                List<Diagnosis> diagnosisDtos = diagnosisService.saveAll(createDiagnoses());

                // Erzeuge OP-Zentren mit Zeitslots für Mittwoch und Freitag
                List<SurgicalCenter> surgicalCenters = createSurgicalCentersWithTimeSlots(
                                SURGICAL_CENTER_NAMES.length);

                // Erzeuge 10 Behandlungspläne mit Behandlungen
                createTreatmentPlansWithTreatments(10, savedPatients, savedMedications, diagnosisDtos, surgicalCenters);
        }

        /**
         * Erstellt realistische Patientendaten
         */
        private List<Patient> createRealisticPatients(int count) {
                Random random = new Random();

                return Instancio.ofList(Patient.class)
                                .size(count)
                                .ignore(field(Patient::getId))
                                .ignore(field(Patient::getVersion))
                                .supply(field(Patient::getFirstName), () -> {
                                        String[] firstNames = { "Anna", "Max", "Sophie", "Felix", "Lena", "Jonas",
                                                        "Marie", "Paul", "Emma", "Alexander" };
                                        return firstNames[random.nextInt(firstNames.length)];
                                })
                                .supply(field(Patient::getLastName), () -> {
                                        String[] lastNames = { "Müller", "Schmidt", "Schneider", "Fischer", "Weber",
                                                        "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann" };
                                        return lastNames[random.nextInt(lastNames.length)];
                                })
                                .supply(field(Patient::getEmail), () -> {
                                        String[] mailProviders = { "gmail.com", "web.de", "gmx.de", "yahoo.com",
                                                        "t-online.de" };
                                        return "patient" + UUID.randomUUID().toString().substring(0, 8) + "@"
                                                        + mailProviders[random.nextInt(mailProviders.length)];
                                })
                                .supply(field(Patient::getBirth), () -> {
                                        // Patienten zwischen 30 und 90 Jahren
                                        int year = LocalDate.now().getYear() - 30 - random.nextInt(60);
                                        int month = 1 + random.nextInt(12);
                                        int day = 1 + random.nextInt(28);
                                        return LocalDate.of(year, month, day);
                                })
                                .supply(field(Patient::getPhone), () -> {
                                        return "+49 " + (130 + random.nextInt(70)) + " "
                                                        + random.nextInt(10000000, 99999999);
                                })
                                .supply(field(Address::getStreet),
                                                () -> STREET_NAMES[random.nextInt(STREET_NAMES.length)] + " "
                                                                + (1 + random.nextInt(100)))
                                .supply(field(Address::getCity), () -> CITIES[random.nextInt(CITIES.length)])
                                .supply(field(Address::getPostalCode),
                                                () -> 10000 + random.nextInt(90000))
                                .supply(field(Address::getCountry), () -> "Deutschland")
                                .supply(field(HealthInsurance::getCostCarrierName), () -> {
                                        String[] insurances = { "TK", "AOK", "Barmer", "DAK", "BKK", "IKK", "KKH",
                                                        "Debeka" };
                                        return insurances[random.nextInt(insurances.length)];
                                })
                                .ignore(field(Address::getId))
                                .ignore(field(Address::getVersion))
                                .ignore(field(HealthInsurance::getId))
                                .ignore(field(HealthInsurance::getVersion))
                                .ignore(field(Patient::getPatientHistory))
                                .create();
        }

        /**
         * Erstellt realistische Medikamente für die Augenheilkunde
         */
        private List<Medication> createRealisticMedications(int count) {
                List<Medication> medications = new ArrayList<>();
                Random random = new Random();
                LocalDate now = LocalDate.now();

                for (int i = 0; i < count; i++) {
                        Medication med = new Medication();
                        med.setArzneimittelbezeichnung(MEDICATION_NAMES[i % MEDICATION_NAMES.length]);
                        med.setWirkstoffe(MEDICATION_WIRKSTOFFE[i % MEDICATION_WIRKSTOFFE.length]);
                        med.setDarreichungsform("Injektionslösung");
                        med.setIndikationAtc(DIAGNOSES_NAMES[i % DIAGNOSES_NAMES.length]);
                        med.setAnwendungsart("intravitreal");
                        med.setDescription(random.nextInt(10, 120) + "mg/ml Injektionslösung");
                        med.setZulassungsinhaber("Pharma GmbH");
                        med.setFavourite(random.nextBoolean());
                        med.setValidFrom(now.minusYears(random.nextInt(1, 5)));
                        med.setValidUntil(now.plusYears(random.nextInt(3, 10)));
                        medications.add(med);
                }

                return medications;
        }

        /**
         * Erstellt Diagnosen basierend auf ICD-Codes
         */
        private List<Diagnosis> createDiagnoses() {
                List<Diagnosis> diagnoses = new ArrayList<>();

                for (int i = 0; i < DIAGNOSES_ICD.length; i++) {
                        Diagnosis diagnosis = new Diagnosis();
                        diagnosis.setName(DIAGNOSES_NAMES[i]);
                        diagnosis.setIcdCode(DIAGNOSES_ICD[i]);
                        diagnosis.setDescription("Diagnose für " + DIAGNOSES_NAMES[i]);
                        diagnoses.add(diagnosis);
                }

                return diagnoses;
        }

        /**
         * Erstellt OP-Zentren mit Zeitslots für Mittwoch und Freitag über 2 Jahre
         */
        private List<SurgicalCenter> createSurgicalCentersWithTimeSlots(int count) {
                List<SurgicalCenter> centers = new ArrayList<>();
                Random random = new Random();

                for (int i = 0; i < count; i++) {
                        SurgicalCenter center = new SurgicalCenter();
                        center.setName(SURGICAL_CENTER_NAMES[i % SURGICAL_CENTER_NAMES.length]);
                        center.setDescription("Zentrum für intravitreale Injektionen");

                        SurgicalCenterAddress address = new SurgicalCenterAddress();
                        address.setStreet(STREET_NAMES[random.nextInt(STREET_NAMES.length)] + " "
                                        + (1 + random.nextInt(100)));
                        address.setCity(CITIES[random.nextInt(CITIES.length)]);
                        address.setPostalCode(10000 + random.nextInt(90000));
                        address.setCountry("Deutschland");
                        center.setSurgicalCenterAddress(address);

                        // Generiere Zeitslots für Mittwoch und Freitag über 2 Jahre
                        List<SurgicalCenterTimeSlot> timeSlots = generateTimeSlots(center);
                        center.setAvailableTimeSlots(timeSlots);

                        // Speichere das Zentrum und die Zeitslots
                        centers.add(surgicalCenterService.saveTimeSlotsAndSurgicalCenter(timeSlots, center));
                }

                return centers;
        }

        /**
         * Generiert Zeitslots für Mittwoch und Freitag für die nächsten 2 Jahre, 7-9
         * Uhr
         */
        private List<SurgicalCenterTimeSlot> generateTimeSlots(SurgicalCenter center) {
                List<SurgicalCenterTimeSlot> timeSlots = new ArrayList<>();
                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusYears(2);

                // Starte 6 Monate in der Vergangenheit
                LocalDate startDateWithHistory = startDate.minusMonths(6);
                
                // Finde den ersten Mittwoch
                LocalDate wednesday = startDateWithHistory.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

                // Generiere Mittwoch-Slots für 2 Jahre
                while (wednesday.isBefore(endDate)) {
                        SurgicalCenterTimeSlot wednesdaySlot = new SurgicalCenterTimeSlot();
                        wednesdaySlot.setDate(wednesday);
                        wednesdaySlot.setStartTime(LocalTime.of(7, 0));
                        wednesdaySlot.setEndTime(LocalTime.of(9, 0));
                        wednesdaySlot.setSurgicalCenter(center);
                        // Slots in der Vergangenheit sind nicht mehr verfügbar
                        wednesdaySlot.setAvailable(wednesday.isAfter(startDate));
                        wednesdaySlot.setApproved(true);
                        wednesdaySlot.setDescription("Regulärer Mittwoch-Termin");
                        timeSlots.add(wednesdaySlot);

                        wednesday = wednesday.plusWeeks(1);
                }

                // Finde den ersten Freitag
                LocalDate friday = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

                // Generiere Freitag-Slots für 2 Jahre
                while (friday.isBefore(endDate)) {
                        SurgicalCenterTimeSlot fridaySlot = new SurgicalCenterTimeSlot();
                        fridaySlot.setDate(friday);
                        fridaySlot.setStartTime(LocalTime.of(7, 0));
                        fridaySlot.setEndTime(LocalTime.of(9, 0));
                        fridaySlot.setSurgicalCenter(center);
                        // Slots in der Vergangenheit sind nicht mehr verfügbar
                        fridaySlot.setAvailable(friday.isAfter(startDate));
                        fridaySlot.setApproved(true);
                        fridaySlot.setDescription("Regulärer Freitag-Termin");
                        timeSlots.add(fridaySlot);

                        friday = friday.plusWeeks(1);
                }

                return timeSlots;
        }

        /**
         * Erstellt Behandlungspläne mit zugehörigen Behandlungen
         */
        /**
         * Erstellt Behandlungspläne mit zugehörigen Behandlungen
         * Verbesserte Version mit größerer Variabilität
         */
        private void createTreatmentPlansWithTreatments(int count, List<Patient> patients,
                        List<Medication> medications,
                        List<Diagnosis> diagnoses,
                        List<SurgicalCenter> surgicalCenters) {
                Random random = new Random();
                LocalDate now = LocalDate.now();

                // Stellen Sie sicher, dass jeder Patient einen eigenen Behandlungsplan hat
                List<Patient> selectedPatients = new ArrayList<>(patients);
                // Mische die Patienten, um zufällige Auswahl zu gewährleisten
                java.util.Collections.shuffle(selectedPatients);

                // Beschränke auf 'count' Patienten oder weniger, falls nicht genug Patienten
                // vorhanden
                int planCount = Math.min(count, selectedPatients.size());

                // Generiere für jeden ausgewählten Patienten einen Behandlungsplan
                for (int i = 0; i < planCount; i++) {
                        // Wichtig: Hole den Patienten direkt aus der Datenbank, um sicherzustellen,
                        // dass es sich um eine persistierte Entität handelt und nicht um ein
                        // transientes Objekt
                        Patient patient = selectedPatients.get(i);

                        // Stelle sicher, dass der Patient eine ID hat (also gespeichert ist)
                        if (patient.getId() == null) {
                                // Das sollte nicht passieren, da wir die Patienten vorher mit
                                // patientService.saveAll(patients) gespeichert haben,
                                // aber zur Sicherheit prüfen wir es
                                System.out.println("Warnung: Patient " + patient.getFirstName() + " "
                                                + patient.getLastName() + " hat keine ID!");
                                continue; // Überspringe diesen Patienten
                        }

                        // Erstelle einen individualisierten Behandlungsplan
                        TreatmentPlan plan = new TreatmentPlan();
                        plan.setCreationDate(now.minusDays(random.nextInt(90)));

                        // Wähle eine passende Diagnose und stelle sicher, dass sie eine ID hat
                        Diagnosis diagnosis = diagnoses.get(random.nextInt(diagnoses.size()));
                        if (diagnosis.getId() == null) {
                                System.out.println("Warnung: Diagnose " + diagnosis.getName() + " hat keine ID!");
                                continue; // Überspringe diesen Plan
                        }
                        plan.setDiagnosis(diagnosis);

                        // Personalisiere den Behandlungsplan
                        plan.setDescription("Behandlungsplan für " + diagnosis.getName() + " - "
                                        + patient.getFirstName() + " " + patient.getLastName());
                        plan.setPatient(patient);

                        // Füge individuelle Informationen hinzu
                        String[] additionalInfos = {
                                        "Regelmäßige Kontrolle erforderlich",
                                        "Patient benötigt besondere Nachsorge",
                                        "Lokale Anästhesie vor Injektion empfohlen",
                                        "Patient ist an Glaukom vorerkrankt",
                                        "Diabetes mellitus Typ II",
                                        "Erhöhtes Risiko für Endophthalmitis",
                                        "Besonders vorsichtige Injektion aufgrund enger Kammerwinkelverhältnisse"
                        };
                        plan.setAdditionalInformation(additionalInfos[random.nextInt(additionalInfos.length)]);

                        // Speichere den Behandlungsplan
                        TreatmentPlan savedPlan;
                        try {
                                savedPlan = treatmentPlanService.saveTreatmentPlan(plan);
                        } catch (Exception e) {
                                System.out.println("Fehler beim Speichern des Behandlungsplans: " + e.getMessage());
                                e.printStackTrace();
                                continue; // Überspringe diesen Plan bei einem Fehler
                        } // Wähle 1-3 verschiedene OP-Zentren für diesen Patienten
                          // Manche Patienten bevorzugen immer das gleiche Zentrum, andere wechseln
                        int centerCount = random.nextInt(3) + 1; // 1 bis 3 Zentren
                        List<SurgicalCenter> patientCenters = new ArrayList<>();

                        // Wähle zufällige Zentren
                        for (int c = 0; c < centerCount && c < surgicalCenters.size(); c++) {
                                int centerIndex = random.nextInt(surgicalCenters.size());
                                if (!patientCenters.contains(surgicalCenters.get(centerIndex))) {
                                        patientCenters.add(surgicalCenters.get(centerIndex));
                                }
                        }

                        // Variable Anzahl von Behandlungen pro Plan (2-12)
                        // Chronische Patienten erhalten mehr Behandlungen
                        int maxTreatments = random.nextInt(11) + 2; // 2 bis 12 Behandlungen
                        List<Treatment> treatments = new ArrayList<>();

                        // Wähle ein bevorzugtes Medikament für diesen Patienten
                        Medication preferredMedication = medications.get(random.nextInt(medications.size()));
                        SideOfEye preferredSideOfEye = EYE_SIDES[random.nextInt(EYE_SIDES.length)];

                        // Erstelle die Behandlungen mit einer gewissen Regelmäßigkeit
                        for (int j = 0; j < maxTreatments; j++) {
                                // Mache j final, damit es im Lambda-Ausdruck verwendet werden kann
                                final int treatmentIndex = j;

                                // Wähle ein Zentrum für diese Behandlung
                                SurgicalCenter center = patientCenters.get(random.nextInt(patientCenters.size()));

                                // Hole Slots für dieses Zentrum
                                List<SurgicalCenterTimeSlot> availableSlots = center.getAvailableTimeSlots()
                                                .stream()
                                                .filter(slot -> {
                                                    // Stelle sicher, dass der Slot nach dem Erstellungsdatum des Plans liegt
                                                    LocalDate planCreationDate = plan.getCreationDate();
                                                    
                                                    // Behandlungen ab Planstart im 14-Tage-Rhythmus
                                                    return slot.getDate()
                                                            .isAfter(planCreationDate.plusDays(treatmentIndex * 14)) &&
                                                           slot.getDate()
                                                            .isBefore(planCreationDate.plusDays(treatmentIndex * 14 + 10)) &&
                                                           // Für historische Slots (vor heute) muss available nicht geprüft werden
                                                           (slot.getDate().isBefore(now) || slot.isAvailable());
                                                })
                                                .limit(5) // Nur die ersten 5 passenden Slots betrachten
                                                .collect(Collectors.toList());

                                // Falls keine passenden Slots gefunden wurden, breche ab
                                if (availableSlots.isEmpty()) {
                                        break;
                                }

                                // Wähle einen zufälligen verfügbaren Slot
                                SurgicalCenterTimeSlot slot = availableSlots.get(random.nextInt(availableSlots.size()));

                                // Erstelle eine neue Behandlung
                                Treatment treatment = new Treatment();
                                treatment.setTreatmentPlan(savedPlan);

                                // 80% Wahrscheinlichkeit für bevorzugte Seite, 20% für andere Seite oder
                                // beidseitig
                                if (random.nextDouble() < 0.8) {
                                        treatment.setSideOfEye(preferredSideOfEye);
                                } else {
                                        treatment.setSideOfEye(
                                                        EYE_SIDES[random.nextInt(EYE_SIDES.length)]);
                                }

                                treatment.setSurgicalCenterTimeSlot(slot);
                                treatment.setApprovalDate(slot.getDate().minusDays(random.nextInt(10) + 1));

                                // Verschiedene Bemerkungen für Behandlungen
                                String[] infoRemarks = {
                                                "Standardbehandlung",
                                                "Verlaufskontrolle",
                                                "Follow-up nach OCT",
                                                "Initiale Behandlungsphase",
                                                "Aufgrund von Makulaödem",
                                                "Nach Laserkoagulation"
                                };
                                treatment.setAdditionalInfo(infoRemarks[random.nextInt(infoRemarks.length)]);

                                // 90% Wahrscheinlichkeit für bevorzugtes Medikament, 10% für Wechsel
                                if (random.nextDouble() < 0.9) {
                                        treatment.setMedication(preferredMedication);
                                } else {
                                        treatment.setMedication(medications.get(random.nextInt(medications.size())));
                                }

                                treatments.add(treatment);

                                // Markiere den Zeitslot als nicht mehr verfügbar
                                slot.setAvailable(false);
                        }

                        // Speichere die Behandlungen, falls welche erstellt wurden
                        if (!treatments.isEmpty()) {
                                try {
                                        treatmentPlanService.saveNewTreatmentsForExistingPlan(treatments,
                                                        savedPlan.getId());
                                } catch (Exception e) {
                                        System.out.println("Fehler beim Speichern der Behandlungen: " + e.getMessage());
                                        e.printStackTrace();
                                }
                        }
                }
        }
}
