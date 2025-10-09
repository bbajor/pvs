package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.base.BaseMapperConfig;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnose;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.patient.service.PatientMapper;

@Mapper(componentModel = "spring", uses = { PatientMapper.class,
        TreatmentMapper.class }, config = BaseMapperConfig.class)
public interface TreatmentPlanMapper {

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    @Mapping(target = "diagnosis", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(TreatmentPlanDto source, @MappingTarget TreatmentPlan target);

    @Mapping(target = "treatments", ignore = true)
    TreatmentPlanDto toTreatmentPlanDto(TreatmentPlan e);

    List<TreatmentPlanDto> toTreatmentPlanDtoList(List<TreatmentPlan> treatmentPlanList);

    List<TreatmentPlan> toTreatmentPlanEntityList(List<TreatmentPlanDto> treatmentPlanDtoList);

    DiagnosisDto toDiagnosisDto(Diagnose entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(DiagnosisDto source, @MappingTarget Diagnose target);

    List<DiagnosisDto> toDiagnosisDtoList(List<Diagnose> entityList);

    void updateTreatmentPlanDto(TreatmentPlanDto source, @MappingTarget TreatmentPlanDto target);
}
