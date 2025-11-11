package de.bbajor.pvs.institution.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.repository.LocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.init.testdata.enabled=false")
class InstitutionFilterIntegrationTest {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        InstitutionFilter.disableFilter(entityManager);
        InstitutionContext.clear();
    }

    @Test
    void locationQueriesAreRestrictedToCurrentInstitution() {
        Institution inst1 = new Institution()
                .setInstitutionCode("INST-AUTO1")
                .setInstitutionName("Test Institution 1")
                .setActive(true)
                .setDatabaseName("pvs_inst_auto1")
                .setContainerName("postgres-inst-auto1");
        Institution inst2 = new Institution()
                .setInstitutionCode("INST-AUTO2")
                .setInstitutionName("Test Institution 2")
                .setActive(true)
                .setDatabaseName("pvs_inst_auto2")
                .setContainerName("postgres-inst-auto2");

        inst1 = institutionRepository.save(inst1);
        inst2 = institutionRepository.save(inst2);
        Long inst1Id = inst1.getId();
        Long inst2Id = inst2.getId();

        Location loc1 = new Location();
        loc1.setInstitution(inst1);
        loc1.setLocationName("Standort 1");

        Location loc2 = new Location();
        loc2.setInstitution(inst2);
        loc2.setLocationName("Standort 2");

        locationRepository.saveAll(List.of(loc1, loc2));
        entityManager.flush();
        entityManager.clear();

        // When InstitutionContext is set to inst1
        InstitutionContext.setInstitutionId(inst1Id);
        InstitutionFilter.enableFilter(entityManager);
        List<Location> locationsForInst1 = locationRepository.findAll();

        assertThat(locationsForInst1)
                .hasSize(1)
                .allMatch(location -> location.getInstitution().getId().equals(inst1Id));

        // Switch to inst2
        InstitutionFilter.disableFilter(entityManager);
        InstitutionContext.setInstitutionId(inst2Id);
        InstitutionFilter.enableFilter(entityManager);
        List<Location> locationsForInst2 = locationRepository.findAll();

        assertThat(locationsForInst2)
                .hasSize(1)
                .allMatch(location -> location.getInstitution().getId().equals(inst2Id));
    }
}
