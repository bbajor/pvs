package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;

@Mapper(componentModel = "spring")
public interface TreatmentPlanMapper {

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    TreatmentPlan toEntity(TreatmentPlanDto dto);

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    void updateEntityFromDto(TreatmentPlanDto workingCopy, @MappingTarget TreatmentPlan original);

    @Mapping(target = "treatments", ignore = true)
    TreatmentPlanDto toDto(TreatmentPlan e);

    List<TreatmentPlanDto> toTreatmentPlanDtoList(List<TreatmentPlan> treatmentPlanList);

    List<TreatmentPlan> toTreatmentPlanEntityList(List<TreatmentPlanDto> treatmentPlanDtoList);

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    Treatment toEntity(TreatmentDto dto);

    @Mapping(target = "treatmentPlan", ignore = true)
    TreatmentDto toDto(Treatment entity);

    List<Treatment> toTreatmentEntityList(List<TreatmentDto> treatmentDtoList);

    List<TreatmentDto> toTreatmentDtoList(List<Treatment> treatmentList);

    Diagnosis toEntity(DiagnosisDto dto);

    DiagnosisDto toDto(Diagnosis entity);

    void updateEntityFromDto(DiagnosisDto dto, @MappingTarget Diagnosis entity);

    Collection<DiagnosisDto> toDiagnosisDtoList(List<Diagnosis> entityList);
}
