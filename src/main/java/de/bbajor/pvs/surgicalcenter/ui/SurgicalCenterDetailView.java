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
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import de.bbajor.pvs.security.AppRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

@Route(value = "surgicalcenter/:id", layout = MainLayout.class)
@PageTitle("OP-Einheit Details")
@PermitAll
public class SurgicalCenterDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final InstitutionRepository institutionRepository;

    @Value("${domain.bundesland}")
    private String bundesland;
    private final SurgicalCenterListPresenter surgicalCenterListPresenter;
    private final SurgicalCenterLayout surgicalCenterLayout = new SurgicalCenterLayout();

    public SurgicalCenterDetailView(SurgicalCenterListPresenter surgicalCenterListPresenter,
            InstitutionRepository institutionRepository) {
        this.surgicalCenterListPresenter = surgicalCenterListPresenter;
        this.institutionRepository = institutionRepository;

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
            if (surgicalCenter.getId() == -1) {
                surgicalCenter.setId(null);
            }
            surgicalCenterListPresenter.save(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
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
        // SUPER_ADMIN without institution context should not access surgical center data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
            return;
        }

        Optional<String> idParameter = event.getRouteParameters().get("id");
        if (idParameter.isEmpty()) {
            event.forwardTo(SurgicalCenterMainView.class);
            return;
        }

        try {
            Integer id = Integer.valueOf(idParameter.get());
            if (-1 == id) {
                // Create new surgical center - set institution from context
                SurgicalCenter newDto = new SurgicalCenter();
                newDto.setId(id);
                
                // Set institution from context if available
                if (hasInstitutionContext) {
                    Long institutionId = InstitutionContext.getInstitutionId();
                    Institution institution = institutionRepository.findById(institutionId)
                            .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
                    newDto.setInstitution(institution);
                }
                
                surgicalCenterLayout.setBean(newDto);
            } else {
                SurgicalCenter dto = surgicalCenterListPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(SurgicalCenterMainView.class);
                    return;
                }
                surgicalCenterLayout.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(SurgicalCenterMainView.class);
        } catch (IllegalStateException e) {
            // Institution not found or access denied
            event.forwardTo(SurgicalCenterMainView.class);
        }
    }
}
