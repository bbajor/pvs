package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import jakarta.transaction.Transactional;

@Service
public class IvomDiagnosisService {

    @Autowired
    private IvomDiagnosisRepository repository;

    @Transactional
    public Diagnosis save(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis);
        return repository.save(diagnosis);
    }

    public Collection<Diagnosis> getDiagnoses() {
        return repository.findAll();
    }

    @Transactional
    public List<Diagnosis> saveAll(List<Diagnosis> diagnosisList) {
        return repository.saveAll(diagnosisList);
    }

    public Diagnosis getByDiagnoseId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Diagnosis not found with id: " + id));
    }

}
