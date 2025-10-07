package de.bbajor.pvs.surgicalcenter.service;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

@Mapper(componentModel = "spring")
public interface SurgicalCenterMapper {

    @Mapping(target = "availableTimeSlots", ignore = true)
    SurgicalCenterDto toDto(SurgicalCenter surgicalCenter);

    @Mapping(target = "availableTimeSlots", ignore = true)
    SurgicalCenter toEntity(SurgicalCenterDto dto);

    SurgicalCenterAddressDto toDto(SurgicalCenterAddress address);

    @Mapping(target = "locale", ignore = true)
    SurgicalCenterAddress toEntity(SurgicalCenterAddressDto dto);

    @Mapping(target = "surgicalCenter", ignore = true)
    SurgicalCenterTimeSlot toEntity(SurgicalCenterTimeSlotDto dto);

    @Mapping(target = "surgicalCenter", ignore = true)
    SurgicalCenterTimeSlotDto toDto(SurgicalCenterTimeSlot entity);

    @Mapping(target = "surgicalCenter", ignore = true)
    List<SurgicalCenterTimeSlotDto> toTimeSlotDtoList(List<SurgicalCenterTimeSlot> entityList);

    List<HealthInsuranceDto> toHealthInsuranceDtoList(List<HealthInsurance> all);

    List<SurgicalCenterDto> toSurgicalCenterDtoList(List<SurgicalCenter> all);

    List<MedicationDto> toMedicationDtoList(Collection<Medication> medicationList);

}