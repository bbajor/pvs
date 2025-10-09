package de.bbajor.pvs.intravitreal.treatment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnose;
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
        Objects.requireNonNull(dto);

        Diagnose diagnosisToSave;
        if (dto.getId() == null || dto.getId() <= 0) {
            dto.setId(null);
            diagnosisToSave = new Diagnose();
            mapper.updateEntityFromDto(dto, diagnosisToSave);
        } else {
            diagnosisToSave = repository.findById(dto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Diagnosis with ID " + dto.getId() + " not found"));
            mapper.updateEntityFromDto(dto, diagnosisToSave);
        }
        return mapper.toDiagnosisDto(repository.save(diagnosisToSave));
    }

    public Collection<DiagnosisDto> getDiagnosisDtos() {
        return mapper.toDiagnosisDtoList(repository.findAll());
    }

    @Transactional
    public List<DiagnosisDto> saveAll(List<DiagnosisDto> dtos) {
        Objects.requireNonNull(dtos);
        List<Diagnose> entities = new ArrayList<>(dtos.size());
        for (DiagnosisDto diagnosisDto : dtos) {
            Diagnose entity = new Diagnose();
            mapper.updateEntityFromDto(diagnosisDto, entity);
            entities.add(entity);
        }
        List<Diagnose> savedEntities = repository.saveAll(entities);
        return mapper.toDiagnosisDtoList(savedEntities);
    }

    public Diagnose getByDiagnoseId(Long id) {
        Diagnose diagnosis = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Diagnosis not found with id: " + id));
        return diagnosis;
    }

}
