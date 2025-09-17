package de.bbajor.pvs.ivom.ui;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivom.dto.IvomDto;
import de.bbajor.pvs.ivom.model.Ivom;
import de.bbajor.pvs.ivom.service.IvomService;

@Component
public class IvomDialogPresenter {

    private final IvomService ivomService;
    private IvomDto workingCopy;
    private Ivom original;

    public IvomDialogPresenter(IvomService ivomService) {
        this.ivomService = ivomService;
    }

    public void loadIvomById(Long id) {
        if (id != null) {
            this.original = ivomService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ivom not found: " + id));
            this.workingCopy = copyFromEntity(original);
        } else {
            this.original = null;
            this.workingCopy = new IvomDto(); // leere WorkingCopy für Neuanlage
        }
    }

    public void saveChanges() {
        if (workingCopy == null) {
            throw new IllegalStateException("No data loaded into dialog");
        }
        if (original == null) {
            Ivom newEntity = mapToIvomEntity(new Ivom(), workingCopy);
            ivomService.save(newEntity);
        } else {
            mapToIvomEntity(original, workingCopy);
            ivomService.save(original);
        }
    }

    public IvomDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new IvomDto();
        }
        return workingCopy;
    }

    private IvomDto copyFromEntity(Ivom e) {
        IvomDto dto = new IvomDto();
        dto.setId(e.getId())
        .setCreationDate(e.getCreationDate());
        // TODO: map other fields
        return dto;
    }

    private Ivom mapToIvomEntity(Ivom entity, IvomDto dto) {

        if (dto == null) {
            return null;
        }

        if (entity == null) {
            entity = new Ivom();
        }
        entity
                .setCreationDate(dto.getCreationDate())
                .setDescription(dto.getAdditionalInformation());
        // TODO: map other fields
        return entity;
    }
}
