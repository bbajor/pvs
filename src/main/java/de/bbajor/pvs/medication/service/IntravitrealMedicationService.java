package de.bbajor.pvs.medication.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.model.IntravitrealMedication;
import de.bbajor.pvs.medication.repository.IntravitrealMedicationRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class IntravitrealMedicationService {

    @Autowired
    private MedicationMapper treatmentPlanMapper;
    @Autowired
    private IntravitrealMedicationRepository medicationRepository;

    public Optional<IntravitrealMedication> findById(Long id) {
        return medicationRepository.findById(id);
    }

    public List<IntravitrealMedicationDto> findIntravitrealMedication(String filter) {
        Specification<IntravitrealMedication> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return treatmentPlanMapper.toMedicationDtoList(medicationRepository.findAll(spec));
    }

    public List<IntravitrealMedication> findAll() {
        return medicationRepository.findAll();
    }

    @Transactional
    public IntravitrealMedicationDto save(IntravitrealMedicationDto dto) {
        IntravitrealMedication entityToSave;

        if (dto.getId() == null) {
            entityToSave = treatmentPlanMapper.toEntity(dto);
        } else {
            entityToSave = medicationRepository.getReferenceById(dto.getId());
            treatmentPlanMapper.updateEntityFromDto(dto, entityToSave);
        }

        IntravitrealMedication savedEntity = medicationRepository.save(entityToSave);
        return treatmentPlanMapper.toDto(savedEntity);
    }

    public List<IntravitrealMedicationDto> getMedicationListFavorites() {
        return treatmentPlanMapper.toMedicationDtoList(medicationRepository.findAllByIsFavouriteTrue());
    }

}
