package de.bbajor.pvs.ai.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.bbajor.pvs.ai.extraction.ExtractionResult;
import de.bbajor.pvs.patient.model.Patient;

public class EntityVerificationDialog<T> extends Dialog {

    private OnConfirmedListener<T> confirmListener;

    public EntityVerificationDialog(ExtractionResult<T> result) {
        setWidth("800px");
        setHeight("600px");
        setHeaderTitle("Patientendaten überprüfen");
        setCloseOnOutsideClick(false);

        // Show confidence indicator
        double confidence = result.getConfidence();
        Span confidenceSpan = new Span(String.format("Erkennungsgenauigkeit: %.0f%%", confidence * 100));
        if (confidence >= 0.8) {
            confidenceSpan.getStyle().set("color", "var(--lumo-success-color)");
        } else if (confidence >= 0.6) {
            confidenceSpan.getStyle().set("color", "var(--lumo-warning-color)");
        } else {
            confidenceSpan.getStyle().set("color", "var(--lumo-error-color)");
        }

        // Show extracted entity (for Patient, we'll use PatientForm)
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        if (result.getEntity() instanceof Patient patient) {
            content.add(new Span("Extrahierte Daten:"));
            content.add(createInfoRow("Vorname", patient.getFirstName(), 
                    result.getFieldConfidences().getOrDefault("name", 0.0)));
            content.add(createInfoRow("Nachname", patient.getLastName(), 
                    result.getFieldConfidences().getOrDefault("name", 0.0)));
            content.add(createInfoRow("Geburtsdatum", 
                    patient.getBirth() != null ? patient.getBirth().toString() : "-",
                    result.getFieldConfidences().getOrDefault("birth", 0.0)));
            if (patient.getAddress() != null) {
                content.add(createInfoRow("Adresse", patient.getAddress().toString(), 
                        result.getFieldConfidences().getOrDefault("address", 0.0)));
            }
            if (patient.getHealthInsurance() != null) {
                content.add(createInfoRow("Versicherung", patient.getHealthInsurance().toString(), 
                        result.getFieldConfidences().getOrDefault("insurance", 0.0)));
            }
            if (patient.getInsuranceNumber() != null) {
                content.add(createInfoRow("Versichertennummer", patient.getInsuranceNumber(), 
                        result.getFieldConfidences().getOrDefault("insuranceNumber", 0.0)));
            }
        }

        content.add(confidenceSpan);

        Button cancelButton = new Button("Abbrechen", e -> close());
        Button confirmButton = new Button("Übernehmen", e -> {
            if (confirmListener != null) {
                confirmListener.onConfirmed(result.getEntity());
            }
            close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(content);
        getFooter().add(cancelButton, confirmButton);
    }

    private HorizontalLayout createInfoRow(String label, String value, double confidence) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "bold");
        labelSpan.setWidth("150px");

        Span valueSpan = new Span(value != null ? value : "-");
        valueSpan.setWidth("300px");

        Icon confidenceIcon;
        if (confidence >= 0.8) {
            confidenceIcon = VaadinIcon.CHECK_CIRCLE.create();
            confidenceIcon.setColor("var(--lumo-success-color)");
        } else if (confidence >= 0.6) {
            confidenceIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            confidenceIcon.setColor("var(--lumo-warning-color)");
        } else {
            confidenceIcon = VaadinIcon.CLOSE_CIRCLE.create();
            confidenceIcon.setColor("var(--lumo-error-color)");
        }

        row.add(labelSpan, valueSpan, confidenceIcon);
        return row;
    }

    public void setOnConfirmedListener(OnConfirmedListener<T> listener) {
        this.confirmListener = listener;
    }

    @FunctionalInterface
    public interface OnConfirmedListener<T> {
        void onConfirmed(T entity);
    }

}

