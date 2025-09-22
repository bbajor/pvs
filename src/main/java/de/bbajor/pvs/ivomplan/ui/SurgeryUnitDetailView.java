package de.bbajor.pvs.ivomplan.ui;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.ivomplan.controller.SurgeryUnitListPresenter;
import de.bbajor.pvs.ivomplan.dto.Bundesland;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import jakarta.annotation.security.PermitAll;

@Route(value = "surgeryunit/:id", layout = MainLayout.class)
@PageTitle("OP-Einheit Details")
@PermitAll
public class SurgeryUnitDetailView extends VerticalLayout implements BeforeEnterObserver {

    @Value("domain.bundesland")
    private String bundesland;
    private final SurgeryUnitListPresenter surgeryUnitListPresenter;
    private final SurgeryUnitForm surgeryUnitForm;

    public SurgeryUnitDetailView(SurgeryUnitListPresenter surgeryUnitListPresenter) {
        this.surgeryUnitListPresenter = surgeryUnitListPresenter;
        surgeryUnitForm = new SurgeryUnitForm(Bundesland.byString(bundesland));

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            SurgeryUnitDto surgeryUnit = surgeryUnitForm.getBean();
            if(surgeryUnit.getId() == -1) {
                surgeryUnit.setId(null);
            }
            surgeryUnitListPresenter.save(surgeryUnit, surgeryUnitForm.getTimeSlotsToCreate());
            UI.getCurrent().navigate("surgeryunit");

        });
        buttonBar.add(createButton);
        Button cancelButton = new Button("Zurück");
        cancelButton.addClickListener(event -> {
            UI.getCurrent().navigate("surgeryunit");
        });
        buttonBar.add(cancelButton);
        HorizontalLayout dummyLayout2 = new HorizontalLayout();
        dummyLayout2.setWidthFull();
        buttonBar.add(dummyLayout2);
        add(buttonBar);

        add(surgeryUnitForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Optional<String> idParameter = event.getRouteParameters().get("id");
        if (idParameter.isEmpty()) {
            event.forwardTo(SurgeryUnitView.class);
            return;
        }

        try {
            Integer id = Integer.valueOf(idParameter.get());
            if (-1 == id) {
                SurgeryUnitDto newDto = new SurgeryUnitDto();
                newDto.setId(id);
                surgeryUnitForm.setBean(newDto);
            } else {
                SurgeryUnitDto dto = surgeryUnitListPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(SurgeryUnitView.class);
                    return;
                }
                surgeryUnitForm.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(SurgeryUnitView.class);
        }
    }
}
