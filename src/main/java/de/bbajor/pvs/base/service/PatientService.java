package de.bbajor.pvs.base.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.domain.Patient;
import de.bbajor.pvs.base.repository.PatientRepository;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import jakarta.persistence.criteria.Predicate;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ModelToDtoMapper modelToDtoMapper;

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
            } catch (NumberFormatException ignored) {
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return patientRepository.findAll(spec);
    }

    public Patient save(Patient patient) {
        Patient saved = patientRepository.save(patient);
        return saved;
    }

    public Collection<Patient> findAll() {
        return patientRepository.findAll();
    }

    public Optional<Patient> findById(Integer id) {
        return id == null ? Optional.empty() : patientRepository.findById(id);
    }

    public List<PatientDto> getAll() {
        return findAll().stream().map(modelToDtoMapper::toDto).toList();
    }

}