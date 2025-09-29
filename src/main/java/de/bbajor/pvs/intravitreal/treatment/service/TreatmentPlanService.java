package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.repository.PatientRepository;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
import de.bbajor.pvs.medication.repository.IntravitrealMedicationRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class TreatmentPlanService {

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private IvomDiagnosisRepository diagnosisRepository;
    @Autowired
    private IntravitrealMedicationRepository medicationRepository;
    @Autowired
    private TreatmentPlanMapper entityMapper;

    private Optional<TreatmentPlan> findById(Long id) {
        return treatmentPlanRepository.findById(id);
    }

    private List<TreatmentPlan> findAll() {
        return treatmentPlanRepository.findAll();
    }

    public List<TreatmentPlanDto> getTreatmentPlans() {
        return findAll().stream().map(entityMapper::toDto).toList();
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        return treatmentPlanRepository.findByPatientId(patientId);
    }

    public List<TreatmentPlanDto> getTreatmentPlans(String filter) {
        return findTreatmentPlans(filter).stream().map(entityMapper::toDto).toList();
    }

    private List<TreatmentPlan> findTreatmentPlans(String filter) {
        Specification<TreatmentPlan> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), likeFilter)));
            // TODO add more fields like name of health insurance, ivom type etc.

            try {
                Integer birthFilter = Integer.parseInt(filter);
                predicates.add(cb.equal(root.get("birth"), birthFilter));
            } catch (NumberFormatException ignored) {
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return treatmentPlanRepository.findAll(spec);
    }

    private Collection<TreatmentPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<TreatmentPlanDto> generateDailyList() {
        return generateDailyList(LocalDate.now()).stream().map(entityMapper::toDto).toList();
    }

    @Transactional
    private List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
        return treatmentRepository.findAllByTreatmentPlanId(treatmentPlanId);
    }

    public List<TreatmentDto> getTreatmentSlotsByTreatmentPlanId(Long treatmentPlanId) {
        return getTreatmentSlots(treatmentPlanId).stream().map(entityMapper::toDto).toList();
    }

    public TreatmentPlanDto loadTreatmentPlanDto(Long id) {
        Optional<TreatmentPlan> treatmentPlan = findById(id);
        if (treatmentPlan.isPresent()) {
            TreatmentPlanDto treatmentPlanDto = entityMapper.toDto(treatmentPlan.get());
            List<TreatmentDto> treatments = entityMapper.toTreatmentDtoList(getTreatmentSlots(id));
            treatmentPlanDto.setTreatments(treatments);
            return treatmentPlanDto;
        }
        return null;
    }

    @Transactional
    public TreatmentPlanDto saveTreatmentPlan(TreatmentPlanDto treatmentPlanDto) {
        // 1. save treatmentplan
        Patient patient = patientRepository.getReferenceById(treatmentPlanDto.getPatient().getId());
        Diagnosis diagnosis = diagnosisRepository.getReferenceById(treatmentPlanDto.getDiagnosis().getId());
        IntravitrealMedication medication = medicationRepository.getReferenceById(treatmentPlanDto.getDrug().getId());

        TreatmentPlan treatmentPlanToSave;
        if(treatmentPlanDto.getId()!=null) {
            treatmentPlanToSave = treatmentPlanRepository.getReferenceById(treatmentPlanDto.getId());
        } else {
            treatmentPlanToSave = entityMapper.toEntity(treatmentPlanDto);
        }
        entityMapper.updateEntityFromDto(treatmentPlanDto, treatmentPlanToSave);
        treatmentPlanToSave.setPatient(patient);
        treatmentPlanToSave.setDiagnosis(diagnosis);
        treatmentPlanToSave.setDrug(medication);
        TreatmentPlan savedTreatmentPlan = treatmentPlanRepository.save(treatmentPlanToSave);

        // 2. apply treatmentplan to all treatments
        List<Treatment> treatmentEntityList = entityMapper.toTreatmentEntityList(treatmentPlanDto.getTreatments());

        treatmentEntityList.forEach(t -> t.setTreatmentPlan(savedTreatmentPlan));
        List<Treatment> savedTreatments = treatmentRepository.saveAll(treatmentEntityList);

        TreatmentPlanDto savedTreatmentPlanDto = entityMapper.toDto(savedTreatmentPlan);
        List<TreatmentDto> savedTreatmentDtos = entityMapper.toTreatmentDtoList(savedTreatments);
        savedTreatmentPlanDto.setTreatments(savedTreatmentDtos);
        return savedTreatmentPlanDto;
    }

    public TreatmentPlanDto getTreatmentPlanById(Long id) {
        Optional<TreatmentPlan> treatmentPlan = treatmentPlanRepository.findById(id);
        if (treatmentPlan.isPresent()) {
            List<TreatmentDto> treatmentDtos = entityMapper.toTreatmentDtoList(treatmentRepository.findAllByTreatmentPlanId(id));
            TreatmentPlanDto treatmentPlanDto = entityMapper.toDto(treatmentPlan.get());
            treatmentPlanDto.setTreatments(treatmentDtos);
            return treatmentPlanDto;
        }
        return null;
    }

    @Transactional
    public List<TreatmentDto> saveTreatments(List<TreatmentDto> treatmentsToCreate) {
        List<Treatment> treatments = entityMapper.toTreatmentEntityList(treatmentsToCreate);
        List<Treatment> saved = treatmentRepository.saveAll(treatments);
        return entityMapper.toTreatmentDtoList(saved);
    }

}
