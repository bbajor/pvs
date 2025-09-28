package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanTimeSlotRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class TreatmentPlanService {

    private TreatmentPlanRepository treatmentPlanRepository;
    private TreatmentPlanTimeSlotRepository treatmentSlotRepository;
    private ModelToDtoMapper modelToDtoMapper;

    public TreatmentPlanService(TreatmentPlanRepository treatmentPlanRepository,
            TreatmentPlanTimeSlotRepository treatmentSlotRepository, ModelToDtoMapper modelToDtoMapper) {
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.treatmentSlotRepository = treatmentSlotRepository;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    private Optional<TreatmentPlan> findById(Long id) {
        return treatmentPlanRepository.findById(id);
    }

    private List<TreatmentPlan> findAll() {
        return treatmentPlanRepository.findAll();
    }

    public List<TreatmentPlanDto> getTreatmentPlans() {
        return findAll().stream().map(modelToDtoMapper::toDto).toList();
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        return treatmentPlanRepository.findByPatientId(patientId);
    }

    public List<TreatmentPlanDto> getTreatmentPlans(String filter) {
        return findTreatmentPlans(filter).stream().map(modelToDtoMapper::toDto).toList();
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

    private TreatmentPlan save(TreatmentPlan newEntity) {
        TreatmentPlan saved = treatmentPlanRepository.save(newEntity);
        return saved;
    }

    private Collection<TreatmentPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<TreatmentPlanDto> generateDailyList() {
        return generateDailyList(LocalDate.now()).stream().map(modelToDtoMapper::toDto).toList();
    }

    private List<Treatment> saveTreatmentSlots(List<Treatment> treatmentSlots) {
        return treatmentSlotRepository.saveAll(treatmentSlots);
    }

    @Transactional
    private List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
        return treatmentSlotRepository.findAllByTreatmentPlanId(treatmentPlanId);
    }

    public List<TreatmentDto> getTreatmentSlotsByTreatmentPlanId(Long treatmentPlanId) {
        return getTreatmentSlots(treatmentPlanId).stream().map(modelToDtoMapper::toDto).toList();
    }

    public TreatmentPlanDto loadTreatmentPlanDto(Long id) {
        Optional<TreatmentPlan> treatmentPlan = findById(id);
        if (treatmentPlan.isPresent()) {
            TreatmentPlanDto treatmentPlanDto = modelToDtoMapper.toDto(treatmentPlan.get());
            List<TreatmentDto> treatments = getTreatmentSlots(id).stream().map(modelToDtoMapper::toDto).toList();
            treatmentPlanDto.setTreatments(treatments);
            return treatmentPlanDto;
        }
        return null;
    }

    public TreatmentPlanDto saveTreatmentPlan(TreatmentPlanDto treatmentPlanDto) {
        // 1. save treatmentplan
        TreatmentPlan treatmentPlan = modelToDtoMapper.toEntity(treatmentPlanDto);
        treatmentPlanRepository.save(treatmentPlan);

        // 2. apply treatmentplan to all treatments
        List<Treatment> treatmentDtos = treatmentPlanDto.getTreatments().stream().map(modelToDtoMapper::toEntity)
                .toList();
        treatmentDtos.forEach(t -> t.setTreatmentPlan(treatmentPlan));
        List<Treatment> saved = treatmentSlotRepository.saveAll(treatmentDtos);

        TreatmentPlanDto savedTreatmentPlanDto = modelToDtoMapper.toDto(treatmentPlan);
        List<TreatmentDto> savedTreatmentDtos = saved.stream().map(modelToDtoMapper::toDto).toList();
        savedTreatmentPlanDto.setTreatments(savedTreatmentDtos);
        return savedTreatmentPlanDto;
    }

    public TreatmentPlanDto getTreatmentPlanById(Long id) {
        Optional<TreatmentPlan> treatmentPlan = treatmentPlanRepository.findById(id);
        if (treatmentPlan.isPresent()) {
            List<TreatmentDto> treatmentDtos = treatmentSlotRepository.findAllByTreatmentPlanId(id).stream()
                    .map(modelToDtoMapper::toDto).toList();
            TreatmentPlanDto treatmentPlanDto = modelToDtoMapper.toDto(treatmentPlan.get());
            treatmentPlanDto.setTreatments(treatmentDtos);
            return treatmentPlanDto;
        }
        return null;
    }

    public List<TreatmentDto> saveTreatments(List<TreatmentDto> treatmentsToCreate) {
        return treatmentSlotRepository.saveAll(treatmentsToCreate.stream().map(modelToDtoMapper::toEntity).toList())
                .stream().map(modelToDtoMapper::toDto).toList();
    }

}
