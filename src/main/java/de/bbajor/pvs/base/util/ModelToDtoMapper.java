package de.bbajor.pvs.base.util;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.base.domain.Address;
import de.bbajor.pvs.base.domain.HealthInsurance;
import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.domain.PatientHistory;
import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.dto.PatientHistoryDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterAddressDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterAddress;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

//TODO !!!!! aufsplitten in Mapper-interfaces pro domain-package !!!!!

@Mapper(componentModel = "spring")
public interface ModelToDtoMapper {

    PatientDto toDto(Patient patient);

    PatientHistory toEntity(PatientHistoryDto dto);

    PatientHistoryDto toDto(PatientHistory entity);

    Patient toEntity(PatientDto dto);

    AddressDto toDto(Address address);

    Address toEntity(AddressDto dto);

    HealthInsuranceDto toDto(HealthInsurance entity);

    HealthInsurance toEntity(HealthInsuranceDto dto);

    void updateEntityFromDto(PatientDto dto, @MappingTarget Patient entity);

    void updateDtoFromEntity(Patient entity, @MappingTarget PatientDto dto);

    void updateDto(PatientDto patient, @MappingTarget PatientDto target);

    IntravitrealMedicationDto toDto(IntravitrealMedication entity);

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    TreatmentPlan toEntity(TreatmentPlanDto dto);

    @Mapping(target = "clinicalTrial", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    void updateEntityFromDto(TreatmentPlanDto workingCopy, @MappingTarget TreatmentPlan original);

    @Mapping(target = "treatments", ignore = true)
    TreatmentPlanDto toDto(TreatmentPlan e);

    @Mapping(target = "availableTimeSlots", ignore = true)
    SurgicalCenterDto toDto(SurgicalCenter surgeryUnit);

    @Mapping(target = "availableTimeSlots", ignore = true)
    SurgicalCenter toEntity(SurgicalCenterDto dto);

    SurgicalCenterAddressDto toDto(SurgicalCenterAddress address);

    SurgicalCenterAddress toEntity(SurgicalCenterAddressDto dto);

    @Mapping(target = "ivomPlanTimeSlots", ignore = true)
    SurgicalCenterTimeSlot toEntity(SurgicalCenterTimeSlotDto dto);

    @Mapping(target = "surgicalCenter", ignore = true)
    SurgicalCenterTimeSlotDto toDto(SurgicalCenterTimeSlot entity);

    IntravitrealMedication toEntity(IntravitrealMedicationDto bean);

    Diagnosis toEntity(DiagnosisDto dto);

    DiagnosisDto toDto(Diagnosis entity);

    @Mapping(target = "surgicalCenterTimeSlot", ignore = true)
    Treatment toEntity(TreatmentDto dto);

    @Mapping(target = "treatmentPlan", ignore = true)
    TreatmentDto toDto(Treatment entity);

}