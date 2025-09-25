package de.bbajor.pvs.intravitreal.treatment.ui;

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
import de.bbajor.pvs.intravitreal.treatment.controller.IvomPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomPlanDto;
import jakarta.annotation.security.PermitAll;

@Route(value = "ivom/:id", layout = MainLayout.class)
@PageTitle("IVOM-Behandlungsplan")
@PermitAll
public class IvomPlanDetailView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${domain.bundesland}")
    private String bundesland;

    private final IvomPlanPresenter ivomPlanPresenter;
    private final IvomPlanLayout ivomPlanLayout;

    public IvomPlanDetailView(IvomPlanPresenter ivomDialogPresenter) {
        this.ivomPlanPresenter = ivomDialogPresenter;

        ivomPlanLayout = new IvomPlanLayout(ivomDialogPresenter);
        setSizeFull();

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();

        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            IvomPlanDto ivomPlan = ivomPlanLayout.geIvomDto();
            if (ivomPlan.getId() == -1) {
                ivomPlan.setId(null);
            }
            ivomDialogPresenter.save(ivomPlan, ivomPlanLayout.getTimeSlotsToCreate());
            UI.getCurrent().navigate("ivom");

        });
        buttonBar.add(createButton);

        Button cancelButton = new Button("Zurück");
        cancelButton.addClickListener(event -> {
            UI.getCurrent().navigate("ivom");
        });
        buttonBar.add(cancelButton);
        HorizontalLayout dummy = new HorizontalLayout();
        dummy.setWidthFull();
        buttonBar.add(dummy);
        add(buttonBar);

        add(ivomPlanLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Optional<String> idParameter = event.getRouteParameters().get("id");
        if (idParameter.isEmpty()) {
            event.forwardTo(IvomPlanMainView.class);
            return;
        }

        try {
            Long id = Long.valueOf(idParameter.get());
            if (-1 == id) {
                IvomPlanDto newDto = new IvomPlanDto();
                newDto.setId(id);
                ivomPlanLayout.setBean(newDto);
            } else {
                IvomPlanDto dto = ivomPlanPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(IvomPlanMainView.class);
                    return;
                }
                ivomPlanLayout.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(IvomPlanMainView.class);
        }
    }

}
