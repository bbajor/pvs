package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

/**
 * Dashboard route that redirects to the main view.
 * This allows accessing the dashboard via /dashboard URL.
 */
@Route(value = "dashboard", layout = MainLayout.class)
@PermitAll
public class DashboardRedirectView extends Div implements BeforeEnterObserver {
    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo("");
    }
}

