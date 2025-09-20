package de.bbajor.pvs.base.misc;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import de.bbajor.pvs.base.domain.Address;
import de.bbajor.pvs.base.domain.HealthInsurance;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.domain.PatientHistory;
import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.dto.PatientHistoryDto;

@Mapper(componentModel = "spring")
public interface ModelToDtoMapper {

    ModelToDtoMapper INSTANCE = Mappers.getMapper(ModelToDtoMapper.class);

    PatientDto toDto(Patient patient);

    PatientHistory toEntity(PatientHistoryDto dto);

    PatientHistoryDto toDto(PatientHistory entity);

    Patient toEntity(PatientDto dto);

    AddressDto toDto(Address address);

    Address toEntity(AddressDto dto);

    HealthInsuranceDto toDto(HealthInsurance entity);

    HealthInsurance toEntity(HealthInsuranceDto dto);

        // Copy/Update: dto -> bestehende Entity
    void updateEntityFromDto(PatientDto dto, @MappingTarget Patient entity);

    // Genauso auch umgekehrt, falls du es brauchst:
    void updateDtoFromEntity(Patient entity, @MappingTarget PatientDto dto);
}