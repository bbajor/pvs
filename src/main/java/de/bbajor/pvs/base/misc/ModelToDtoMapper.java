package de.bbajor.pvs.base.misc;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.boot.context.properties.bind.Name;

import de.bbajor.pvs.base.domain.Address;
import de.bbajor.pvs.base.domain.HealthInsurance;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.domain.PatientHistory;
import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomdrug.model.IvomDrug;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitAddressDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.model.IvomPlan;
import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitAddress;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.dto.PatientHistoryDto;

@Mapper(componentModel = "spring")
public interface ModelToDtoMapper {

    PatientDto toDto(Patient patient);

    PatientHistory toEntity(PatientHistoryDto dto);

    PatientHistoryDto toDto(PatientHistory entity);

    Patient toEntity(PatientDto dto);

    @Mapping(target = "locale", source = "locale")
    AddressDto toDto(Address address);

    @Mapping(target = "locale", source = "locale")
    Address toEntity(AddressDto dto);

    HealthInsuranceDto toDto(HealthInsurance entity);

    HealthInsurance toEntity(HealthInsuranceDto dto);

    void updateEntityFromDto(PatientDto dto, @MappingTarget Patient entity);

    void updateDtoFromEntity(Patient entity, @MappingTarget PatientDto dto);

    void updateDto(PatientDto patient, @MappingTarget PatientDto target);

    IvomDrugDto toDto(IvomDrug entity);

    IvomPlan toEntity(IvomPlanDto dto);

    void updateEntityFromDto(IvomPlanDto workingCopy, @MappingTarget IvomPlan original);

    IvomPlanDto toDto(IvomPlan e);

    // @Mapping(target = "availableTimeSlots", ignore = true)
    SurgeryUnitDto toDto(SurgeryUnit surgeryUnit);

    @Named("fullMapping")
    SurgeryUnitDto toDtoWithTimeSlots(SurgeryUnit surgeryUnit);

    SurgeryUnit toEntity(SurgeryUnitDto dto);

    @Mapping(target = "locale", source = "locale")
    SurgeryUnitAddressDto toDto(SurgeryUnitAddress address);

    @Mapping(target = "locale", source = "locale")
    SurgeryUnitAddress toEntity(SurgeryUnitAddressDto dto);

    SurgeryUnitTimeSlot toEntity(SurgeryUnitTimeSlotDto dto);

    SurgeryUnitTimeSlotDto toDto(SurgeryUnitTimeSlot entity);

}