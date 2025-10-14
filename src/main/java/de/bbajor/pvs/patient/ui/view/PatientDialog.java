package de.bbajor.pvs.patient.ui.view;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.ValidationException;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.presenter.PatientPresenter;

public class PatientDialog extends Dialog {

    private List<PatientChangeListener> listeners = new ArrayList<>();
    private final Button saveButton = new Button();

    private final PatientPresenter presenter;
    private final PatientForm form;

    public PatientDialog(PatientPresenter presenter, Patient patient) {
        this.presenter = presenter;
        if (patient == null) {
            patient = new Patient();
        }

        setWidth("1400px");
        setHeight("1000px");

        // Create the components
        form = new PatientForm(presenter.getHealthInsurances(), patient, e -> valueChanged(e));

        var readBtn = new Button("Aus Gesundheitskarte einlesen", event -> {
            try {
                form.setValue(presenter.readDataFromEgk());
            } catch (Exception e) {
                Notification.show("Einlesen der Karte nicht erfolgreich: " + e.getMessage());
            }
        });
        readBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        var saveLbl = patient == null || patient.getId() == null ? "Erstellen"
                : "Aktualisieren";
        saveButton.setText(saveLbl);
        saveButton.setEnabled(form.isValidateOk());
        saveButton.addClickListener(event -> {
            try {
                save();
            } catch (ValidationException e) {
                Notification.show("Patientendaten konnten nicht gespeichert werden:" + e.getMessage());
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelBtn = new Button("Abbrechen", event -> close());

        // Configure the dialog
        String title = patient == null || patient.getId() == null ? "Neuer Patient"
                : "Patient " + patient.toString();
        setHeaderTitle(title);
        add(form);
        getFooter().add(cancelBtn, readBtn, saveButton);
    }

    public void addChangeListener(PatientChangeListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners() {
        listeners.forEach(e -> e.onPatientChanged(form.getValue()));
    }

    private void save() throws ValidationException {
        if (form.isValidateOk()) {
            form.writeIfValid();
            presenter.savePatient(form.getValue());
            notifyListeners();
            close();
        } else {
            Notification.show("Es fehlen noch Angaben. Bitte ergänzen.");
        }
    }

    public void valueChanged(ValueChangeEvent<?> event) {
        saveButton.setEnabled(form.isValidateOk());
    }

}