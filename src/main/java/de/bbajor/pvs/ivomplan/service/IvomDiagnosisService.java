package de.bbajor.pvs.ivomplan.service;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.ivomplan.model.IvomDiagnosis;
import de.bbajor.pvs.ivomplan.repository.IvomDiagnosisRepository;

@Service
public class IvomDiagnosisService {

    private final IvomDiagnosisRepository repository;

    public IvomDiagnosisService(IvomDiagnosisRepository repository) {
        this.repository = repository;
    }

    public void save(IvomDiagnosis newEntity) {
        repository.save(newEntity);
    }

}
