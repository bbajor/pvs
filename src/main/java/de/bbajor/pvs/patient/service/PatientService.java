package de.bbajor.pvs.patient.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;
import de.bbajor.pvs.patient.repository.PatientAddressRepository;
import de.bbajor.pvs.patient.repository.PatientRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private PatientAddressRepository addressRepository;
    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    @Autowired
    private PatientMapper mapper;

    public List<PatientDto> findPatients(String filter) {

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
                Integer birthFilter = Integer.parseInt(filter);
                predicates.add(cb.equal(root.get("birth"), birthFilter));
            } catch (NumberFormatException ignored) {
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return mapper.toPatientDtoList(patientRepository.findAll(spec));
    }

    public PatientDto save(Patient patient) {
        if (patient == null) {
            return null;
        }
        Patient saved;
        if (patient.getId() != null) {
            if (patient.getId() <= 0) {
                patient.setId(null);
                if (patient.getAddress() != null) {
                    Address address = patient.getAddress();
                    if (address.getId() != null && address.getId() <= 0) {
                        address.setId(null);
                    }
                    Address savedAddress = addressRepository.save(address);
                    patient.setAddress(savedAddress);
                }
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
                if (patient.getAddress() != null) {
                    Address address = patient.getAddress();
                    if (address.getId() != null && address.getId() <= 0) {
                        address.setId(null);
                    }
                    Address savedAddress = addressRepository.save(address);
                    patient.setAddress(savedAddress);
                }
                if (patient.getHealthInsurance() != null) {
                    HealthInsurance healthInsurance = patient.getHealthInsurance();
                    if (healthInsurance.getId() != null && healthInsurance.getId() <= 0) {
                        healthInsurance.setId(null);
                    }
                    HealthInsurance savedHealthInsurance = healthInsuranceRepository.save(healthInsurance);
                    patient.setHealthInsurance(savedHealthInsurance);
                }
                mapper.updateEntityFromEntity(patient, existingPatient);
                saved = patientRepository.save(existingPatient);
            }
        } else {
            if (patient.getAddress() != null) {
                Address address = patient.getAddress();
                if (address.getId() != null && address.getId() <= 0) {
                    address.setId(null);
                }
                Address savedAddress = addressRepository.save(address);
                patient.setAddress(savedAddress);
            }
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
        return mapper.toDto(saved);
    }

    public List<PatientDto> findAll() {
        return mapper.toPatientDtoList(patientRepository.findAll());
    }

    public PatientDto findById(Integer id) {
        return id == null ? null : mapper.toDto(patientRepository.findById(id).get());
    }

    public List<PatientDto> getAll() {
        return mapper.toPatientDtoList(patientRepository.findAll());
    }

    public Patient findEntityById(Integer id) {
        if (id == null) {
            return null;
        }
        return patientRepository.findById(id).orElseThrow();
    }

    @Transactional
    public List<PatientDto> saveAll(List<Patient> patients) {
        List<PatientDto> patientDtos = new ArrayList<>();
        for (Patient patient : patients) {
            patientDtos.add(save(patient));
        }
        return patientDtos;
    }

}