package de.bbajor.pvs.medication.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntravitrealMedicationService {

    @Autowired
    private MedicationMapper medicationMapper;
    @Autowired
    private MedicationRepository medicationRepository;

    public Optional<Medication> findById(Long id) {
        return medicationRepository.findById(id);
    }

    public List<Medication> findIntravitrealMedication(String filter) {
        Specification<Medication> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return medicationRepository.findAll(spec);
    }

    public List<Medication> findAll() {
        return medicationRepository.findAll();
    }

    @Transactional
    public Medication save(Medication update) {
        if (update.getId() == null || update.getId() <= 0) {
            update.setId(null);
            return medicationRepository.save(update);
        } else {
            Medication medication = medicationRepository.getReferenceById(update.getId());
            medicationMapper.updateMedication(update, medication);
            return medicationRepository.save(medication);
        }
    }

    public List<Medication> getMedicationListFavourites() {
        return medicationRepository.findAllByIsFavouriteTrue();
    }

    @Transactional
    public List<Medication> saveAll(List<Medication> medications) {
        return medicationRepository.saveAll(medications);
    }

}
