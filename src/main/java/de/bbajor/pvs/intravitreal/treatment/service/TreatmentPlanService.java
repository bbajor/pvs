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

import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterMapper;
import jakarta.persistence.criteria.Predicate;

@Service
public class TreatmentPlanService {

    private final SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository;

    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private TreatmentPlanMapper entityMapper;
    @Autowired
    private SurgicalCenterMapper surgicalCenterMapper;

    TreatmentPlanService(SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository) {
        this.surgicalCenterTimeSlotRepository = surgicalCenterTimeSlotRepository;
    }

    private TreatmentPlan findByIdWithDetails(Long id) {
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findByIdWithDetailsWithoutSurgicalCenterTimeSlots(id)
                .orElseThrow();
        treatmentPlan.setTreatments(treatmentRepository.findTreatmentsByPlanId(treatmentPlan.getId()));
        return treatmentPlan;
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

    private List<TreatmentPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<TreatmentPlanDto> generateDailyList() {
        return entityMapper.toTreatmentPlanDtoList(generateDailyList(LocalDate.now()));
    }

    @Transactional
    private List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
        List<Treatment> treatments = treatmentRepository.findTreatmentsByPlanId(treatmentPlanId);
        return treatments;
    }

    public List<TreatmentDto> getTreatmentSlotsByTreatmentPlanId(Long treatmentPlanId) {
        List<Treatment> treatments = getTreatmentSlots(treatmentPlanId);
        return toTreatmentDtoList(treatments);
    }

    public TreatmentPlanDto loadTreatmentPlanDto(Long id) {
        TreatmentPlan treatmentPlan = findByIdWithDetails(id);
        TreatmentPlanDto treatmentPlanDto = entityMapper.toDto(treatmentPlan);
        List<TreatmentDto> treatments = toTreatmentDtoList(treatmentPlan.getTreatments());
        treatmentPlanDto.setTreatments(treatments);
        return treatmentPlanDto;
    }

    @Transactional
    public TreatmentPlanDto saveTreatmentPlan(TreatmentPlanDto treatmentPlanDto) {
        // 1. save treatmentplan without treatments
        TreatmentPlan treatmentPlanToSave;
        if (treatmentPlanDto.getId() != null) {
            treatmentPlanToSave = treatmentPlanRepository
                    .findByIdWithDetailsWithoutSurgicalCenterTimeSlots(treatmentPlanDto.getId()).get();
        } else {
            treatmentPlanToSave = entityMapper.toEntity(treatmentPlanDto);
        }
        entityMapper.updateEntityFromDto(treatmentPlanDto, treatmentPlanToSave);
        TreatmentPlan savedTreatmentPlan = treatmentPlanRepository.save(treatmentPlanToSave);

        // 2. apply treatmentplan to all treatments
        List<Treatment> treatmentEntityList = new ArrayList<>();
        for (TreatmentDto treatmentDto : treatmentPlanDto.getTreatments()) {
            Treatment treatmentToSave = entityMapper.toEntity(treatmentDto);
            SurgicalCenterTimeSlot surgicalCenterTimeSlot = surgicalCenterTimeSlotRepository
                    .getReferenceById(treatmentDto.getSurgicalCenterTimeSlot().getId());
            treatmentToSave.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot);
            treatmentToSave.setTreatmentPlan(savedTreatmentPlan);
            treatmentEntityList.add(treatmentToSave);
        }
        List<Treatment> savedTreatments = treatmentRepository.saveAll(treatmentEntityList);

        TreatmentPlanDto savedTreatmentPlanDto = getTreatmentPlanById(savedTreatmentPlan.getId());
        return savedTreatmentPlanDto;
    }

    public TreatmentPlanDto getTreatmentPlanById(Long id) {
        TreatmentPlan result = treatmentPlanRepository.findByIdWithDetailsWithoutSurgicalCenterTimeSlots(id)
                .orElseThrow();
        result.getTreatments().clear();
        result.getTreatments().addAll(treatmentRepository.findTreatmentsByPlanId(result.getId()));
        List<TreatmentDto> treatmentDtos = toTreatmentDtoList(result.getTreatments());
        TreatmentPlanDto treatmentPlanDto = entityMapper.toDto(result);
        treatmentPlanDto.setTreatments(treatmentDtos);
        return treatmentPlanDto;
    }

    @Transactional
    public List<TreatmentDto> saveTreatments(List<TreatmentDto> treatmentsToCreate, Long treatmentPlanId) {

        TreatmentPlan treatmentPlan = getOriginalById(treatmentPlanId).orElseThrow();
        List<Treatment> treatments = new ArrayList<>();
        for (TreatmentDto treatmentDto : treatmentsToCreate) {
            SurgicalCenterTimeSlot timeSlot = surgicalCenterTimeSlotRepository
                    .getReferenceById(treatmentDto.getSurgicalCenterTimeSlot().getId());
            Treatment treatment = entityMapper.toEntity(treatmentDto);
            treatment.setSurgicalCenterTimeSlot(timeSlot);
            treatment.setTreatmentPlan(treatmentPlan);
            treatments.add(treatment);
        }

        List<Treatment> saved = treatmentRepository.saveAll(treatments);
        return toTreatmentDtoList(saved);

    }

    private List<TreatmentDto> toTreatmentDtoList(List<Treatment> treatments) {
        List<TreatmentDto> resultList = new ArrayList<>();
        for (Treatment treatment : treatments) {
            TreatmentDto treatmentDto = entityMapper.toDto(treatment);
            treatmentDto.setTreatmentPlan(entityMapper.toDto(treatment.getTreatmentPlan()));
            treatmentDto.setSurgicalCenterTimeSlot(surgicalCenterMapper.toDto(treatment.getSurgicalCenterTimeSlot()));
            resultList.add(treatmentDto);
        }
        return resultList;
    }

    public Optional<TreatmentPlan> getOriginalById(Long id) {
        return treatmentPlanRepository.findById(id);
    }
}
