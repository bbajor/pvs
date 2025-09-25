package de.bbajor.pvs.base.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.domain.HealthInsurance;
import de.bbajor.pvs.base.repository.HealthInsuranceRepository;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.patientsearch.dto.HealthInsuranceDto;

@Service
public class HealthInsuranceService {

    private final HealthInsuranceRepository healthInsuranceRepository;
    private final ModelToDtoMapper modelToDtoMapper;

    public HealthInsuranceService(HealthInsuranceRepository healthInsuranceRepository,
            ModelToDtoMapper modelToDtoMapper) {
        this.healthInsuranceRepository = healthInsuranceRepository;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public List<HealthInsuranceDto> findAll() {
        return healthInsuranceRepository.findAll().stream().map(this::doDto).toList();
    }

    private HealthInsuranceDto doDto(HealthInsurance healthinsurance) {
        return modelToDtoMapper.toDto(healthinsurance);
    }
}
