package de.bbajor.pvs.settings.ui.tabs;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Component
public class MedicationSettingsTab extends VerticalLayout {

    public MedicationSettingsTab() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Show message with link instead of redirect
        Span message = new Span("Zur Verwaltung der Medikamentendatenbank:");
        Anchor link = new Anchor("/ivom-drugs", "Medikamentendatenbank öffnen");
        link.getStyle().set("font-weight", "bold");
        link.getStyle().set("text-decoration", "underline");
        
        add(message, link);
    }

}

