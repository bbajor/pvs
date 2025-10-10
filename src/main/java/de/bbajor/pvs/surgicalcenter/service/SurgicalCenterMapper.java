package de.bbajor.pvs.surgicalcenter.service;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;

@Mapper(componentModel = "spring")
public interface SurgicalCenterMapper {

    public SurgicalCenter updateSurgicalCenter(SurgicalCenter source, @MappingTarget SurgicalCenter target);
}