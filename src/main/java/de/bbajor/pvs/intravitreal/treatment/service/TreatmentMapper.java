package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.base.BaseMapperConfig;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.service.PatientMapper;

@Mapper(componentModel = "spring", uses = { PatientMapper.class,
        TreatmentPlanMapper.class }, config = BaseMapperConfig.class)
public interface TreatmentMapper {

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    @Mapping(target = "treatmentPlan", ignore = true)
    @Mapping(target = "medication", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateTreatmentEntityFromDto(TreatmentDto source, @MappingTarget Treatment target);

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    @Mapping(target = "treatmentPlan", ignore = true)
    @Mapping(target = "medication", ignore = true)
    TreatmentDto toTreatmentDto(Treatment entity);

    List<Treatment> toTreatmentEntityList(List<TreatmentDto> treatmentDtoList);
}
