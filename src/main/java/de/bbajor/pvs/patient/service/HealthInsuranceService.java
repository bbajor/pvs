package de.bbajor.pvs.patient.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.patient.dto.HealthInsuranceDto;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;

@Service
public class HealthInsuranceService {

    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    @Autowired
    private PatientMapper mapper;

    public List<HealthInsuranceDto> findAll() {
        return mapper.toHealthInsuranceDtoList(healthInsuranceRepository.findAll());
    }

    public HealthInsurance findById(HealthInsurance healthInsurance) {
        return healthInsuranceRepository.getReferenceById(healthInsurance.getId());
    }

}
