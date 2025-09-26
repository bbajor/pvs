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

import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentSlot;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomPlanTimeSlotRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class TreatmentPlanService {

    private IvomPlanRepository ivomRepository;
    private IvomPlanTimeSlotRepository treatmentSlotRepository;

    public TreatmentPlanService(IvomPlanRepository ivomRepository, IvomPlanTimeSlotRepository treatmentSlotRepository) {
        this.ivomRepository = ivomRepository;
        this.treatmentSlotRepository = treatmentSlotRepository;
    }

    public Optional<TreatmentPlan> findById(Long id) {
        return ivomRepository.findById(id);
    }

    @Transactional
    public Collection<TreatmentPlan> findByPatient(Integer patientId) {
        return ivomRepository.findByPatientId(patientId);
    }

    public List<TreatmentPlan> findIvoms(String filter) {
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

        return ivomRepository.findAll(spec);
    }

    public TreatmentPlan save(TreatmentPlan newEntity) {
        return ivomRepository.save(newEntity);
    }

    public Collection<TreatmentPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<TreatmentSlot> saveTimeSlots(List<TreatmentSlot> ivomPlanTimeSlotsToCreate) {
        return treatmentSlotRepository.saveAll(ivomPlanTimeSlotsToCreate);
    }

    public List<TreatmentSlot> getTreatmentSlots(Long treatmentPlanId, String sideOfEye) {
        return treatmentSlotRepository.findAllByTreatmentPlanIdAndSideOfEye(treatmentPlanId, sideOfEye);
    }

}
