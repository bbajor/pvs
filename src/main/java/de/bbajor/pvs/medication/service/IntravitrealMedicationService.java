package de.bbajor.pvs.medication.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class IntravitrealMedicationService {

    @Autowired
    private MedicationMapper medicationMapper;
    @Autowired
    private MedicationRepository medicationRepository;

    public Optional<Medication> findById(Long id) {
        return medicationRepository.findById(id);
    }

    public List<MedicationDto> findIntravitrealMedication(String filter) {
        Specification<Medication> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return medicationMapper.toMedicationDtoList(medicationRepository.findAll(spec));
    }

    public List<Medication> findAll() {
        return medicationRepository.findAll();
    }

    @Transactional
    public MedicationDto save(MedicationDto dto) {
        Medication entityToSave;

        if (dto.getId() == null) {
            entityToSave = medicationMapper.toEntity(dto);
        } else {
            entityToSave = medicationRepository.getReferenceById(dto.getId());
            medicationMapper.updateEntityFromDto(dto, entityToSave);
        }

        Medication savedEntity = medicationRepository.save(entityToSave);
        return medicationMapper.toDto(savedEntity);
    }

    public List<MedicationDto> getMedicationListFavourites() {
        return medicationMapper.toMedicationDtoList(medicationRepository.findAllByIsFavouriteTrue());
    }

    @Transactional
    public List<MedicationDto> saveAll(List<Medication> medications) {
        List<MedicationDto> medicationDtos = new ArrayList<>();
        for (Medication medication : medications) {
            medicationDtos.add(save(medicationMapper.toDto(medication)));
        }
        return medicationDtos;
    }

}
