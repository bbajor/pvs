package de.bbajor.pvs.ivomplan.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.ivomplan.model.IvomDiagnosis;
import de.bbajor.pvs.ivomplan.repository.IvomDiagnosisRepository;
import jakarta.transaction.Transactional;

@Service
public class IvomDiagnosisService {

    private final IvomDiagnosisRepository repository;

    public IvomDiagnosisService(IvomDiagnosisRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IvomDiagnosis save(IvomDiagnosis newEntity) {
        return repository.save(newEntity);
    }

    public Collection<IvomDiagnosis> findAll() {
        return repository.findAll();
    }

}
