package de.bbajor.pvs.surgicalcenter.service;

import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.patient.dto.HealthInsuranceDto;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterAddressDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterAddress;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SurgicalCenterMapperTest {

    private SurgicalCenterMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(SurgicalCenterMapper.class);
    }

    @Test
    void testToDto() {
        SurgicalCenter entity = new SurgicalCenter();
        entity.setId(1);
        entity.setName("Center A");
        SurgicalCenterDto dto = mapper.toDto(entity);
        assertNotNull(dto);
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getName(), dto.getName());
    }

    @Test
    void testToEntity() {
        SurgicalCenterDto dto = new SurgicalCenterDto();
        dto.setId(2);
        dto.setName("Center B");
        SurgicalCenter entity = mapper.toEntity(dto);
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getName(), entity.getName());
    }

    @Test
    void testAddressToDtoAndBack() {
        SurgicalCenterAddress address = new SurgicalCenterAddress();
        address.setId(3L);
        address.setStreet("Main St");
        SurgicalCenterAddressDto dto = mapper.toDto(address);
        assertNotNull(dto);
        assertEquals(address.getId(), dto.getId());
        assertEquals(address.getStreet(), dto.getStreet());

        SurgicalCenterAddress entity = mapper.toEntity(dto);
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getStreet(), entity.getStreet());
    }

    @Test
    void testTimeSlotToDtoAndBack() {
        SurgicalCenterTimeSlot slot = new SurgicalCenterTimeSlot();
        slot.setId(4L);
        SurgicalCenterTimeSlotDto dto = mapper.toDto(slot);
        assertNotNull(dto);
        assertEquals(slot.getId(), dto.getId());

        SurgicalCenterTimeSlot entity = mapper.toEntity(dto);
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
    }

    @Test
    void testToTimeSlotDtoList() {
        SurgicalCenterTimeSlot slot1 = new SurgicalCenterTimeSlot();
        slot1.setId(5L);
        SurgicalCenterTimeSlot slot2 = new SurgicalCenterTimeSlot();
        slot2.setId(6L);
        List<SurgicalCenterTimeSlotDto> dtos = mapper.toTimeSlotDtoList(Arrays.asList(slot1, slot2));
        assertEquals(2, dtos.size());
        assertEquals(slot1.getId(), dtos.get(0).getId());
        assertEquals(slot2.getId(), dtos.get(1).getId());
    }

    @Test
    void testToHealthInsuranceDtoList() {
        HealthInsurance hi = new HealthInsurance();
        hi.setId(7);
        hi.setBillingCarrierName("Insurance X");
        List<HealthInsuranceDto> dtos = mapper.toHealthInsuranceDtoList(Collections.singletonList(hi));
        assertEquals(1, dtos.size());
        assertEquals(hi.getId(), dtos.get(0).getId());
        assertEquals(hi.getBillingCarrierName(), dtos.get(0).getBillingCarrierName());
    }

    @Test
    void testToSurgicalCenterDtoList() {
        SurgicalCenter sc = new SurgicalCenter();
        sc.setId(8);
        sc.setName("Center C");
        List<SurgicalCenterDto> dtos = mapper.toSurgicalCenterDtoList(Collections.singletonList(sc));
        assertEquals(1, dtos.size());
        assertEquals(sc.getId(), dtos.get(0).getId());
        assertEquals(sc.getName(), dtos.get(0).getName());
    }

    @Test
    void testToMedicationDtoList() {
        Medication med = new Medication();
        med.setId(9L);
        med.setArzneimittelbezeichnung("Med A");
        List<MedicationDto> dtos = mapper.toMedicationDtoList(Collections.singletonList(med));
        assertEquals(1, dtos.size());
        assertEquals(med.getId(), dtos.get(0).getId());
        assertEquals(med.getArzneimittelbezeichnung(), dtos.get(0).getArzneimittelbezeichnung());
    }
}