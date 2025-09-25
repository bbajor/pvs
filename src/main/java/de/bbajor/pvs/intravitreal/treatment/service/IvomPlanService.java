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

import de.bbajor.pvs.intravitreal.treatment.model.IvomPlan;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlanTimeSlot;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomPlanTimeSlotRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class IvomPlanService {

    private IvomPlanRepository ivomRepository;
    private IvomPlanTimeSlotRepository treatmentSlotRepository;

    public IvomPlanService(IvomPlanRepository ivomRepository, IvomPlanTimeSlotRepository treatmentSlotRepository) {
        this.ivomRepository = ivomRepository;
        this.treatmentSlotRepository = treatmentSlotRepository;
    }

    public Optional<IvomPlan> findById(Long id) {
        return ivomRepository.findById(id);
    }

    @Transactional
    public Collection<IvomPlan> findByPatient(Integer patientId) {
        return ivomRepository.findByPatientId(patientId);
    }

    public List<IvomPlan> findIvoms(String filter) {
        Specification<IvomPlan> spec = (root, query, cb) -> {
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

        return ivomRepository.findAll(spec);
    }

    public IvomPlan save(IvomPlan newEntity) {
        return ivomRepository.save(newEntity);
    }

    public Collection<IvomPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<IvomPlanTimeSlot> saveTimeSlots(List<IvomPlanTimeSlot> ivomPlanTimeSlotsToCreate) {
        return treatmentSlotRepository.saveAll(ivomPlanTimeSlotsToCreate);
    }

}
