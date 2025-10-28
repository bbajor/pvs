package de.bbajor.pvs.patient.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;

@Service
public class HealthInsuranceService {

    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;

    @Transactional(readOnly = true)
    public List<HealthInsurance> findAll() {
        return healthInsuranceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public HealthInsurance findById(HealthInsurance healthInsurance) {
        return healthInsuranceRepository.getReferenceById(healthInsurance.getId());
    }

}
