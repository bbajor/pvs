package de.bbajor.pvs.patient.ui.view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.presenter.PatientPresenter;

class PatientDialogTest {

    private PatientPresenter presenter;
    private Patient patient;

    @BeforeEach
    void setUp() {
        presenter = mock(PatientPresenter.class);
        when(presenter.getHealthInsurances()).thenReturn(Collections.emptyList());
        when(presenter.getDrugs()).thenReturn(Collections.emptyList());
        patient = new Patient();
        patient.setFirstName("Test");
        patient.setLastName("Patient");
    }

    @Test
    void testDialogInitializesWithCorrectTitleForNewPatient() {
        PatientDialog dialog = new PatientDialog(presenter, null);
        assertEquals("Neuer Patient", dialog.getHeaderTitle());
    }

    @Test
    void testDialogInitializesWithCorrectTitleForExistingPatient() {
        patient.setId(123);
        when(presenter.getHealthInsurances()).thenReturn(Collections.emptyList());
        when(presenter.getDrugs()).thenReturn(Collections.emptyList());
        PatientDialog dialog = new PatientDialog(presenter, patient);
        assertTrue(dialog.getHeaderTitle().contains("Patient"));
    }

    @Test
    void testSaveButtonLabelForNewPatient() throws Exception {
        PatientDialog dialog = new PatientDialog(presenter, null);
        var saveButtonField = PatientDialog.class.getDeclaredField("saveButton");
        saveButtonField.setAccessible(true);
        Button saveButton = (Button) saveButtonField.get(dialog);
        assertEquals("Erstellen", saveButton.getText());
    }

    @Test
    void testSaveButtonLabelForExistingPatient() throws Exception {
        patient.setId(1);
        when(presenter.getHealthInsurances()).thenReturn(Collections.emptyList());
        when(presenter.getDrugs()).thenReturn(Collections.emptyList());
        PatientDialog dialog = new PatientDialog(presenter, patient);
        var saveButtonField = PatientDialog.class.getDeclaredField("saveButton");
        saveButtonField.setAccessible(true);
        Button saveButton = (Button) saveButtonField.get(dialog);
        assertEquals("Aktualisieren", saveButton.getText());
    }

    @Test
    void testAddChangeListenerAndNotifyListeners() {
        PatientDialog dialog = new PatientDialog(presenter, null);
        AtomicBoolean called = new AtomicBoolean(false);
        dialog.addChangeListener(patient -> called.set(true));
        // Simulate notifyListeners (protected, so use reflection)
        assertDoesNotThrow(() -> {
            var method = PatientDialog.class.getDeclaredMethod("notifyListeners");
            method.setAccessible(true);
            method.invoke(dialog);
        });
        assertTrue(called.get());
    }

    @Test
    void testSaveCallsPresenterSaveWhenValid() throws Exception {
        PatientDialog dialog = new PatientDialog(presenter, null);

        // Mock form to always validate OK
        var formField = PatientDialog.class.getDeclaredField("form");
        formField.setAccessible(true);
        PatientForm form = mock(PatientForm.class);
        when(form.isValidateOk()).thenReturn(true);
        when(form.getValue()).thenReturn(patient);
        formField.set(dialog, form);

        // Call save via reflection (private)
        var saveMethod = PatientDialog.class.getDeclaredMethod("save");
        saveMethod.setAccessible(true);
        saveMethod.invoke(dialog);

        verify(form).writeIfValid();
        verify(presenter).savePatient(any(Patient.class));
    }

    @Test
    void testSaveDoesNotCallPresenterSaveWhenInvalid() throws Exception {
        try (var notificationMock = mockStatic(Notification.class)) {

            PatientDialog dialog = new PatientDialog(presenter, null);

            var formField = PatientDialog.class.getDeclaredField("form");
            formField.setAccessible(true);
            PatientForm form = mock(PatientForm.class);
            when(form.isValidateOk()).thenReturn(false);
            formField.set(dialog, form);

            var saveMethod = PatientDialog.class.getDeclaredMethod("save");
            saveMethod.setAccessible(true);
            saveMethod.invoke(dialog);

            verify(form, never()).writeIfValid();
            verify(presenter, never()).savePatient(any(Patient.class));
        }
    }

    @Test
    void testValueChangedEnablesSaveButtonWhenValid() throws Exception {
        PatientDialog dialog = new PatientDialog(presenter, null);
        var formField = PatientDialog.class.getDeclaredField("form");
        formField.setAccessible(true);
        PatientForm form = mock(PatientForm.class);
        when(form.isValidateOk()).thenReturn(true);
        formField.set(dialog, form);

        var saveButtonField = PatientDialog.class.getDeclaredField("saveButton");
        saveButtonField.setAccessible(true);
        Button saveButton = (Button) saveButtonField.get(dialog);

        dialog.valueChanged(null);
        assertTrue(saveButton.isEnabled());
    }

    @Test
    void testValueChangedDisablesSaveButtonWhenInvalid() throws Exception {
        PatientDialog dialog = new PatientDialog(presenter, null);
        var formField = PatientDialog.class.getDeclaredField("form");
        formField.setAccessible(true);
        PatientForm form = mock(PatientForm.class);
        when(form.isValidateOk()).thenReturn(false);
        formField.set(dialog, form);

        var saveButtonField = PatientDialog.class.getDeclaredField("saveButton");
        saveButtonField.setAccessible(true);
        Button saveButton = (Button) saveButtonField.get(dialog);

        dialog.valueChanged(null);
        assertFalse(saveButton.isEnabled());
    }
}