package de.bbajor.pvs.base.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.repository.HealthInsuranceRepository;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;

@Service
public class HealthInsuranceService {

    @Autowired
    private HealthInsuranceRepository healthInsuranceRepository;
    @Autowired
    private PatientMapper mapper;

    public List<HealthInsuranceDto> findAll() {
        return mapper.toHealthInsuranceDtoList(healthInsuranceRepository.findAll());
    }

}
