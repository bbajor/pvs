package de.bbajor.pvs.medication.service;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.model.Medication;

@Mapper(componentModel = "spring")
public interface MedicationMapper {

    Medication toEntity(MedicationDto bean);

    MedicationDto toDto(Medication entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(MedicationDto dto, @MappingTarget Medication entity);

    List<MedicationDto> toMedicationDtoList(Collection<Medication> allByIsFavouriteTrue);
}
