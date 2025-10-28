package de.bbajor.pvs.settings.ui.tabs;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.bbajor.pvs.security.domain.UserAccountRepository;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserSettingsTab extends VerticalLayout {

    public UserSettingsTab(UserAccountRepository userAccountRepository) {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Show message with link instead of redirect
        Span message = new Span("Zur Verwaltung der Benutzer:");
        Anchor link = new Anchor("/admin/users", "Benutzerverwaltung öffnen");
        link.getStyle().set("font-weight", "bold");
        link.getStyle().set("text-decoration", "underline");
        
        add(message, link);
    }

}

