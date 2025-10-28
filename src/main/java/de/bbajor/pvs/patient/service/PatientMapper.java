package de.bbajor.pvs.patient.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.patient.model.Patient;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    // @Mapping(target = "salutation", expression =
    // "java(Salutation.byString(patient.getSalutation()))")
    // @Mapping(target = "title", expression =
    // "java(Title.byString(patient.getTitle()))")

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updatePatientEntity(Patient source, @MappingTarget Patient target);

    // Defensive null-safe mapping helpers can be added here later if needed

}
