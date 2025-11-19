package de.bbajor.pvs.init;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.instancio.Instancio;
import static org.instancio.Select.field;
import javax.sql.DataSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.medication.service.MedicationFavouriteService;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class TestDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    private final TreatmentPlanService treatmentPlanService;

    private final PatientService patientService;
    private final IntravitrealMedicationService medicationService;
    private final MedicationFavouriteService medicationFavouriteService;
    private final SurgicalCenterService surgicalCenterService;
    private final IvomDiagnosisService diagnosisService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;
    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

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
    private static final SideOfEye[] EYE_SIDES = {SideOfEye.LEFT, SideOfEye.RIGHT};
    private static final String[] SURGICAL_CENTER_NAMES = {
        "Augenklinik Mitte", "MVZ Sehen und Mehr", "Augen-OP Zentrum", "Netzhautzentrum",
        "Augenheilkunde Plus"
    };

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    @Transactional
    public void initializeTestData() {
        // Only run for H2 databases - PostgreSQL uses Flyway migrations
        try {
            String jdbcUrl = dataSource.getConnection().getMetaData().getURL();
            if (!jdbcUrl.startsWith("jdbc:h2:")) {
                log.info("TestDataInitializer skipped - not using H2 database (URL: {})", jdbcUrl);
                return;
            }
        } catch (Exception e) {
            log.warn("Could not determine database type, skipping TestDataInitializer", e);
            return;
        }
        
        log.info("TestDataInitializer running for H2 database");
        
        // Ensure Hibernate has created the schema by accessing entities
        // This forces Hibernate to create tables if using create-drop
        try {
            // Force schema creation by accessing repositories
            // This triggers Hibernate to create all necessary tables
            institutionRepository.count();
            userAccountRepository.count();
            
            // Force creation of all dependent tables by querying entities
            // This ensures all tables exist before we try to insert data
            entityManager.createQuery("SELECT COUNT(p) FROM Patient p").getSingleResult();
            entityManager.createQuery("SELECT COUNT(ph) FROM PatientHistory ph").getSingleResult();
            entityManager.createQuery("SELECT COUNT(hi) FROM HealthInsurance hi").getSingleResult();
            entityManager.createQuery("SELECT COUNT(m) FROM Medication m").getSingleResult();
            entityManager.createQuery("SELECT COUNT(d) FROM Diagnosis d").getSingleResult();
            
            // Flush to ensure all DDL statements are executed
            entityManager.flush();
        } catch (Exception e) {
            // If still fails, wait a bit and retry
            log.debug("Waiting for schema creation...", e);
            try {
                Thread.sleep(500);
                institutionRepository.count();
                userAccountRepository.count();
                entityManager.createQuery("SELECT COUNT(p) FROM Patient p").getSingleResult();
                entityManager.createQuery("SELECT COUNT(ph) FROM PatientHistory ph").getSingleResult();
                entityManager.createQuery("SELECT COUNT(hi) FROM HealthInsurance hi").getSingleResult();
                entityManager.createQuery("SELECT COUNT(m) FROM Medication m").getSingleResult();
                entityManager.createQuery("SELECT COUNT(d) FROM Diagnosis d").getSingleResult();
                entityManager.flush();
            } catch (Exception e2) {
                log.warn("Schema might not be fully created yet, continuing anyway", e2);
            }
        }

        // Initialisiere Test-Institutionen mit Standorten und Usern
        TestInstitutions testInstitutions = initTestInstitutions();

        // Flush, um sicherzustellen, dass alle Institutionen und Locations persistiert sind,
        // bevor wir sie später verwenden
        entityManager.flush();
        entityManager.clear(); // Clear session, um alle Objekte zu detachen

        // Erstelle Superadmin
        createInstitutionUserIfNotExists(
                null,
                "superadmin",
                "admin@pvs.local",
                "Super Administrator",
                Set.of(AppRoles.SUPER_ADMIN, AppRoles.ADMIN, AppRoles.USER)
        );

        // Institution 1: Leer lassen (nur Institutionsadmin)
        createInstitutionUserIfNotExists(
                testInstitutions.institution1,
                "inst1-admin",
                "inst1-admin@pvs.local",
                "Institution 1 Admin",
                Set.of(AppRoles.INSTITUTION_ADMIN, AppRoles.ADMIN, AppRoles.USER)
        );

        // Institution 2: Vollständige Testdaten mit allen Rollen
        createUsersForInstitution(testInstitutions.institution2);

        // CRITICAL: Set InstitutionContext for test data initialization (Institution 2)
        InstitutionContext.setInstitutionId(testInstitutions.institution2.getId());
        log.debug("InstitutionContext set to: {} for test data initialization", testInstitutions.institution2.getId());

        // Erstelle Testdaten für Institution 2
        List<Medication> savedMedications = medicationService
                .saveAll(createRealisticMedications(MEDICATION_NAMES.length));
        List<MedicationFavourite> savedFavourites = createMedicationFavouritesForInstitution(
                testInstitutions.institution2, savedMedications);
        List<Diagnosis> diagnosisDtos = diagnosisService.saveAll(createDiagnoses());

        // Erstelle 50 Patienten für Institution 2 (jeweils auf einem der beiden Standorte)
        List<Patient> savedPatients = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Location patientLocation = (i % 2 == 0) ? testInstitutions.institution2Location1 : testInstitutions.institution2Location2;
            List<Patient> patients = createRealisticPatients(1, testInstitutions.institution2, patientLocation);
            savedPatients.addAll(patientService.saveAll(patients));
        }

        // Erzeuge OP-Zentren mit Zeitslots (2 Jahre Vergangenheit, 1 Jahr Zukunft)
        List<SurgicalCenter> surgicalCenters = createSurgicalCentersWithTimeSlots(
                SURGICAL_CENTER_NAMES.length, testInstitutions.institution2Location1);

        // Erzeuge Behandlungspläne für alle Patienten (mindestens 5 Termine in Vergangenheit, max 1 in Zukunft)
        createTreatmentPlansWithTreatments(savedPatients, savedFavourites, diagnosisDtos, surgicalCenters);

        // Clear InstitutionContext after initialization to avoid side effects
        InstitutionContext.clear();
        log.debug("InstitutionContext cleared after test data initialization");
    }

    /**
     * Datenklasse für Test-Institutionen
     */
    private static class TestInstitutions {
        Institution institution1;
        Location institution1Location1;
        Location institution1Location2;
        Institution institution2;
        Location institution2Location1;
        Location institution2Location2;
    }

    /**
     * Initialisiert 2 Test-Institutionen mit je 2 Standorten
     *
     * @return TestInstitutions mit allen Institutionen und Standorten
     */
    private TestInstitutions initTestInstitutions() {
        TestInstitutions result = new TestInstitutions();

        // Institution 1: Leer (nur Institutionsadmin)
        result.institution1 = createInstitutionIfNotExists(
                "PRAX-001",
                "Augenarztpraxis Dr. Müller",
                "Leere Test-Institution für Neuaufbau"
        );
        result.institution1Location1 = createLocationForInstitution(result.institution1, "Standort 1 - Hauptpraxis");
        result.institution1Location2 = createLocationForInstitution(result.institution1, "Standort 2 - Filiale");

        // Institution 2: Vollständige Testdaten
        result.institution2 = createInstitutionIfNotExists(
                "PRAX-002",
                "MVZ Augenheilkunde Hamburg",
                "Vollständige Test-Institution mit Patienten und Behandlungen"
        );
        result.institution2Location1 = createLocationForInstitution(result.institution2, "Standort 1 - Hauptpraxis");
        result.institution2Location2 = createLocationForInstitution(result.institution2, "Standort 2 - Filiale");

        return result;
    }

    /**
     * Erstellt User für eine Institution mit allen Rollen
     */
    private void createUsersForInstitution(Institution institution) {
        createInstitutionUserIfNotExists(
                institution,
                "inst2-admin",
                "inst2-admin@pvs.local",
                "Institution 2 Admin",
                Set.of(AppRoles.ADMIN, AppRoles.USER)
        );
        createInstitutionUserIfNotExists(
                institution,
                "inst2-owner",
                "inst2-owner@pvs.local",
                "Institution 2 Owner",
                Set.of(AppRoles.OWNER, AppRoles.USER)
        );
        createInstitutionUserIfNotExists(
                institution,
                "inst2-doctor",
                "inst2-doctor@pvs.local",
                "Institution 2 Doctor",
                Set.of(AppRoles.DOCTOR, AppRoles.USER)
        );
        createInstitutionUserIfNotExists(
                institution,
                "inst2-medical",
                "inst2-medical@pvs.local",
                "Institution 2 Medical Staff",
                Set.of(AppRoles.MEDICAL_STAFF, AppRoles.USER)
        );
        createInstitutionUserIfNotExists(
                institution,
                "inst2-tech",
                "inst2-tech@pvs.local",
                "Institution 2 Tech User",
                Set.of(AppRoles.TECH_USER, AppRoles.USER)
        );
        createInstitutionUserIfNotExists(
                institution,
                "inst2-user",
                "inst2-user@pvs.local",
                "Institution 2 User",
                Set.of(AppRoles.USER)
        );
    }

    /**
     * Erstellt eine Location für eine Institution, falls noch nicht vorhanden
     */
    private Location createLocationForInstitution(Institution institution, String locationName) {
        // Ensure Institution is persisted
        if (institution.getId() == null) {
            institution = institutionRepository.save(institution);
        }

        // Check if location with this name already exists for this institution
        List<Location> existingLocations = locationService.findByInstitution(institution);
        for (Location loc : existingLocations) {
            if (locationName.equals(loc.getLocationName())) {
                return loc;
            }
        }

        // Create new location
        Location location = new Location();
        location.setLocationName(locationName);
        location.setStreet("Hauptstraße");
        location.setHouseNumber("42");
        location.setPostalCode("10115");
        location.setCity("Berlin");
        location.setCountry("Deutschland");
        location.setOwnerTitle("Dr. med.");
        location.setOwnerName("Max Mustermann");
        location.setLanr("123456789");
        location.setBsnr("987654321");
        location.setPhone("+49 30 12345678");
        location.setFax("+49 30 12345679");
        location.setEmail("praxis@augenarzt-muster.de");
        location.setAdditionalInfo(
                "Beispielstandort für die Entwicklungsumgebung. Spezialisiert auf Netzhauterkrankungen und intravitreale Injektionen.");
        location.setInstitution(institution);
        return locationService.saveLocation(location);
    }

    /**
     * Erstellt eine Institution, falls sie noch nicht existiert
     */
    private Institution createInstitutionIfNotExists(String code, String name, String description) {
        return institutionRepository.findByInstitutionCode(code)
                .orElseGet(() -> {
                    String normalizedCode = code.replace("-", "_").toLowerCase();
                    Institution institution = new Institution()
                            .setInstitutionCode(code)
                            .setInstitutionName(name)
                            .setDescription(description)
                            .setActive(true)
                            .setDatabaseName("pvs_inst_" + normalizedCode)
                            .setContainerName("postgres-inst-" + normalizedCode);
                    Institution saved = institutionRepository.save(institution);
                    return saved;
                });
    }

    /**
     * Erstellt einen User mit Institution-Zuordnung, falls er noch nicht existiert
     *
     * @param institution die Institution (Einrichtung) für den User (null für Super-Admin)
     */
    private void createInstitutionUserIfNotExists(Institution institution, String username, String email,
            String fullName, Set<String> roles) {
        userAccountRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserAccount user = new UserAccount()
                            .setUsername(username)
                            .setEmail(email)
                            .setFullName(fullName)
                            .setPasswordHash(passwordEncoder.encode("123"))
                            .setEnabled(true)
                            .setInstitution(institution)
                            .setRoles(roles);
                    UserAccount saved = userAccountRepository.save(user);
                    return saved;
                });
    }

    /**
     * Erstellt die Testuser für verschiedene Rollen in der Datenbank
     */
    private void createTestUsers() {
        String testPassword = "123";

        // Test-Admin User (umbenannt, da SampleUsers.ADMIN_USERNAME bereits existiert)
        createUserIfNotExists("test-admin", testPassword, "Test Administrator", "test-admin@example.com",
                UUID.randomUUID().toString(), AppRoles.ADMIN, AppRoles.OWNER, AppRoles.USER,
                AppRoles.DOCTOR);

        // Test-User (umbenannt, da SampleUsers.USER_USERNAME bereits existiert)
        createUserIfNotExists("test-user", testPassword, "Test Benutzer", "test-user@example.com",
                UUID.randomUUID().toString(), AppRoles.USER, AppRoles.TECH_USER, AppRoles.MEDICAL_STAFF);

        // Doctor User
        createUserIfNotExists("test-doctor", testPassword, "Dr. Test Arzt", "test-doctor@example.com",
                UUID.randomUUID().toString(), AppRoles.DOCTOR, AppRoles.USER);

        // Tech User
        createUserIfNotExists("test-tech", testPassword, "Test Techniker", "test-tech@example.com",
                UUID.randomUUID().toString(), AppRoles.TECH_USER, AppRoles.USER);

        // Medical Staff
        createUserIfNotExists("test-medical", testPassword, "Test Medizinisches Personal", "test-medical@example.com",
                UUID.randomUUID().toString(), AppRoles.MEDICAL_STAFF, AppRoles.USER);

        // Owner
        createUserIfNotExists("test-owner", testPassword, "Test Praxisinhaber", "test-owner@example.com",
                UUID.randomUUID().toString(), AppRoles.OWNER, AppRoles.USER);
    }

    /**
     * Hilfsmethode zum Erstellen von Benutzern
     */
    private void createUserIfNotExists(String username, String password, String fullName, String email,
            String userId, String... roles) {
        if (userAccountRepository.findByUsername(username).isEmpty()) {
            UserAccount user = new UserAccount();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setUserId(userId);
            user.setEnabled(true);
            for (String role : roles) {
                user.getRoles().add(role);
            }
            userAccountRepository.save(user);
        }
    }

    /**
     * Erstellt realistische Patientendaten
     */
    private List<Patient> createRealisticPatients(int count, Institution institution, Location location) {
        Random random = new Random();

        List<Patient> patients = Instancio.ofList(Patient.class)
                .size(count)
                .ignore(field(Patient::getId))
                .ignore(field(Patient::getVersion))
                .supply(field(Patient::getFirstName), () -> {
                    String[] firstNames = {"Anna", "Max", "Sophie", "Felix", "Lena", "Jonas",
                        "Marie", "Paul", "Emma", "Alexander"};
                    return firstNames[random.nextInt(firstNames.length)];
                })
                .supply(field(Patient::getLastName), () -> {
                    String[] lastNames = {"Müller", "Schmidt", "Schneider", "Fischer", "Weber",
                        "Meyer", "Wagner", "Becker", "Schulz", "Hoffmann"};
                    return lastNames[random.nextInt(lastNames.length)];
                })
                .supply(field(Patient::getEmail), () -> {
                    String[] mailProviders = {"gmail.com", "web.de", "gmx.de", "yahoo.com",
                        "t-online.de"};
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
                .supply(field(Address::getCountry), () -> Locale.GERMANY)
                .supply(field(HealthInsurance::getCostCarrierName), () -> {
                    String[] insurances = {"TK", "AOK", "Barmer", "DAK", "BKK", "IKK", "KKH",
                        "Debeka"};
                    return insurances[random.nextInt(insurances.length)];
                })
                .ignore(field(HealthInsurance::getId))
                .ignore(field(HealthInsurance::getVersion))
                .ignore(field(Patient::getPatientHistory))
                .create();

        // CRITICAL: Set institution and location for each patient and their health insurance
        // This ensures all entities have the required non-nullable associations before saving
        for (Patient patient : patients) {
            patient.setInstitution(institution);
            patient.setLocation(location);
            
            // Set institution for health insurance (required non-nullable association)
            if (patient.getHealthInsurance() != null) {
                patient.getHealthInsurance().setInstitution(institution);
            }
        }

        return patients;
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
            med.setValidFrom(now.minusYears(random.nextInt(1, 5)));
            med.setValidUntil(now.plusYears(random.nextInt(3, 10)));
            medications.add(med);
        }

        return medications;
    }

    private List<MedicationFavourite> createMedicationFavouritesForInstitution(Institution institution,
            List<Medication> medications) {
        List<MedicationFavourite> favourites = new ArrayList<>();
        if (institution == null || medications == null || medications.isEmpty()) {
            return favourites;
        }
        LocalDate validFrom = LocalDate.now().minusMonths(1);
        int favouritesCount = Math.min(3, medications.size());
        for (int i = 0; i < favouritesCount; i++) {
            Medication medication = medications.get(i);
            MedicationFavourite favourite = medicationFavouriteService.addOrReactivateFavourite(
                    institution.getId(),
                    medication.getId(),
                    medication.getArzneimittelbezeichnung(),
                    validFrom);
            favourites.add(favourite);
        }
        return favourites;
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
    private void createPastTimeSlotsWithUnapprovedTreatments(List<Patient> patients, SurgicalCenter center,
            MedicationFavourite medication, Diagnosis diagnosis) {

        // Erstelle einen TimeSlot der gerade abgelaufen ist
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SurgicalCenterTimeSlot pastTimeSlot = new SurgicalCenterTimeSlot()
                .setDate(yesterday)
                .setStartTime(LocalTime.of(14, 0))
                .setEndTime(LocalTime.of(16, 0))
                .setSurgicalCenter(center)
                .setDescription("Test TimeSlot für Tasks")
                .setAvailable(true) // Da wir den Slot für Treatments verwenden
                .setApproved(true);  // Der Slot selbst ist genehmigt

        // TimeSlot speichern und den gespeicherten Slot anhand der eindeutigen Felder ermitteln,
        // statt blind den ersten Eintrag zu verwenden
        SurgicalCenter savedCenter = surgicalCenterService
                .saveTimeSlotsAndSurgicalCenter(List.of(pastTimeSlot), center);
        pastTimeSlot = savedCenter.getAvailableTimeSlots().stream()
                .filter(ts -> ts.getDate().equals(yesterday)
                && ts.getStartTime().equals(LocalTime.of(14, 0))
                && ts.getEndTime().equals(LocalTime.of(16, 0)))
                .findFirst()
                .orElse(savedCenter.getAvailableTimeSlots().isEmpty() ? pastTimeSlot
                        : savedCenter.getAvailableTimeSlots().get(0));

        // Erstelle für jeden Patienten ein Treatment ohne ApprovalDate
        for (Patient patient : patients) {
            // CRITICAL: Set institution from patient.location.institution for data isolation
            if (patient.getLocation() == null || patient.getLocation().getInstitution() == null) {
                log.warn("Patient {} has no location with institution, skipping treatment plan", patient.getId());
                continue;
            }
            
            TreatmentPlan plan = new TreatmentPlan()
                    .setPatient(patient)
                    .setInstitution(patient.getLocation().getInstitution())
                    .setDescription("Testplan für unapproved Treatments")
                    .setDiagnosis(diagnosis)
                    .setCreationDate(LocalDate.now());

            Treatment treatment = new Treatment()
                    .setSurgicalCenterTimeSlot(pastTimeSlot)
                    .setMedicationFavourite(medication)
                    .setSideOfEye(SideOfEye.LEFT);  // Oder random zwischen LEFT/RIGHT
            // Kein ApprovalDate setzen, damit es als unapproved gilt

            treatment.setTreatmentPlan(plan);  // Bidirektionale Beziehung setzen
            plan.getTreatments().add(treatment);  // Behandlung zur Liste hinzufügen
            treatmentPlanService.saveTreatmentPlanInternal(plan);
        }
    }

    private List<SurgicalCenter> createSurgicalCentersWithTimeSlots(int count, Location location) {
        List<SurgicalCenter> centers = new ArrayList<>();
        Random random = new Random();

        // Ensure location is managed in current persistence context
        if (location.getId() == null) {
            throw new IllegalStateException("Location must be persisted before creating surgical centers");
        }
        Location managedLocation = locationService.findById(location.getId())
                .orElseThrow(() -> new IllegalStateException(
                "Location with ID " + location.getId() + " not found in database."));

        for (int i = 0; i < count; i++) {
            SurgicalCenter center = new SurgicalCenter();
            center.setName(SURGICAL_CENTER_NAMES[i % SURGICAL_CENTER_NAMES.length]);
            center.setDescription("Zentrum für intravitreale Injektionen");
            // Set institution from location
            center.setInstitution(managedLocation.getInstitution());

            Address address = new Address();
            address.setStreet(STREET_NAMES[random.nextInt(STREET_NAMES.length)] + " "
                    + (1 + random.nextInt(100)));
            address.setCity(CITIES[random.nextInt(CITIES.length)]);
            address.setPostalCode(10000 + random.nextInt(90000));
            address.setCountry(Locale.GERMANY);
            center.setAddress(address);

            // Generiere Zeitslots für Mittwoch und Freitag über 2 Jahre
            List<SurgicalCenterTimeSlot> timeSlots = generateTimeSlots(center);
            center.setAvailableTimeSlots(timeSlots);

            // Speichere das Zentrum und die Zeitslots
            centers.add(surgicalCenterService.saveTimeSlotsAndSurgicalCenter(timeSlots, center));
        }

        return centers;
    }

    /**
     * Generiert Zeitslots für Mittwoch und Freitag:
     * - 2 Jahre in die Vergangenheit
     * - 1 Jahr in die Zukunft
     * 7-9 Uhr
     */
    private List<SurgicalCenterTimeSlot> generateTimeSlots(SurgicalCenter center) {
        List<SurgicalCenterTimeSlot> timeSlots = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusYears(2); // 2 Jahre in die Vergangenheit
        LocalDate endDate = now.plusYears(1); // 1 Jahr in die Zukunft

        // Finde den ersten Mittwoch ab Startdatum
        LocalDate wednesday = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

        // Generiere Mittwoch-Slots von 2 Jahren Vergangenheit bis 1 Jahr Zukunft
        while (wednesday.isBefore(endDate) || wednesday.isEqual(endDate)) {
            SurgicalCenterTimeSlot wednesdaySlot = new SurgicalCenterTimeSlot();
            wednesdaySlot.setDate(wednesday);
            wednesdaySlot.setStartTime(LocalTime.of(7, 0));
            wednesdaySlot.setEndTime(LocalTime.of(9, 0));
            wednesdaySlot.setSurgicalCenter(center);
            // Slots in der Vergangenheit sind nicht mehr verfügbar
            wednesdaySlot.setAvailable(wednesday.isAfter(now) || wednesday.isEqual(now));
            wednesdaySlot.setApproved(true);
            wednesdaySlot.setDescription("Regulärer Mittwoch-Termin");
            timeSlots.add(wednesdaySlot);

            wednesday = wednesday.plusWeeks(1);
        }

        // Finde den ersten Freitag ab Startdatum
        LocalDate friday = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        // Generiere Freitag-Slots von 2 Jahren Vergangenheit bis 1 Jahr Zukunft
        while (friday.isBefore(endDate) || friday.isEqual(endDate)) {
            SurgicalCenterTimeSlot fridaySlot = new SurgicalCenterTimeSlot();
            fridaySlot.setDate(friday);
            fridaySlot.setStartTime(LocalTime.of(7, 0));
            fridaySlot.setEndTime(LocalTime.of(9, 0));
            fridaySlot.setSurgicalCenter(center);
            // Slots in der Vergangenheit sind nicht mehr verfügbar
            fridaySlot.setAvailable(friday.isAfter(now) || friday.isEqual(now));
            fridaySlot.setApproved(true);
            fridaySlot.setDescription("Regulärer Freitag-Termin");
            timeSlots.add(fridaySlot);

            friday = friday.plusWeeks(1);
        }

        return timeSlots;
    }

    /**
     * Erstellt Behandlungspläne mit zugehörigen Behandlungen für alle Patienten.
     * - Mindestens 4 Wochen zwischen Behandlungen
     * - Mehrere Patienten pro Zeitslot
     * - 20 Patienten in einem zukünftigen Zeitslot
     * - 30 Patienten in einem vergangenen Zeitslot
     */
    private void createTreatmentPlansWithTreatments(List<Patient> patients,
            List<MedicationFavourite> favourites,
            List<Diagnosis> diagnoses,
            List<SurgicalCenter> surgicalCenters) {
        Random random = new Random();
        LocalDate now = LocalDate.now();

        if (favourites.isEmpty()) {
            log.warn("Keine Medikamentenfavoriten verfügbar, überspringe Erstellung von Behandlungen");
            return;
        }

        // Wähle ein OP-Zentrum für alle Patienten
        SurgicalCenter center = surgicalCenters.get(random.nextInt(surgicalCenters.size()));
        
        // Lade alle Zeitslots des Zentrums neu, um sicherzustellen, dass sie aktuell sind
        SurgicalCenter reloadedCenter = surgicalCenterService.findByIdWithDetails(center.getId());
        List<SurgicalCenterTimeSlot> allSlots = reloadedCenter.getAvailableTimeSlots();

        // Finde einen vergangenen Zeitslot für 30 Patienten (z.B. vor 2 Wochen)
        LocalDate tempPastSlotDate = now.minusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY));
        final LocalDate pastSlotDate = tempPastSlotDate.isAfter(now) ? tempPastSlotDate.minusWeeks(1) : tempPastSlotDate;
        SurgicalCenterTimeSlot pastSlot = allSlots.stream()
                .filter(slot -> slot.getDate().equals(pastSlotDate))
                .findFirst()
                .orElse(null);
        
        if (pastSlot == null) {
            // Erstelle einen vergangenen Zeitslot falls nicht vorhanden
            pastSlot = new SurgicalCenterTimeSlot()
                    .setDate(pastSlotDate)
                    .setStartTime(LocalTime.of(7, 0))
                    .setEndTime(LocalTime.of(15, 0))
                    .setSurgicalCenter(reloadedCenter)
                    .setDescription("Vergangener Zeitslot für Testdaten")
                    .setAvailable(false)
                    .setApproved(true);
            reloadedCenter = surgicalCenterService.saveTimeSlotsAndSurgicalCenter(List.of(pastSlot), reloadedCenter);
            pastSlot = reloadedCenter.getAvailableTimeSlots().stream()
                    .filter(slot -> slot.getDate().equals(pastSlotDate))
                    .findFirst()
                    .orElseThrow();
        }

        // Finde einen zukünftigen Zeitslot für 20 Patienten (z.B. in 2 Wochen)
        LocalDate futureSlotDate = now.plusWeeks(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        SurgicalCenterTimeSlot futureSlot = allSlots.stream()
                .filter(slot -> slot.getDate().equals(futureSlotDate))
                .filter(SurgicalCenterTimeSlot::isAvailable)
                .findFirst()
                .orElse(null);
        
        if (futureSlot == null) {
            // Erstelle einen zukünftigen Zeitslot falls nicht vorhanden
            futureSlot = new SurgicalCenterTimeSlot()
                    .setDate(futureSlotDate)
                    .setStartTime(LocalTime.of(7, 0))
                    .setEndTime(LocalTime.of(15, 0))
                    .setSurgicalCenter(reloadedCenter)
                    .setDescription("Zukünftiger Zeitslot für Testdaten")
                    .setAvailable(true)
                    .setApproved(true);
            reloadedCenter = surgicalCenterService.saveTimeSlotsAndSurgicalCenter(List.of(futureSlot), reloadedCenter);
            futureSlot = reloadedCenter.getAvailableTimeSlots().stream()
                    .filter(slot -> slot.getDate().equals(futureSlotDate))
                    .findFirst()
                    .orElseThrow();
        }

        // Sammle verfügbare vergangene Slots (mindestens 4 Wochen auseinander)
        final LocalDate finalPastSlotDate = pastSlotDate;
        List<SurgicalCenterTimeSlot> availablePastSlots = allSlots.stream()
                .filter(slot -> slot.getDate().isBefore(now) && slot.getDate().isBefore(finalPastSlotDate))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // Neueste zuerst
                .collect(Collectors.toList());

        // Verteile Patienten auf Zeitslots
        int patientIndex = 0;
        
        // 30 Patienten für vergangenen Zeitslot (bereits behandelt)
        for (int i = 0; i < 30 && patientIndex < patients.size(); i++) {
            Patient patientFromList = patients.get(patientIndex++);
            createTreatmentPlanForPatient(patientFromList, pastSlot, favourites, diagnoses, random, true, true);
        }

        // 20 Patienten für zukünftigen Zeitslot
        for (int i = 0; i < 20 && patientIndex < patients.size(); i++) {
            Patient patientFromList = patients.get(patientIndex++);
            createTreatmentPlanForPatient(patientFromList, futureSlot, favourites, diagnoses, random, false, false);
        }

        // Restliche Patienten: Behandlungsreihenfolge mit mindestens 4 Wochen Abstand
        for (; patientIndex < patients.size(); patientIndex++) {
            Patient patientFromList = patients.get(patientIndex);
            
            // Reload patient from database
            if (patientFromList.getId() == null) {
                log.warn("Patient has no ID, skipping treatment plan");
                continue;
            }
            
            Patient patient = patientService.findEntityById(patientFromList.getId());
            if (patient == null) {
                log.warn("Patient {} not found in database, skipping treatment plan", patientFromList.getId());
                continue;
            }

            // Erstelle Behandlungsplan
            TreatmentPlan plan = new TreatmentPlan();
            plan.setCreationDate(now.minusMonths(6 + random.nextInt(6)));
            Diagnosis diagnosis = diagnoses.get(random.nextInt(diagnoses.size()));
            plan.setDiagnosis(diagnosis);
            plan.setDescription("Behandlungsplan für " + diagnosis.getName() + " - "
                    + patient.getFirstName() + " " + patient.getLastName());
            plan.setPatient(patient);
            
            if (patient.getLocation() != null && patient.getLocation().getInstitution() != null) {
                plan.setInstitution(patient.getLocation().getInstitution());
            } else {
                log.warn("Patient {} has no location with institution, skipping treatment plan", patient.getId());
                continue;
            }

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

            TreatmentPlan savedPlan;
            try {
                savedPlan = treatmentPlanService.saveTreatmentPlanInternal(plan);
            } catch (Exception e) {
                log.error("Fehler beim Speichern des Behandlungsplans: {}", e.getMessage(), e);
                continue;
            }

            MedicationFavourite preferredMedication = favourites.get(random.nextInt(favourites.size()));
            SideOfEye preferredSideOfEye = EYE_SIDES[random.nextInt(EYE_SIDES.length)];

            // Erstelle Behandlungen mit mindestens 4 Wochen Abstand
            LocalDate lastTreatmentDate = plan.getCreationDate();
            List<Treatment> treatments = new ArrayList<>();
            
            // Finde passende Slots mit mindestens 4 Wochen Abstand
            // Verwende eine Kopie für die Lambda-Ausdrücke
            LocalDate currentLastDate = lastTreatmentDate;
            List<SurgicalCenterTimeSlot> suitablePastSlots = new ArrayList<>();
            
            for (SurgicalCenterTimeSlot slot : availablePastSlots) {
                if (slot.getDate().isAfter(plan.getCreationDate()) 
                        && slot.getDate().isBefore(now)
                        && slot.getDate().isAfter(currentLastDate.plusWeeks(4))) {
                    suitablePastSlots.add(slot);
                    currentLastDate = slot.getDate();
                    if (suitablePastSlots.size() >= 5) { // Maximal 5 Behandlungen in der Vergangenheit
                        break;
                    }
                }
            }
            
            // Sortiere nach Datum
            suitablePastSlots.sort((a, b) -> a.getDate().compareTo(b.getDate()));

            for (SurgicalCenterTimeSlot slot : suitablePastSlots) {
                Treatment treatment = createTreatment(savedPlan, slot, preferredMedication, preferredSideOfEye, favourites, random, true);
                treatments.add(treatment);
                lastTreatmentDate = slot.getDate();
            }

            // Optional: Nächstmöglicher Termin (Ausnahme für nicht erschienene Patienten)
            if (random.nextDouble() < 0.2) { // 20% Wahrscheinlichkeit
                final LocalDate finalLastTreatmentDate = lastTreatmentDate;
                List<SurgicalCenterTimeSlot> nextSlots = allSlots.stream()
                        .filter(slot -> slot.getDate().isAfter(now) || slot.getDate().isEqual(now))
                        .filter(SurgicalCenterTimeSlot::isAvailable)
                        .filter(slot -> slot.getDate().isAfter(finalLastTreatmentDate.plusWeeks(4)))
                        .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                        .limit(1)
                        .collect(Collectors.toList());
                
                if (!nextSlots.isEmpty()) {
                    SurgicalCenterTimeSlot nextSlot = nextSlots.get(0);
                    Treatment treatment = createTreatment(savedPlan, nextSlot, preferredMedication, preferredSideOfEye, favourites, random, false);
                    treatments.add(treatment);
                }
            }

            if (!treatments.isEmpty()) {
                try {
                    treatmentPlanService.saveNewTreatmentsForExistingPlanInternal(treatments, savedPlan.getId());
                } catch (Exception e) {
                    log.error("Fehler beim Speichern der Behandlungen: {}", e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Erstellt einen Behandlungsplan für einen Patienten mit einem spezifischen Zeitslot
     */
    private void createTreatmentPlanForPatient(Patient patientFromList, SurgicalCenterTimeSlot slot,
            List<MedicationFavourite> favourites, List<Diagnosis> diagnoses, Random random,
            boolean isPast, boolean isApproved) {
        
        if (patientFromList.getId() == null) {
            log.warn("Patient has no ID, skipping treatment plan");
            return;
        }
        
        Patient patient = patientService.findEntityById(patientFromList.getId());
        if (patient == null) {
            log.warn("Patient {} not found in database, skipping treatment plan", patientFromList.getId());
            return;
        }

        TreatmentPlan plan = new TreatmentPlan();
        plan.setCreationDate(slot.getDate().minusMonths(6 + random.nextInt(6)));
        Diagnosis diagnosis = diagnoses.get(random.nextInt(diagnoses.size()));
        plan.setDiagnosis(diagnosis);
        plan.setDescription("Behandlungsplan für " + diagnosis.getName() + " - "
                + patient.getFirstName() + " " + patient.getLastName());
        plan.setPatient(patient);
        
        if (patient.getLocation() != null && patient.getLocation().getInstitution() != null) {
            plan.setInstitution(patient.getLocation().getInstitution());
        } else {
            log.warn("Patient {} has no location with institution, skipping treatment plan", patient.getId());
            return;
        }

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

        TreatmentPlan savedPlan;
        try {
            savedPlan = treatmentPlanService.saveTreatmentPlanInternal(plan);
        } catch (Exception e) {
            log.error("Fehler beim Speichern des Behandlungsplans: {}", e.getMessage(), e);
            return;
        }

        MedicationFavourite preferredMedication = favourites.get(random.nextInt(favourites.size()));
        SideOfEye preferredSideOfEye = EYE_SIDES[random.nextInt(EYE_SIDES.length)];

        Treatment treatment = createTreatment(savedPlan, slot, preferredMedication, preferredSideOfEye, favourites, random, isPast);
        
        // Für vergangene Behandlungen: Setze ApprovalDate wenn isApproved
        if (isPast && isApproved) {
            treatment.setApprovalDate(slot.getDate().minusDays(random.nextInt(5) + 1));
        }

        try {
            treatmentPlanService.saveNewTreatmentsForExistingPlanInternal(List.of(treatment), savedPlan.getId());
        } catch (Exception e) {
            log.error("Fehler beim Speichern der Behandlungen: {}", e.getMessage(), e);
        }
    }

    /**
     * Erstellt eine Behandlung für einen Zeitslot
     */
    private Treatment createTreatment(TreatmentPlan plan, SurgicalCenterTimeSlot slot, 
            MedicationFavourite preferredMedication, SideOfEye preferredSideOfEye,
            List<MedicationFavourite> favourites, Random random, boolean isPast) {
        Treatment treatment = new Treatment();
        treatment.setTreatmentPlan(plan);
        treatment.setSurgicalCenterTimeSlot(slot);

        // 80% Wahrscheinlichkeit für bevorzugte Seite
        if (random.nextDouble() < 0.8) {
            treatment.setSideOfEye(preferredSideOfEye);
        } else {
            treatment.setSideOfEye(EYE_SIDES[random.nextInt(EYE_SIDES.length)]);
        }

        // 90% Wahrscheinlichkeit für bevorzugtes Medikament
        if (random.nextDouble() < 0.9) {
            treatment.setMedicationFavourite(preferredMedication);
        } else {
            MedicationFavourite alternative = favourites.get(random.nextInt(favourites.size()));
            treatment.setMedicationFavourite(alternative);
        }

        // Verschiedene Bemerkungen
        String[] infoRemarks = {
            "Standardbehandlung",
            "Verlaufskontrolle",
            "Follow-up nach OCT",
            "Initiale Behandlungsphase",
            "Aufgrund von Makulaödem",
            "Nach Laserkoagulation"
        };
        treatment.setAdditionalInfo(infoRemarks[random.nextInt(infoRemarks.length)]);

        // Für Vergangenheitstermine: 70% approved, 30% unapproved (für TaskView)
        if (isPast) {
            if (random.nextDouble() < 0.7) {
                // Approved: ApprovalDate setzen
                treatment.setApprovalDate(slot.getDate().minusDays(random.nextInt(10) + 1));
            }
            // Unapproved: ApprovalDate bleibt null
        } else {
            // Zukunftstermine sind noch nicht approved
            // ApprovalDate bleibt null
        }

        return treatment;
    }
}
