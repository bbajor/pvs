package de.bbajor.pvs.patient.service;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.patient.dto.HealthInsuranceDto;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.dto.PatientHistoryDto;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.model.PatientHistory;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "salutation", expression = "java(Salutation.byString(patient.getSalutation()))")
    @Mapping(target = "title", expression = "java(Title.byString(patient.getTitle()))")
    PatientDto toDto(Patient patient);

    PatientHistory toEntity(PatientHistoryDto dto);

    PatientHistoryDto toDto(PatientHistory entity);

    Patient toEntity(PatientDto dto);

    AddressDto toDto(Address address);

    Address toEntity(AddressDto dto);

    HealthInsuranceDto toDto(HealthInsurance entity);

    HealthInsurance toEntity(HealthInsuranceDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromEntity(Patient source, @MappingTarget Patient target);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(PatientDto dto, @MappingTarget Patient entity);

    void updateDtoFromEntity(Patient entity, @MappingTarget PatientDto dto);

    void updateDto(PatientDto patient, @MappingTarget PatientDto target);

    List<HealthInsuranceDto> toHealthInsuranceDtoList(List<HealthInsurance> entities);

    List<PatientDto> toPatientDtoList(List<Patient> all);
}
