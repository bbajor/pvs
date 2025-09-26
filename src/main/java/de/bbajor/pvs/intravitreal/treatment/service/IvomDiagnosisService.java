package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import jakarta.transaction.Transactional;

@Service
public class IvomDiagnosisService {

    private final IvomDiagnosisRepository repository;

    public IvomDiagnosisService(IvomDiagnosisRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Diagnosis save(Diagnosis newEntity) {
        return repository.save(newEntity);
    }

    public Collection<Diagnosis> findAll() {
        return repository.findAll();
    }

}
