package de.bbajor.pvs.intravitreal.treatment.service;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.base.config.BaseMapperConfig;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
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
    void updateTreatmentPlan(TreatmentPlan source, @MappingTarget TreatmentPlan target);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateDiagnosis(Diagnosis source, @MappingTarget Diagnosis target);

}
