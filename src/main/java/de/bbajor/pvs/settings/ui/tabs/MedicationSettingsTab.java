package de.bbajor.pvs.settings.ui.tabs;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MedicationSettingsTab extends VerticalLayout {

    public MedicationSettingsTab() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Show message with information about medication database
        Span message = new Span("Die Medikamentendatenbank wird direkt in diesem Tab verwaltet. " +
                "Bitte laden Sie die Arzneimitteldaten als CSV von folgender Seite herunter:");
        message.getStyle().set("display", "block");
        message.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        
        Anchor link = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        link.setTarget("_blank");
        link.getStyle().set("font-weight", "bold");
        
        add(message, link);
    }

}

