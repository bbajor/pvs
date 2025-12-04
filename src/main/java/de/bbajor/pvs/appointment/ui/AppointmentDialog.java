package de.bbajor.pvs.appointment.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.AppointmentStatus;
import de.bbajor.pvs.appointment.service.AppointmentService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;

/**
 * Dialog for creating and editing appointments.
 * Includes patient selection, treatment linking, and appointment details.
 */
public class AppointmentDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(AppointmentDialog.class);

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final AppointmentScheduler scheduler;
    private final ApplicationContext applicationContext;
    
    private Appointment appointment;
    private Runnable onSaveCallback;

    // Form fields
    private ComboBox<Patient> patientComboBox;
    private DateTimePicker startTimePicker;
    private DateTimePicker endTimePicker;
    private TextField reasonField;
    private TextArea notesArea;
    private TextArea additionalInfoArea;
    private ComboBox<AppointmentStatus> statusComboBox;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public AppointmentDialog(
            AppointmentService appointmentService,
            PatientService patientService,
            AppointmentScheduler scheduler,
            Appointment appointment) {
        this(appointmentService, patientService, scheduler, appointment, null);
    }

    public AppointmentDialog(
            AppointmentService appointmentService,
            PatientService patientService,
            AppointmentScheduler scheduler,
            Appointment appointment,
            ApplicationContext applicationContext) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;
        this.appointment = appointment != null ? appointment : createNewAppointment();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        setCloseOnOutsideClick(false);
        
        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        initializeDialog();
    }

    private Appointment createNewAppointment() {
        Appointment apt = new Appointment();
        apt.setScheduler(scheduler);
        apt.setStatus(AppointmentStatus.SCHEDULED);
        apt.setCreatedAt(LocalDateTime.now());
        return apt;
    }

    private void initializeDialog() {
        H3 title = new H3(appointment.getId() != null ? "Termin bearbeiten" : "Neuer Termin");
        
        FormLayout formLayout = createFormLayout();
        
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );

        // InstitutionContext sicherstellen, bevor Patienten geladen werden
        ensureInstitutionContext();

        // Patient selection
        patientComboBox = new ComboBox<>("Patient");
        patientComboBox.setItems(patientService.getAll());
        patientComboBox.setItemLabelGenerator(patient -> 
            patient.getLastName() + ", " + patient.getFirstName() + " (geb. " + 
            patient.getBirth().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ")"
        );
        patientComboBox.setValue(appointment.getPatient());
        patientComboBox.setRequiredIndicatorVisible(true);
        patientComboBox.setPlaceholder("Patient auswählen");
        
        // Add button to create new patient if needed
        Button newPatientButton = new Button("Neuer Patient", event -> {
            showNewPatientInfo();
        });
        newPatientButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        // Start time
        startTimePicker = new DateTimePicker("Startzeit");
        startTimePicker.setValue(appointment.getStartTime());
        startTimePicker.setRequiredIndicatorVisible(true);
        
        // End time
        endTimePicker = new DateTimePicker("Endzeit");
        endTimePicker.setValue(appointment.getEndTime());
        endTimePicker.setRequiredIndicatorVisible(true);

        // Auto-calculate end time when start time changes (default 30 minutes)
        startTimePicker.addValueChangeListener(event -> {
            if (event.getValue() != null && endTimePicker.getValue() == null) {
                endTimePicker.setValue(event.getValue().plusMinutes(30));
            }
        });

        // Reason
        reasonField = new TextField("Grund des Besuchs");
        reasonField.setValue(appointment.getReason() != null ? appointment.getReason() : "");
        reasonField.setRequiredIndicatorVisible(true);
        reasonField.setPlaceholder("z.B. Kontrolluntersuchung, IVOM-Behandlung");

        // Status
        statusComboBox = new ComboBox<>("Status");
        statusComboBox.setItems(AppointmentStatus.values());
        statusComboBox.setValue(appointment.getStatus());
        statusComboBox.setItemLabelGenerator(this::translateStatus);

        // Notes
        notesArea = new TextArea("Bemerkungen");
        notesArea.setValue(appointment.getNotes() != null ? appointment.getNotes() : "");
        notesArea.setPlaceholder("Interne Notizen");

        // Additional info
        additionalInfoArea = new TextArea("Zusätzliche Informationen");
        additionalInfoArea.setValue(appointment.getAdditionalInfo() != null ? appointment.getAdditionalInfo() : "");
        additionalInfoArea.setPlaceholder("Weitere Informationen für die Behandlung");

        // Show next available slot button
        Button nextSlotButton = new Button("Nächster freier Termin", event -> findNextAvailableSlot());
        nextSlotButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        layout.add(patientComboBox, 1);
        layout.add(newPatientButton, 1);
        layout.add(startTimePicker, 1);
        layout.add(endTimePicker, 1);
        layout.add(nextSlotButton, 2);
        layout.add(reasonField, 2);
        layout.add(statusComboBox, 2);
        layout.add(notesArea, 2);
        layout.add(additionalInfoArea, 2);

        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button saveButton = new Button("Speichern", event -> saveAppointment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button deleteButton = new Button("Löschen", event -> deleteAppointment());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(appointment.getId() != null);

        layout.add(deleteButton, saveButton);
        return layout;
    }

    private void saveAppointment() {
        try {
            // Validate required fields
            if (patientComboBox.getValue() == null) {
                showError("Bitte wählen Sie einen Patienten aus");
                return;
            }
            if (startTimePicker.getValue() == null) {
                showError("Bitte geben Sie eine Startzeit an");
                return;
            }
            if (endTimePicker.getValue() == null) {
                showError("Bitte geben Sie eine Endzeit an");
                return;
            }
            if (reasonField.getValue() == null || reasonField.getValue().isBlank()) {
                showError("Bitte geben Sie einen Grund für den Besuch an");
                return;
            }

            // Update appointment
            appointment.setPatient(patientComboBox.getValue());
            appointment.setStartTime(startTimePicker.getValue());
            appointment.setEndTime(endTimePicker.getValue());
            appointment.setReason(reasonField.getValue());
            appointment.setStatus(statusComboBox.getValue());
            appointment.setNotes(notesArea.getValue());
            appointment.setAdditionalInfo(additionalInfoArea.getValue());
            appointment.setLastModifiedAt(LocalDateTime.now());

            appointmentService.save(appointment);

            showSuccess("Termin erfolgreich gespeichert");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (IllegalStateException | IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Fehler beim Speichern des Termins: " + e.getMessage());
        }
    }

    private void deleteAppointment() {
        try {
            appointmentService.delete(appointment);
            showSuccess("Termin erfolgreich gelöscht");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Fehler beim Löschen des Termins: " + e.getMessage());
        }
    }

    private void findNextAvailableSlot() {
        LocalDateTime from = LocalDateTime.now();
        appointmentService.findNextAvailableSlot(scheduler, from, 30)
            .ifPresentOrElse(
                slot -> {
                    startTimePicker.setValue(slot);
                    endTimePicker.setValue(slot.plusMinutes(30));
                    showSuccess("Nächster freier Termin: " + slot.format(DATETIME_FORMATTER));
                },
                () -> showError("Kein freier Termin in den nächsten 4 Wochen gefunden")
            );
    }

    private void showNewPatientInfo() {
        Notification notification = Notification.show(
            "Bitte legen Sie zuerst einen neuen Patienten in der Patientenverwaltung an.",
            5000,
            Notification.Position.MIDDLE
        );
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String translateStatus(AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> "Geplant";
            case ARRIVED -> "Angekommen";
            case IN_PROGRESS -> "In Behandlung";
            case COMPLETED -> "Abgeschlossen";
            case CANCELLED -> "Abgesagt";
            case NO_SHOW -> "Nicht erschienen";
        };
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     * Dies ist notwendig, da Vaadin Button-Klicks kein BeforeEnterEvent auslösen,
     * sodass der Context möglicherweise nicht gesetzt ist.
     */
    private void ensureInstitutionContext() {
        // Nur setzen, wenn noch nicht gesetzt
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication wurde aus Session deserialisiert
            if (applicationContext != null) {
                try {
                    UserAccountRepository userAccountRepository = applicationContext.getBean(UserAccountRepository.class);
                    String username = adapter.getUsername();
                    UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                        log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                                institutionId, userAccount.getInstitution().getInstitutionCode());
                    }
                } catch (Exception e) {
                    log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
                }
            }
        }
    }
}
