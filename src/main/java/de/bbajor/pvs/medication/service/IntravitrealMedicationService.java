package de.bbajor.pvs.medication.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.medication.model.IntravitrealMedication;
import de.bbajor.pvs.medication.repository.IntravitrealMedicationRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class IntravitrealMedicationService {

    private IntravitrealMedicationRepository intravitrealMedicationRepository;

    public IntravitrealMedicationService(IntravitrealMedicationRepository ivomDrugRepository) {
        this.intravitrealMedicationRepository = ivomDrugRepository;
    }

    public Optional<IntravitrealMedication> findById(Long id) {
        return intravitrealMedicationRepository.findById(id);
    }

    public List<IntravitrealMedication> findIntravitrealMedication(String filter) {
        Specification<IntravitrealMedication> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return intravitrealMedicationRepository.findAll(spec);
    }

    public List<IntravitrealMedication> findAll() {
        return intravitrealMedicationRepository.findAll();
    }

    public void save(IntravitrealMedication newEntity) {
        intravitrealMedicationRepository.save(newEntity);
    }

}
