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
import de.bbajor.pvs.patient.service.PatientMapper;

@Mapper(componentModel = "spring", uses = {PatientMapper.class})
public interface TreatmentPlanMapper {

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    TreatmentPlan toEntity(TreatmentPlanDto dto);

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    void updateEntityFromDto(TreatmentPlanDto source, @MappingTarget TreatmentPlan target);

    @Mapping(target = "treatments", ignore = true)
    TreatmentPlanDto toDto(TreatmentPlan e);

    List<TreatmentPlanDto> toTreatmentPlanDtoList(List<TreatmentPlan> treatmentPlanList);

    List<TreatmentPlan> toTreatmentPlanEntityList(List<TreatmentPlanDto> treatmentPlanDtoList);

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    @Mapping(target = "treatmentPlan", ignore = true)
    @Mapping(target = "medication", ignore = true)
    Treatment toEntity(TreatmentDto dto);

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    @Mapping(target = "treatmentPlan", ignore = true)
    @Mapping(target = "medication", ignore = true)
    TreatmentDto toDto(Treatment entity);

    List<Treatment> toTreatmentEntityList(List<TreatmentDto> treatmentDtoList);

    Diagnosis toEntity(DiagnosisDto dto);

    DiagnosisDto toDto(Diagnosis entity);

    void updateEntityFromDto(DiagnosisDto source, @MappingTarget Diagnosis target);

    Collection<DiagnosisDto> toDiagnosisDtoList(List<Diagnosis> entityList);

    void updateDto(TreatmentPlanDto source, @MappingTarget TreatmentPlanDto target);
}
