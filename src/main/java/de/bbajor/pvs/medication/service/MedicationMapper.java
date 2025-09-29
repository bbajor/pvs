package de.bbajor.pvs.medication.service;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.model.IntravitrealMedication;

@Mapper(componentModel = "spring")
public interface MedicationMapper {

    IntravitrealMedication toEntity(IntravitrealMedicationDto bean);

    IntravitrealMedicationDto toDto(IntravitrealMedication entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(IntravitrealMedicationDto dto, @MappingTarget IntravitrealMedication entity);

    List<IntravitrealMedicationDto> toMedicationDtoList(Collection<IntravitrealMedication> allByIsFavouriteTrue);
}
