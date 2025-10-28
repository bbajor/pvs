package de.bbajor.pvs.patient.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;
import de.bbajor.pvs.patient.repository.PatientRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    @Autowired
    private PatientMapper mapper;

    public List<Patient> findPatients(String filter) {

        if (StringUtils.isEmpty(filter)) {
            return getAll();
        }

        Specification<Patient> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("firstName")), likeFilter),
                    cb.like(cb.lower(root.get("lastName")), likeFilter)
            // cb.like(cb.lower(root.get("healthInsuranceCard")), likeFilter)
            ));
            try {
                // Parse as ISO date (yyyy-MM-dd) for filtering by birth date
                java.time.LocalDate birthDate = java.time.LocalDate.parse(filter);
                predicates.add(cb.equal(root.get("birth"), birthDate));
            } catch (Exception ignored) {
                // Not a date; ignore
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return patientRepository.findAll(spec);
    }

    public Patient save(Patient patient) {
        if (patient == null) {
            return null;
        }
        Patient saved;
        if (patient.getId() != null) {
            if (patient.getId() <= 0) {
                patient.setId(null);
                if (patient.getHealthInsurance() != null) {
                    HealthInsurance healthInsurance = patient.getHealthInsurance();
                    if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                        healthInsurance.setId(null);
                    }
                    HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                    patient.setHealthInsurance(savedHealthInsurance);
                }
                saved = patientRepository.save(patient);
            } else {
                Patient existingPatient = patientRepository.getReferenceById(patient.getId());
                if (patient.getHealthInsurance() != null) {
                    HealthInsurance healthInsurance = patient.getHealthInsurance();
                    if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                        healthInsurance.setId(null);
                    }
                    HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                    patient.setHealthInsurance(savedHealthInsurance);
                }
                mapper.updatePatientEntity(patient, existingPatient);
                saved = patientRepository.save(existingPatient);
            }
        } else {
            if (patient.getHealthInsurance() != null) {
                HealthInsurance healthInsurance = patient.getHealthInsurance();
                if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                    healthInsurance.setId(null);
                }
                HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                patient.setHealthInsurance(savedHealthInsurance);
            }
            saved = patientRepository.save(patient);
        }
        return saved;
    }

    public Patient findById(Integer id) {
        return id == null ? null : patientRepository.findById(id).orElseThrow();
    }

    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    public Patient findEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        return patientRepository.findById(id).orElseThrow();
    }

    @Transactional
    public List<Patient> saveAll(List<Patient> patients) {
        Objects.requireNonNull(patients);
        return patientRepository.saveAll(patients);
    }

}