package de.bbajor.pvs.settings.ui.tabs;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PracticeSettingsTab extends VerticalLayout {

    public PracticeSettingsTab() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Show message - Location management is now handled via Institution management
        Span message = new Span("Standortverwaltung erfolgt über die Institutionsverwaltung.");
        
        add(message);
    }

}

