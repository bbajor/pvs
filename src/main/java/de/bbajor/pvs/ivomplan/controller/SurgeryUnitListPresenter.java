package de.bbajor.pvs.ivomplan.controller;

import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.service.SurgeryUnitService;

@Component
public class SurgeryUnitListPresenter {

    private final SurgeryUnitService surgeryUnitService;

    public SurgeryUnitListPresenter(SurgeryUnitService surgeryUnitService) {
        this.surgeryUnitService = surgeryUnitService;
    }

    public List<SurgeryUnitDto> getAll() {
        return surgeryUnitService.findAll();
    }

    public SurgeryUnitDto getById(Integer id) {
        return surgeryUnitService.getById(id);
    }

}
