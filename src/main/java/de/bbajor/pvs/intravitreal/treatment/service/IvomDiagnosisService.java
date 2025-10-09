package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import jakarta.transaction.Transactional;

@Service
public class IvomDiagnosisService {

    @Autowired
    private TreatmentPlanMapper mapper;
    @Autowired
    private IvomDiagnosisRepository repository;

    @Transactional
    public DiagnosisDto save(DiagnosisDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public Collection<DiagnosisDto> getDiagnosisDtos() {
        return mapper.toDiagnosisDtoList(repository.findAll());
    }

    @Transactional
    public List<DiagnosisDto> saveAll(List<DiagnosisDto> dtos) {
        List<Diagnosis> entities = dtos.stream().map(mapper::toEntity).toList();
        List<Diagnosis> savedEntities = repository.saveAll(entities);
        return mapper.toDiagnosisDtoList(savedEntities);
    }

}
