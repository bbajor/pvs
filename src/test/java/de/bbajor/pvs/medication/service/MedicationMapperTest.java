package de.bbajor.pvs.medication.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.model.Medication;

class MedicationMapperTest {

    private MedicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(MedicationMapper.class);
    }

    @Test
    void testToDto() {
        LocalDate now = LocalDate.now();
        Medication entity = new Medication();
        entity.setArzneimittelbezeichnung("Lucentis");
        entity.setDarreichungsform("Injektionslösung");
        entity.setWirkstoffe("Ranibizumab");
        entity.setIndikationAtc("nAMD");
        entity.setAnwendungsart("intravitreal");
        entity.setDescription("10mg/ml Injektionslösung");
        entity.setZulassungsinhaber("Novartis Pharma");
        entity.setFavourite(true);
        entity.setValidFrom(now);
        entity.setValidUntil(now.plusYears(2));

        MedicationDto dto = mapper.toMedicationDto(entity);

        assertNotNull(dto);
        assertEquals("Lucentis", dto.getArzneimittelbezeichnung());
        assertEquals("Injektionslösung", dto.getDarreichungsform());
        assertEquals("Ranibizumab", dto.getWirkstoffe());
        assertEquals("nAMD", dto.getIndikationAtc());
        assertEquals("intravitreal", dto.getAnwendungsart());
        assertEquals("10mg/ml Injektionslösung", dto.getDescription());
        assertEquals("Novartis Pharma", dto.getZulassungsinhaber());
        assertTrue(dto.isFavourite());
        assertEquals(now, dto.getValidFrom());
        assertEquals(now.plusYears(2), dto.getValidUntil());
    }

    @Test
    void testUpdateEntityFromDto() {
        LocalDate now = LocalDate.now();
        MedicationDto dto = new MedicationDto();
        dto.setArzneimittelbezeichnung("Beovu");
        dto.setDarreichungsform("Injektionslösung");
        dto.setWirkstoffe("Brolucizumab");
        dto.setIndikationAtc("DMÖ");
        dto.setValidFrom(now);
        dto.setValidUntil(now.plusYears(1));
        dto.setDescription("120mg/ml Injektionslösung");

        Medication entity = new Medication();
        entity.setId(1L);
        entity.setVersion(2L);
        entity.setArzneimittelbezeichnung("OldName");
        entity.setWirkstoffe("OldWirkstoff");
        entity.setValidFrom(now);
        entity.setValidUntil(now.plusYears(1));

        mapper.updateEntityFromDto(dto, entity);

        assertEquals(1L, entity.getId());
        assertEquals(2L, entity.getVersion());
        assertEquals("Beovu", entity.getArzneimittelbezeichnung());
        assertEquals("Injektionslösung", entity.getDarreichungsform());
        assertEquals("Brolucizumab", entity.getWirkstoffe());
        assertEquals("DMÖ", entity.getIndikationAtc());
        assertEquals("120mg/ml Injektionslösung", entity.getDescription());
        assertEquals(now, entity.getValidFrom());
        assertEquals(now.plusYears(1), entity.getValidUntil());
    }

    @Test
    void testToMedicationDtoList() {
        Medication med1 = new Medication();
        med1.setArzneimittelbezeichnung("Eylea");
        med1.setWirkstoffe("Aflibercept");
        med1.setIndikationAtc("nAMD");

        Medication med2 = new Medication();
        med2.setArzneimittelbezeichnung("Lucentis");
        med2.setWirkstoffe("Ranibizumab");
        med2.setIndikationAtc("DMÖ");

        List<Medication> meds = Arrays.asList(med1, med2);

        List<MedicationDto> dtos = mapper.toMedicationDtoList(meds);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Eylea", dtos.get(0).getArzneimittelbezeichnung());
        assertEquals("Lucentis", dtos.get(1).getArzneimittelbezeichnung());
        assertEquals("Aflibercept", dtos.get(0).getWirkstoffe());
        assertEquals("Ranibizumab", dtos.get(1).getWirkstoffe());
    }
}