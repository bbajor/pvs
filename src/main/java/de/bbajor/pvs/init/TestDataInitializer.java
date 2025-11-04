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
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
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
    private final SurgicalCenterService surgicalCenterService;
    private final IvomDiagnosisService diagnosisService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;

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

        // Initialisiere zuerst Test-Tenants und Tenant-spezifische User
        // Erstelle auch die Location für DEV-TEST Tenant
        Location defaultLocation = initTestTenants();

        // Flush, um sicherzustellen, dass alle Tenants und Locations persistiert sind,
        // bevor wir sie später verwenden
        entityManager.flush();
        entityManager.clear(); // Clear session, um alle Objekte zu detachen

        // Erstelle Testuser aus SampleUsers
        createTestUsers();

        // CRITICAL: Ensure Institution and Location exist BEFORE creating patients
        // This ensures patients can be assigned to a location with institution
        // Location is already created in initTestTenants()
        log.info("Default location: {} (institution: {})",
                defaultLocation.getLocationName(),
                defaultLocation.getInstitution().getInstitutionCode());

        // CRITICAL: Set InstitutionContext for test data initialization
        // This is required for patient service methods that check institution context
        InstitutionContext.setInstitutionId(defaultLocation.getInstitution().getId());
        log.debug("InstitutionContext set to: {} for test data initialization", defaultLocation.getInstitution().getId());

        List<Patient> savedPatients = patientService.saveAll(createRealisticPatients(20));
        List<Medication> savedMedications = medicationService
                .saveAll(createRealisticMedications(MEDICATION_NAMES.length));
        List<Diagnosis> diagnosisDtos = diagnosisService.saveAll(createDiagnoses());

        // Erzeuge OP-Zentren mit Zeitslots für Mittwoch und Freitag
        List<SurgicalCenter> surgicalCenters = createSurgicalCentersWithTimeSlots(
                SURGICAL_CENTER_NAMES.length, defaultLocation);

        // Erzeuge 10 Behandlungspläne mit Behandlungen
        createTreatmentPlansWithTreatments(10, savedPatients, savedMedications, diagnosisDtos, surgicalCenters);

        // Erstelle einen abgelaufenen TimeSlot mit nicht genehmigten Behandlungen für Task-Testing
        createPastTimeSlotsWithUnapprovedTreatments(savedPatients.subList(0, 3), surgicalCenters.get(0),
                savedMedications.get(0), diagnosisDtos.get(0));
        
        // Clear InstitutionContext after initialization to avoid side effects
        // The context will be set properly when users log in
        InstitutionContext.clear();
        log.debug("InstitutionContext cleared after test data initialization");
    }

    /**
     * Initialisiert Test-Tenants für Multi-Tenancy und erstellt die Location
     * für DEV-TEST
     *
     * @return die Location für DEV-TEST Tenant für spätere Verwendung
     */
    private Location initTestTenants() {
        // Create default test tenant
        Institution testInstitution = createInstitutionIfNotExists(
                "DEV-TEST",
                "Test-Praxis (Dev)",
                "Standard-Test-Praxis für Entwicklung"
        );

        // Create sample tenants
        Institution institution1 = createInstitutionIfNotExists(
                "PRAX-001",
                "Augenarztpraxis Dr. Müller",
                "Praxis in Berlin"
        );

        Institution institution2 = createInstitutionIfNotExists(
                "PRAX-002",
                "MVZ Augenheilkunde Hamburg",
                "Medizinisches Versorgungszentrum"
        );

        // Create Location for DEV-TEST tenant
        Location defaultLocation = createLocationForInstitution(testInstitution);

        // Create super admin (no tenant, can manage all tenants/locations)
        createInstitutionUserIfNotExists(
                null,
                "superadmin",
                "admin@pvs.local",
                "Super Administrator",
                Set.of("SUPER_ADMIN", "ADMIN", "USER")
        );

        // Create test users for each tenant
        createInstitutionUserIfNotExists(
                testInstitution,
                "testadmin",
                "testadmin@test.local",
                "Test Admin",
                Set.of("ADMIN", "USER")
        );

        // Create "admin" user (matching SampleUsers.ADMIN_USERNAME) for DEV-TEST tenant
        createInstitutionUserIfNotExists(
                testInstitution,
                "admin",
                "alice@example.com",
                "Alice Administrator",
                Set.of("ADMIN", "OWNER", "USER", "DOCTOR")
        );

        // Create "user" user (matching SampleUsers.USER_USERNAME) for DEV-TEST tenant
        createInstitutionUserIfNotExists(
                testInstitution,
                "user",
                "ursula@example.com",
                "Ursula User",
                Set.of("USER", "TECH_USER", "MEDICAL_STAFF")
        );

        // Create users for other tenants (they would need their own locations)
        createInstitutionUserIfNotExists(
                institution1,
                "dr.mueller",
                "mueller@praxis.local",
                "Dr. Müller",
                Set.of("ADMIN", "USER")
        );

        createInstitutionUserIfNotExists(
                institution2,
                "dr.schmidt",
                "schmidt@mvz.local",
                "Dr. Schmidt",
                Set.of("ADMIN", "USER")
        );

        // Note: tenant1 and tenant2 would need their own locations for users
        // For now, we only create location for DEV-TEST
        return defaultLocation;
    }

    /**
     * Erstellt eine Location für eine Institution, falls noch nicht vorhanden
     */
    private Location createLocationForInstitution(Institution institution) {
        // Ensure Institution is persisted
        if (institution.getId() == null) {
            institution = institutionRepository.save(institution);
        }

        // Check if location already exists for this institution
        List<Location> existingLocations = locationService.findByInstitution(institution);
        if (!existingLocations.isEmpty()) {
            return existingLocations.get(0);
        }

        // Create new location
        Location location = new Location();
        location.setLocationName("Augenarztpraxis Muster");
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
    private List<Patient> createRealisticPatients(int count) {
        Random random = new Random();

        return Instancio.ofList(Patient.class)
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
    private void createPastTimeSlotsWithUnapprovedTreatments(List<Patient> patients, SurgicalCenter center,
            Medication medication, Diagnosis diagnosis) {

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
                    .setMedication(medication)
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
     * Generiert Zeitslots für Mittwoch und Freitag für die nächsten 2 Jahre,
     * 7-9 Uhr
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
     * Erstellt Behandlungspläne mit zugehörigen Behandlungen Verbesserte
     * Version mit größerer Variabilität
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
            Patient patientFromList = selectedPatients.get(i);

            // Stelle sicher, dass der Patient eine ID hat (also gespeichert ist)
            if (patientFromList.getId() == null) {
                log.warn("Patient has no ID, skipping treatment plan");
                continue; // Überspringe diesen Patienten
            }
            
            // Reload patient from database to ensure Location and Institution are loaded
            Patient patient = patientService.findEntityById(patientFromList.getId());
            if (patient == null) {
                log.warn("Patient {} not found in database, skipping treatment plan", patientFromList.getId());
                continue;
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
            
            // CRITICAL: Set institution from patient.location.institution for data isolation
            if (patient.getLocation() != null && patient.getLocation().getInstitution() != null) {
                plan.setInstitution(patient.getLocation().getInstitution());
            } else {
                log.warn("Patient {} has no location with institution, skipping treatment plan", patient.getId());
                continue; // Überspringe diesen Plan
            }

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

            // Speichere den Behandlungsplan (nutze interne Methode ohne Security-Check für Testdaten)
            TreatmentPlan savedPlan;
            try {
                savedPlan = treatmentPlanService.saveTreatmentPlanInternal(plan);
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
                                    .isAfter(planCreationDate.plusDays(treatmentIndex * 14))
                                    && slot.getDate()
                                            .isBefore(planCreationDate.plusDays(treatmentIndex * 14 + 10))
                                    && // Für historische Slots (vor heute) muss available nicht geprüft werden
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
                    treatmentPlanService.saveNewTreatmentsForExistingPlanInternal(treatments,
                            savedPlan.getId());
                } catch (Exception e) {
                    System.out.println("Fehler beim Speichern der Behandlungen: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
