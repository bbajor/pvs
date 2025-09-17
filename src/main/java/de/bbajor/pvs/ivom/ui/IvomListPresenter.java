package de.bbajor.pvs.ivom.ui;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivom.dto.IvomDto;
import de.bbajor.pvs.ivom.model.Ivom;
import de.bbajor.pvs.ivom.service.IvomService;

@Component
public class IvomListPresenter {

    private final IvomService ivomService;

    public IvomListPresenter(IvomService ivomService) {
        this.ivomService = ivomService;
    }

    public IvomDialogPresenter getDialogPresenter() {
        return new IvomDialogPresenter(ivomService);
    }

    public List<IvomDto> generateDailyList(LocalDate date) {
        return ivomService.generateDailyList(date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<IvomDto> findAllBy(String searchString) {
        List<Ivom> ivoms = ivomService.findIvoms(searchString);
        return ivoms.stream()
                .map(this::mapToDto)
                .toList();
    }

    private IvomDto mapToDto(Ivom entity) {
        IvomDto dto = new IvomDto();
        dto.setId(entity.getId())
                .setCreationDate(entity.getCreationDate());
        return dto;
    }

}
