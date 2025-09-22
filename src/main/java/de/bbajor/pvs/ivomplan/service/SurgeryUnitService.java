package de.bbajor.pvs.ivomplan.service;

import java.util.List;
import de.bbajor.pvs.ivomplan.repository.SurgeryUnitTimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.misc.ModelToDtoMapper;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;
import de.bbajor.pvs.ivomplan.repository.SurgeryUnitRepository;

@Service
public class SurgeryUnitService {

    private final SurgeryUnitTimeSlotRepository surgeryUnitTimeSlotRepository;

    private final SurgeryUnitRepository surgeryUnitRepository;

    private final ModelToDtoMapper modelToDtoMapper;

    public SurgeryUnitService(SurgeryUnitRepository surgeryUnitRepository, ModelToDtoMapper modelToDtoMapper,
            SurgeryUnitTimeSlotRepository surgeryUnitTimeSlotRepository) {
        this.surgeryUnitRepository = surgeryUnitRepository;
        this.modelToDtoMapper = modelToDtoMapper;
        this.surgeryUnitTimeSlotRepository = surgeryUnitTimeSlotRepository;
    }

    public List<SurgeryUnitDto> findAll() {
        List<SurgeryUnit> surgeryUnits = surgeryUnitRepository.findAll();
        return surgeryUnits.stream()
                .map(this::toDto)
                .toList();
    }

    public SurgeryUnitDto toDto(SurgeryUnit surgeryUnit) {
        return modelToDtoMapper.toDto(surgeryUnit);
    }

    public SurgeryUnitDto findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return surgeryUnitRepository.findByIdWithDetails(id)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public void save(SurgeryUnitDto surgeryUnitDto) {
        SurgeryUnit entityToSave = modelToDtoMapper.toEntity(surgeryUnitDto);
        if (entityToSave.getId() != null && entityToSave.getId() == 0L) {
            entityToSave.setId(null);
        }
        if (entityToSave.getSurgeryUnitAddress() != null &&
                (entityToSave.getSurgeryUnitAddress().getId() == null
                        || entityToSave.getSurgeryUnitAddress().getId() == 0L)) {
            entityToSave.getSurgeryUnitAddress().setId(null);
        }
        surgeryUnitRepository.save(entityToSave);
    }

    @Transactional
    public void saveTimeSlots(List<SurgeryUnitTimeSlotDto> newTimeSlots, SurgeryUnitDto surgeryUnitDto) {
        if (newTimeSlots == null || newTimeSlots.isEmpty() || surgeryUnitDto == null) {
            return;
        }

        SurgeryUnit surgeryUnit;
        if (surgeryUnitDto.getId() != null) {
            surgeryUnit = findById(surgeryUnitDto.getId());
        } else {
            surgeryUnit = modelToDtoMapper.toEntity(surgeryUnitDto);
            surgeryUnitRepository.save(surgeryUnit);
        }
        List<SurgeryUnitTimeSlot> newTimeSlotsToSave = newTimeSlots.stream()
                .map(e -> {
                    SurgeryUnitTimeSlot entity = toEntity(e);
                    entity.setSurgeryUnit(surgeryUnit);
                    return entity;
                })
                .toList();
        surgeryUnitTimeSlotRepository.saveAll(newTimeSlotsToSave);

    }

    private SurgeryUnit findById(Integer id) {
        return surgeryUnitRepository.getReferenceById(id);
    }

    private SurgeryUnitTimeSlot toEntity(SurgeryUnitTimeSlotDto dto) {
        return modelToDtoMapper.toEntity(dto);
    }

}
