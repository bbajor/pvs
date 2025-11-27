package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import de.bbajor.pvs.institution.model.EmailEncryptionMethod;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.model.InstitutionEmailContact;
import de.bbajor.pvs.institution.service.InstitutionEmailContactService;
import org.springframework.stereotype.Component;

/**
 * Dialog for managing email contacts of an institution with OpenPGP keys.
 */
@Component
public class InstitutionEmailContactDialog extends Dialog {

    private final InstitutionEmailContactService emailContactService;
    private Institution institution;
    private Grid<InstitutionEmailContact> grid;
    private TextField emailField;
    private TextField displayNameField;
    private ComboBox<EmailEncryptionMethod> encryptionMethodComboBox;
    private TextArea openpgpKeyField;
    private TextArea smimeCertificateField;
    private TextField keyIdField;
    private TextField fingerprintField;
    private Checkbox activeCheckbox;
    private TextArea notesField;
    private Span encryptionWarning;
    private Button saveButton;
    private Button cancelButton;
    private InstitutionEmailContact currentContact;

    public InstitutionEmailContactDialog(InstitutionEmailContactService emailContactService) {
        this.emailContactService = emailContactService;
        
        setWidth("800px");
        setHeight("90vh");
        setResizable(true);
        setDraggable(true);
        setCloseOnOutsideClick(false);

        H3 title = new H3("E-Mail-Kontakte verwalten");
        
        // Grid for existing contacts
        grid = new Grid<>(InstitutionEmailContact.class, false);
        grid.addColumn(InstitutionEmailContact::getEmail).setHeader("E-Mail").setSortable(true);
        grid.addColumn(InstitutionEmailContact::getDisplayName).setHeader("Anzeigename");
        grid.addColumn(contact -> {
            EmailEncryptionMethod method = contact.getEncryptionMethod();
            if (method == null) {
                method = (contact.getOpenpgpPublicKey() != null && !contact.getOpenpgpPublicKey().isEmpty()) 
                        ? EmailEncryptionMethod.OPENPGP 
                        : EmailEncryptionMethod.NONE;
            }
            return method.getDisplayName();
        }).setHeader("Verschlüsselung").setSortable(true);
        grid.addColumn(contact -> contact.getActive() != null && contact.getActive() ? "Aktiv" : "Inaktiv")
                .setHeader("Status").setSortable(true);
        
        grid.addComponentColumn(contact -> {
            Button editButton = new Button("Bearbeiten", e -> editContact(contact));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            Button deleteButton = new Button("Löschen", e -> deleteContact(contact));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editButton, deleteButton);
        }).setHeader("Aktionen");
        
        grid.setHeight("300px");
        grid.addItemClickListener(e -> editContact(e.getItem()));

        // Form for adding/editing contacts
        emailField = new TextField("E-Mail-Adresse");
        emailField.setRequired(true);
        emailField.setWidthFull();

        displayNameField = new TextField("Anzeigename");
        displayNameField.setPlaceholder("z.B. Hauptkontakt, IT-Support");
        displayNameField.setWidthFull();

        encryptionMethodComboBox = new ComboBox<>("Verschlüsselungsmethode");
        encryptionMethodComboBox.setItems(EmailEncryptionMethod.values());
        encryptionMethodComboBox.setItemLabelGenerator(method -> method.getDisplayName() + " - " + method.getDescription());
        encryptionMethodComboBox.setWidthFull();
        encryptionMethodComboBox.setRequired(true);
        encryptionMethodComboBox.setRequiredIndicatorVisible(true);
        
        // Initialize ALL fields BEFORE setting the value (which triggers the listener)
        encryptionWarning = new Span();
        encryptionWarning.getStyle().set("color", "var(--lumo-error-color)");
        encryptionWarning.getStyle().set("font-weight", "bold");
        encryptionWarning.getStyle().set("padding", "0.5em");
        encryptionWarning.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
        encryptionWarning.getStyle().set("border-radius", "4px");
        encryptionWarning.getStyle().set("display", "none");

        openpgpKeyField = new TextArea("OpenPGP Public Key");
        openpgpKeyField.setPlaceholder("Fügen Sie hier den ASCII-armored OpenPGP Public Key ein");
        openpgpKeyField.setWidthFull();
        openpgpKeyField.setHeight("150px");
        openpgpKeyField.setHelperText("Der Key wird automatisch validiert und Key-ID sowie Fingerprint extrahiert");

        smimeCertificateField = new TextArea("S/MIME Zertifikat (PEM-Format)");
        smimeCertificateField.setPlaceholder("-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----");
        smimeCertificateField.setWidthFull();
        smimeCertificateField.setHeight("150px");
        smimeCertificateField.setHelperText("X.509 Zertifikat im PEM-Format für S/MIME-Verschlüsselung");

        keyIdField = new TextField("Key ID");
        keyIdField.setReadOnly(true);
        keyIdField.setWidthFull();
        keyIdField.setHelperText("Wird automatisch aus dem Public Key extrahiert");

        fingerprintField = new TextField("Fingerprint");
        fingerprintField.setReadOnly(true);
        fingerprintField.setWidthFull();
        fingerprintField.setHelperText("Wird automatisch aus dem Public Key extrahiert");
        
        // Add listener AFTER all fields are initialized
        encryptionMethodComboBox.addValueChangeListener(e -> updateEncryptionFieldsVisibility());
        // Set default value to NONE (will be overridden when editing existing contact)
        encryptionMethodComboBox.setValue(EmailEncryptionMethod.NONE);

        activeCheckbox = new Checkbox("Aktiv");
        activeCheckbox.setValue(true);

        notesField = new TextArea("Notizen");
        notesField.setPlaceholder("Optionale Notizen zu diesem Kontakt");
        notesField.setWidthFull();
        notesField.setHeight("100px");

        // Validate OpenPGP key when pasted
        openpgpKeyField.addValueChangeListener(e -> {
            String key = e.getValue();
            if (key != null && !key.trim().isEmpty()) {
                if (emailContactService.isValidOpenPgpKey(key)) {
                    try {
                        // Extract key info (this will be done on save, but we can show preview)
                        Notification.show("OpenPGP-Schlüssel ist gültig", 2000, Notification.Position.BOTTOM_START)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception ex) {
                        Notification.show("Fehler beim Validieren des Schlüssels: " + ex.getMessage(),
                                3000, Notification.Position.BOTTOM_START)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } else {
                    Notification.show("Ungültiger OpenPGP-Schlüssel", 3000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        formLayout.add(emailField, 2);
        formLayout.add(displayNameField, 2);
        formLayout.add(encryptionMethodComboBox, 2);
        formLayout.add(encryptionWarning, 2);
        formLayout.add(openpgpKeyField, 2);
        formLayout.add(smimeCertificateField, 2);
        formLayout.add(keyIdField);
        formLayout.add(fingerprintField);
        formLayout.add(activeCheckbox);
        formLayout.add(notesField, 2);

        saveButton = new Button("Speichern", e -> saveContact());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        cancelButton = new Button("Abbrechen", e -> {
            clearForm();
            close();
        });

        Button newButton = new Button("Neuer Kontakt", e -> newContact());
        newButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout buttonLayout = new HorizontalLayout(newButton, saveButton, cancelButton);
        buttonLayout.setSpacing(true);

        VerticalLayout content = new VerticalLayout(title, grid, formLayout, buttonLayout);
        content.setSpacing(true);
        content.setPadding(true);
        content.setSizeFull();

        add(content);
    }

    public void openForInstitution(Institution institution) {
        this.institution = institution;
        refreshGrid();
        clearForm();
        open();
    }

    private void refreshGrid() {
        if (institution != null) {
            grid.setItems(emailContactService.findByInstitution(institution));
        }
    }

    private void newContact() {
        currentContact = null;
        clearForm();
    }

    private void editContact(InstitutionEmailContact contact) {
        currentContact = contact;
        emailField.setValue(contact.getEmail() != null ? contact.getEmail() : "");
        displayNameField.setValue(contact.getDisplayName() != null ? contact.getDisplayName() : "");
        
        // Set encryption method - default to OPENPGP if key exists, otherwise NONE
        EmailEncryptionMethod encryptionMethod = contact.getEncryptionMethod();
        if (encryptionMethod == null) {
            encryptionMethod = (contact.getOpenpgpPublicKey() != null && !contact.getOpenpgpPublicKey().isEmpty()) 
                    ? EmailEncryptionMethod.OPENPGP 
                    : EmailEncryptionMethod.NONE;
        }
        encryptionMethodComboBox.setValue(encryptionMethod);
        
        openpgpKeyField.setValue(contact.getOpenpgpPublicKey() != null ? contact.getOpenpgpPublicKey() : "");
        smimeCertificateField.setValue(contact.getSmimeCertificate() != null ? contact.getSmimeCertificate() : "");
        keyIdField.setValue(contact.getKeyId() != null ? contact.getKeyId() : "");
        fingerprintField.setValue(contact.getKeyFingerprint() != null ? contact.getKeyFingerprint() : "");
        activeCheckbox.setValue(contact.getActive() != null ? contact.getActive() : Boolean.TRUE);
        notesField.setValue(contact.getNotes() != null ? contact.getNotes() : "");
        
        updateEncryptionFieldsVisibility();
    }
    
    private void updateEncryptionFieldsVisibility() {
        // Safety check: return early if fields are not yet initialized
        if (encryptionWarning == null || openpgpKeyField == null || smimeCertificateField == null 
                || keyIdField == null || fingerprintField == null) {
            return;
        }
        
        EmailEncryptionMethod method = encryptionMethodComboBox.getValue();
        
        if (method == null || method == EmailEncryptionMethod.NONE) {
            encryptionWarning.setText("⚠️ WARNUNG: Keine Verschlüsselung ausgewählt! " +
                    "E-Mails werden unverschlüsselt versendet. Es wird dringend empfohlen, eine Verschlüsselungsmethode zu verwenden.");
            encryptionWarning.getStyle().set("display", "block");
            openpgpKeyField.setVisible(false);
            smimeCertificateField.setVisible(false);
            keyIdField.setVisible(false);
            fingerprintField.setVisible(false);
        } else {
            encryptionWarning.getStyle().set("display", "none");
            
            if (method == EmailEncryptionMethod.OPENPGP) {
                openpgpKeyField.setVisible(true);
                smimeCertificateField.setVisible(false);
                keyIdField.setVisible(true);
                fingerprintField.setVisible(true);
            } else if (method == EmailEncryptionMethod.SMIME) {
                openpgpKeyField.setVisible(false);
                smimeCertificateField.setVisible(true);
                keyIdField.setVisible(false);
                fingerprintField.setVisible(false);
            }
        }
    }

    private void saveContact() {
        if (emailField.getValue() == null || emailField.getValue().trim().isEmpty()) {
            Notification.show("E-Mail-Adresse ist erforderlich", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        EmailEncryptionMethod method = encryptionMethodComboBox.getValue();
        if (method == null) {
            Notification.show("Bitte wählen Sie eine Verschlüsselungsmethode aus", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Validate that required fields are present based on encryption method
        if (method == EmailEncryptionMethod.OPENPGP) {
            String pgpKey = openpgpKeyField.getValue();
            if (pgpKey == null || pgpKey.trim().isEmpty()) {
                Notification.show("Für OpenPGP-Verschlüsselung ist ein Public Key erforderlich", 
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (!emailContactService.isValidOpenPgpKey(pgpKey)) {
                Notification.show("Ungültiger OpenPGP-Schlüssel", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
        } else if (method == EmailEncryptionMethod.SMIME) {
            String cert = smimeCertificateField.getValue();
            if (cert == null || cert.trim().isEmpty()) {
                Notification.show("Für S/MIME-Verschlüsselung ist ein Zertifikat erforderlich", 
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            // TODO: Validate S/MIME certificate format
        } else if (method == EmailEncryptionMethod.NONE) {
            // Show warning but allow saving
            Notification.show("⚠️ Warnung: Keine Verschlüsselung ausgewählt. " +
                    "E-Mails werden unverschlüsselt versendet.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        }

        try {
            if (currentContact == null) {
                currentContact = new InstitutionEmailContact();
                currentContact.setInstitution(institution);
            }

            currentContact.setEmail(emailField.getValue().trim());
            currentContact.setDisplayName(displayNameField.getValue() != null ? displayNameField.getValue().trim() : null);
            currentContact.setEncryptionMethod(method);
            
            // Set encryption-specific fields
            if (method == EmailEncryptionMethod.OPENPGP) {
                currentContact.setOpenpgpPublicKey(openpgpKeyField.getValue() != null && !openpgpKeyField.getValue().trim().isEmpty()
                        ? openpgpKeyField.getValue().trim() : null);
                currentContact.setSmimeCertificate(null);
            } else if (method == EmailEncryptionMethod.SMIME) {
                currentContact.setOpenpgpPublicKey(null);
                currentContact.setSmimeCertificate(smimeCertificateField.getValue() != null && !smimeCertificateField.getValue().trim().isEmpty()
                        ? smimeCertificateField.getValue().trim() : null);
            } else {
                // NONE - clear both
                currentContact.setOpenpgpPublicKey(null);
                currentContact.setSmimeCertificate(null);
            }
            
            currentContact.setActive(activeCheckbox.getValue() != null ? activeCheckbox.getValue() : Boolean.TRUE);
            currentContact.setNotes(notesField.getValue() != null ? notesField.getValue().trim() : null);

            emailContactService.save(currentContact);
            
            Notification.show("E-Mail-Kontakt gespeichert", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            refreshGrid();
            clearForm();
        } catch (Exception e) {
            Notification.show("Fehler beim Speichern: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteContact(InstitutionEmailContact contact) {
        try {
            emailContactService.delete(contact);
            Notification.show("E-Mail-Kontakt gelöscht", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
            if (currentContact != null && currentContact.getId().equals(contact.getId())) {
                clearForm();
            }
        } catch (Exception e) {
            Notification.show("Fehler beim Löschen: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void clearForm() {
        currentContact = null;
        emailField.clear();
        displayNameField.clear();
        // Set default encryption method to NONE for new contacts
        encryptionMethodComboBox.setValue(EmailEncryptionMethod.NONE);
        openpgpKeyField.clear();
        smimeCertificateField.clear();
        keyIdField.clear();
        fingerprintField.clear();
        activeCheckbox.setValue(true);
        notesField.clear();
        updateEncryptionFieldsVisibility();
    }
}

