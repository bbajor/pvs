package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import jakarta.transaction.Transactional;

@Service
public class IvomDiagnosisService {

    @Autowired
    private ModelToDtoMapper modelToDtoMapper;
    @Autowired
    private IvomDiagnosisRepository repository;

    @Transactional
    public DiagnosisDto save(DiagnosisDto dto) {
        return modelToDtoMapper.toDto(repository.save(modelToDtoMapper.toEntity(dto)));
    }

    public Collection<Diagnosis> findAll() {
        return repository.findAll();
    }

}
