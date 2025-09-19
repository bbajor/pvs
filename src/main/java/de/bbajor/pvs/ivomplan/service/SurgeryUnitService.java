package de.bbajor.pvs.ivomplan.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.domain.Address;
import de.bbajor.pvs.base.dto.AddressDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.repository.SurgeryUnitRepository;

@Service
public class SurgeryUnitService {

    private final SurgeryUnitRepository surgeryUnitRepository;

    public SurgeryUnitService(SurgeryUnitRepository surgeryUnitRepository) {
        this.surgeryUnitRepository = surgeryUnitRepository;
    }

    public List<SurgeryUnitDto> findAll() {
        List<SurgeryUnit> surgeryUnits = surgeryUnitRepository.findAll();
        return surgeryUnits.stream()
                .map(SurgeryUnitService::toDto)
                .toList();
    }

    public static SurgeryUnitDto toDto(SurgeryUnit surgeryunit) {
        SurgeryUnitDto dto = new SurgeryUnitDto();

        dto.setId(surgeryunit.getId())
                .setName(surgeryunit.getName())
                .setPhone(surgeryunit.getPhone())
                .setEmail(surgeryunit.getEmail())
                .setContact(surgeryunit.getContact())
                .setPhoneContact(surgeryunit.getPhoneContact());

        if (surgeryunit.getAddress() != null) {
            Address address = surgeryunit.getAddress();
            AddressDto addressDto = new AddressDto();
            addressDto.setStreet(address.getStreet())
                    .setHouseNumber(address.getHouseNumber())
                    .setPostalCode(address.getPostalCode())
                    .setCity(address.getCity());
            if (address.getCountry() != null) {
                addressDto.setCountryCode(Locale.of(address.getCountry()));
            }
        }

        return dto;
    }

    public SurgeryUnitDto getById(Integer id) {
        SurgeryUnit surgeryUnit = surgeryUnitRepository.getReferenceById(id);
        return toDto(surgeryUnit);
    }
}
