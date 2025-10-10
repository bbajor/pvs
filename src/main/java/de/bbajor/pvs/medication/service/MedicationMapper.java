package de.bbajor.pvs.medication.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.medication.model.Medication;

@Mapper(componentModel = "spring")
public interface MedicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateMedication(Medication source, @MappingTarget Medication target);

}
