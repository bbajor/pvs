package de.bbajor.pvs.surgicalcenter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterAddressDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterAddress;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterRepository;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;

class SurgicalCenterServiceTest {

    private SurgicalCenterTimeSlotRepository timeSlotRepository;
    private SurgicalCenterRepository surgicalCenterRepository;
    private SurgicalCenterMapper mapper;
    private SurgicalCenterService service;

    @BeforeEach
    void setUp() {
        timeSlotRepository = mock(SurgicalCenterTimeSlotRepository.class);
        surgicalCenterRepository = mock(SurgicalCenterRepository.class);
        mapper = mock(SurgicalCenterMapper.class);
        service = new SurgicalCenterService();
        service.timeSlotRepository = timeSlotRepository;
        service.surgicalCenterRepository = surgicalCenterRepository;
        service.mapper = mapper;
    }

    @Test
    void testFindAll() {
        List<SurgicalCenter> entities = List.of(new SurgicalCenter());
        List<SurgicalCenterDto> dtos = List.of(new SurgicalCenterDto());
        when(surgicalCenterRepository.findAll()).thenReturn(entities);
        when(mapper.toSurgicalCenterDtoList(entities)).thenReturn(dtos);

        List<SurgicalCenterDto> result = service.findAll();

        assertEquals(dtos, result);
        verify(surgicalCenterRepository).findAll();
        verify(mapper).toSurgicalCenterDtoList(entities);
    }

    @Test
    void testFindByIdWithDetails() {
        Integer id = 1;
        SurgicalCenter entity = mock(SurgicalCenter.class);
        SurgicalCenterDto dto = new SurgicalCenterDto();
        SurgicalCenterTimeSlot slot = new SurgicalCenterTimeSlot();
        SurgicalCenterTimeSlotDto slotDto = new SurgicalCenterTimeSlotDto();
        List<SurgicalCenterTimeSlot> slots = List.of(slot);

        when(surgicalCenterRepository.findByIdWithDetails(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);
        when(entity.getAvailableTimeSlots()).thenReturn(slots);
        when(mapper.toDto(slot)).thenReturn(slotDto);

        SurgicalCenterDto result = service.findByIdWithDetails(id);

        assertEquals(dto, result);
        assertEquals(List.of(slotDto), result.getAvailableTimeSlots());
        verify(surgicalCenterRepository).findByIdWithDetails(id);
        verify(mapper).toDto(entity);
        verify(mapper).toDto(slot);
    }

    @Test
    void testSaveSurgicalCenterDto() {
        SurgicalCenterDto dto = new SurgicalCenterDto();
        SurgicalCenter entity = new SurgicalCenter();
        SurgicalCenter saved = new SurgicalCenter();
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(surgicalCenterRepository.save(entity)).thenReturn(saved);

        SurgicalCenter result = service.saveSurgicalCenter(dto);

        assertEquals(saved, result);
        verify(mapper).toEntity(dto);
        verify(surgicalCenterRepository).save(entity);
    }

    @Test
    void testSaveSurgicalCenterEntityWithZeroId() {
        SurgicalCenter entity = new SurgicalCenter();
        entity.setId(0);
        SurgicalCenter saved = new SurgicalCenter();
        when(surgicalCenterRepository.save(any())).thenReturn(saved);

        SurgicalCenter result = service.saveSurgicalCenter(entity);

        assertEquals(saved, result);
        assertNull(entity.getId());
        verify(surgicalCenterRepository).save(entity);
    }

    @Test
    void testSaveSurgicalCenterEntityWithAddressZeroId() {
        SurgicalCenter entity = new SurgicalCenter();
        SurgicalCenterAddress address = new SurgicalCenterAddress();
        address.setId(0L);
        entity.setSurgicalCenterAddress(address);
        SurgicalCenter saved = new SurgicalCenter();
        when(surgicalCenterRepository.save(any())).thenReturn(saved);

        SurgicalCenter result = service.saveSurgicalCenter(entity);

        assertEquals(saved, result);
        assertNull(entity.getSurgicalCenterAddress().getId());
        verify(surgicalCenterRepository).save(entity);
    }

    @Test
    void testFindTimeSlotsBySurgicalCenterId_NotFound() {
        when(surgicalCenterRepository.findById(1)).thenReturn(Optional.empty());

        List<SurgicalCenterTimeSlot> result = service.findTimeSlotsBySurgicalCenterId(1);

        assertTrue(result.isEmpty());
        verify(surgicalCenterRepository).findById(1);
    }

    @Test
    void testFindTimeSlotsBySurgicalCenterId_Found() {
        SurgicalCenter center = new SurgicalCenter();
        List<SurgicalCenterTimeSlot> slots = List.of(new SurgicalCenterTimeSlot());
        when(surgicalCenterRepository.findById(1)).thenReturn(Optional.of(center));
        when(timeSlotRepository.findBySurgicalCenterAndDateGreaterThanEqual(eq(center), any(LocalDate.class)))
                .thenReturn(slots);

        List<SurgicalCenterTimeSlot> result = service.findTimeSlotsBySurgicalCenterId(1);

        assertEquals(slots, result);
        verify(surgicalCenterRepository).findById(1);
        verify(timeSlotRepository).findBySurgicalCenterAndDateGreaterThanEqual(eq(center), any(LocalDate.class));
    }

    @Test
    void testFindAvailableTimeSlotsFilteredBy_NullParams() {
        Collection<SurgicalCenterTimeSlotDto> result = service.findAvailableTimeSlotsFilteredBy(null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindAvailableTimeSlotsFilteredBy_AllCenters() {
        LocalDate now = LocalDate.now();
        TimePeriod period = mock(TimePeriod.class);
        LocalDate end = now.plusDays(7);
        List<SurgicalCenterTimeSlot> slots = List.of(new SurgicalCenterTimeSlot());
        List<SurgicalCenterTimeSlotDto> dtos = List.of(new SurgicalCenterTimeSlotDto());

        when(period.calculateEndDate(any())).thenReturn(end);
        when(timeSlotRepository.findByDateBetween(eq(now), eq(end), any(Sort.class))).thenReturn(slots);
        when(mapper.toTimeSlotDtoList(slots)).thenReturn(dtos);

        Collection<SurgicalCenterTimeSlotDto> result = service.findAvailableTimeSlotsFilteredBy(now, period, null);

        assertEquals(dtos, result);
    }

    @Test
    void testFindAvailableTimeSlotsFilteredBy_SpecificCenter() {
        LocalDate now = LocalDate.now();
        TimePeriod period = mock(TimePeriod.class);
        LocalDate end = now.plusDays(7);
        SurgicalCenter center = new SurgicalCenter();
        List<SurgicalCenterTimeSlot> slots = List.of(new SurgicalCenterTimeSlot());
        List<SurgicalCenterTimeSlotDto> dtos = List.of(new SurgicalCenterTimeSlotDto());

        when(period.calculateEndDate(any())).thenReturn(end);
        when(surgicalCenterRepository.getReferenceById(1)).thenReturn(center);
        when(timeSlotRepository.findByDateBetweenAndSurgicalCenter(eq(now), eq(end), eq(center), any(Sort.class)))
                .thenReturn(slots);
        when(mapper.toTimeSlotDtoList(slots)).thenReturn(dtos);

        Collection<SurgicalCenterTimeSlotDto> result = service.findAvailableTimeSlotsFilteredBy(now, period, 1);

        assertEquals(dtos, result);
    }

    @Test
    void testGetSurgicalCenters() {
        SurgicalCenter center = new SurgicalCenter();
        SurgicalCenterDto dto = new SurgicalCenterDto();
        List<SurgicalCenter> centers = List.of(center);
        List<SurgicalCenterTimeSlotDto> slotDtos = List.of(new SurgicalCenterTimeSlotDto());

        when(surgicalCenterRepository.findAll()).thenReturn(centers);
        when(mapper.toDto(center)).thenReturn(dto);
        when(timeSlotRepository.findBySurgicalCenter(center)).thenReturn(List.of());
        when(mapper.toTimeSlotDtoList(anyList())).thenReturn(slotDtos);

        List<SurgicalCenterDto> result = service.getSurgicalCenters();

        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
        assertEquals(slotDtos, result.get(0).getAvailableTimeSlots());
    }

    @Test
    void testSaveTimeSlotsAndSurgicalCenter_Entities() {
        SurgicalCenter center = new SurgicalCenter();
        center.setId(1);
        SurgicalCenter saved = new SurgicalCenter();
        saved.setId(1);
        List<SurgicalCenterTimeSlot> slots = List.of(new SurgicalCenterTimeSlot());

        when(surgicalCenterRepository.save(center)).thenReturn(saved);
        when(surgicalCenterRepository.getReferenceById(1)).thenReturn(saved);

        service.saveTimeSlotsAndSurgicalCenter(slots, center);

        for (SurgicalCenterTimeSlot slot : slots) {
            assertEquals(saved, slot.getSurgicalCenter());
        }
        verify(timeSlotRepository).saveAll(slots);
    }

    @Test
    void testSaveTimeSlotsAndSurgicalCenter_Dtos_NullDto() {
        service.saveTimeSlotsAndSurgicalCenter(new ArrayList<SurgicalCenterTimeSlotDto>(), null);
        // Should not throw
    }

    @Test
    void testSaveTimeSlotsAndSurgicalCenter_Dtos_WithAddress() {
        SurgicalCenterDto dto = new SurgicalCenterDto();
        SurgicalCenterTimeSlotDto slotDto = new SurgicalCenterTimeSlotDto();
        SurgicalCenter entity = new SurgicalCenter();
        SurgicalCenter saved = new SurgicalCenter();
        dto.setSurgicalCenterAddress(new SurgicalCenterAddressDto());

        when(mapper.toEntity(dto)).thenReturn(entity);
        when(mapper.toEntity(any(SurgicalCenterAddressDto.class)))
                .thenReturn(new SurgicalCenterAddress());
        when(mapper.toEntity(slotDto)).thenReturn(new SurgicalCenterTimeSlot());
        when(surgicalCenterRepository.save(entity)).thenReturn(saved);

        service.saveTimeSlotsAndSurgicalCenter(List.of(slotDto), dto);

        verify(surgicalCenterRepository).save(entity);
    }

    @Test
    void testGetTimeSlotsBySurgicalCenterIdWithTreatmentCount() {
        List<SurgicalCenterTimeSlotDto> dtos = List.of(new SurgicalCenterTimeSlotDto());
        when(timeSlotRepository.findBySurgicalCenterIdWithTreatmentCount(1)).thenReturn(dtos);

        List<SurgicalCenterTimeSlotDto> result = service.getTimeSlotsBySurgicalCenterIdWithTreatmentCount(1);

        assertEquals(dtos, result);
        verify(timeSlotRepository).findBySurgicalCenterIdWithTreatmentCount(1);
    }
}