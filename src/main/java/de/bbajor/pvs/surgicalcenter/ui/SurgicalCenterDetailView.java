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
    private final SurgicalCenterListPresenter surgicalCenterListPresenter;
    private final SurgicalCenterLayout surgicalCenterLayout = new SurgicalCenterLayout();

    public SurgicalCenterDetailView(SurgicalCenterListPresenter surgicalCenterListPresenter) {
        this.surgicalCenterListPresenter = surgicalCenterListPresenter;

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            SurgicalCenterDto surgeryUnit = surgicalCenterLayout.getBean();
            if(surgeryUnit.getId() == -1) {
                surgeryUnit.setId(null);
            }
            surgicalCenterListPresenter.save(surgeryUnit, surgicalCenterLayout.getTimeSlotsToCreate());
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

        add(surgicalCenterLayout);
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
                surgicalCenterLayout.setBean(newDto);
            } else {
                SurgicalCenterDto dto = surgicalCenterListPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(SurgicalCenterMainView.class);
                    return;
                }
                surgicalCenterLayout.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(SurgicalCenterMainView.class);
        }
    }
}
