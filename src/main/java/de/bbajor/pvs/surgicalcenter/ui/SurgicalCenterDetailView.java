package de.bbajor.pvs.surgicalcenter.ui;

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
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import jakarta.annotation.security.PermitAll;

@Route(value = "surgicalcenter/:id", layout = MainLayout.class)
@PageTitle("OP-Einheit Details")
@PermitAll
public class SurgicalCenterDetailView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${domain.bundesland}")
    private String bundesland;
    private final SurgicalCenterListPresenter surgeryUnitListPresenter;
    private final SurgicalCenterLayout surgeryUnitForm = new SurgicalCenterLayout();

    public SurgicalCenterDetailView(SurgicalCenterListPresenter surgeryUnitListPresenter) {
        this.surgeryUnitListPresenter = surgeryUnitListPresenter;

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            SurgicalCenterDto surgeryUnit = surgeryUnitForm.getBean();
            if(surgeryUnit.getId() == -1) {
                surgeryUnit.setId(null);
            }
            surgeryUnitListPresenter.save(surgeryUnit, surgeryUnitForm.getTimeSlotsToCreate());
            UI.getCurrent().navigate("surgicalcenter");

        });
        buttonBar.add(createButton);
        Button cancelButton = new Button("Zurück");
        cancelButton.addClickListener(event -> {
            UI.getCurrent().navigate("surgicalcenter");
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
            event.forwardTo(SurgicalCenterMainView.class);
            return;
        }

        try {
            Integer id = Integer.valueOf(idParameter.get());
            if (-1 == id) {
                SurgicalCenterDto newDto = new SurgicalCenterDto();
                newDto.setId(id);
                surgeryUnitForm.setBean(newDto);
            } else {
                SurgicalCenterDto dto = surgeryUnitListPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(SurgicalCenterMainView.class);
                    return;
                }
                surgeryUnitForm.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(SurgicalCenterMainView.class);
        }
    }
}
