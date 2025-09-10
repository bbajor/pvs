package de.bbajor.pvs.base.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.repository.PatientRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> findPatients(String filter) {
    Specification<Patient> spec = (root, query, cb) -> {
        String likeFilter = "%" + filter.toLowerCase() + "%";
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.or(
            cb.like(cb.lower(root.get("firstName")), likeFilter),
            cb.like(cb.lower(root.get("lastName")), likeFilter)
           // cb.like(cb.lower(root.get("healthInsuranceCard")), likeFilter)
        ));

        // Beispiel für Integer-Feld birth
        try {
            Integer birthFilter = Integer.parseInt(filter);
            predicates.add(cb.equal(root.get("birth"), birthFilter));
        } catch (NumberFormatException ignored) {}

        return cb.or(predicates.toArray(new Predicate[0]));
    };

    return patientRepository.findAll(spec);
}

}