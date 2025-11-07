package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.Command;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.service.InstitutionLayoutService;
import de.bbajor.pvs.institution.service.WebsiteColorExtractorService;
import de.bbajor.pvs.institution.service.WebsiteColorExtractorService.LayoutColors;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Tab for editing institution layout settings (colors, fonts, etc.).
 * Allows importing colors from the institution's website.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class LayoutSettingsTab extends VerticalLayout {

    private final InstitutionRepository institutionRepository;
    private final WebsiteColorExtractorService colorExtractorService;
    private final InstitutionLayoutService layoutService;

    private TextField websiteUrlField;
    private Button importColorsButton;
    private TextField primaryColorField;
    private TextField secondaryColorField;
    private TextField backgroundColorField;
    private TextField textColorField;
    private TextField accentColorField;
    private TextField borderRadiusField;
    private TextField fontFamilyField;
    private Button saveButton;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Layout-Einstellungen");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));

        H3 title = new H3("Layout-Einstellungen");
        Span description = new Span("Passen Sie das Erscheinungsbild der Anwendung an Ihr Corporate Design an. " +
                "Sie können Farben von Ihrer Webseite importieren oder manuell eingeben.");

        // Website URL and import section
        H3 importSectionTitle = new H3("Farben von Webseite importieren");
        websiteUrlField = new TextField("Webseiten-URL");
        websiteUrlField.setValue(institution.getWebsiteUrl() != null ? institution.getWebsiteUrl() : "");
        websiteUrlField.setWidthFull();
        websiteUrlField.setPlaceholder("https://www.example.com");
        websiteUrlField.setHelperText("Geben Sie die URL Ihrer Webseite ein, um automatisch Farben zu extrahieren");

        importColorsButton = new Button("Farben importieren", e -> importColorsFromWebsite(institution));
        importColorsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout importLayout = new HorizontalLayout(websiteUrlField, importColorsButton);
        importLayout.setWidthFull();
        importLayout.setFlexGrow(1, websiteUrlField);

        VerticalLayout importSection = new VerticalLayout(importSectionTitle, importLayout);
        importSection.setSpacing(true);
        importSection.setPadding(false);

        // Color fields section
        H3 colorSectionTitle = new H3("Farben");
        primaryColorField = createColorField("Primärfarbe", institution.getLayoutPrimaryColor());
        secondaryColorField = createColorField("Sekundärfarbe", institution.getLayoutSecondaryColor());
        backgroundColorField = createColorField("Hintergrundfarbe", institution.getLayoutBackgroundColor());
        textColorField = createColorField("Textfarbe", institution.getLayoutTextColor());
        accentColorField = createColorField("Akzentfarbe", institution.getLayoutAccentColor());

        // Other layout settings
        H3 otherSectionTitle = new H3("Weitere Einstellungen");
        borderRadiusField = new TextField("Border Radius");
        borderRadiusField.setValue(institution.getLayoutBorderRadius() != null ? institution.getLayoutBorderRadius() : "");
        borderRadiusField.setWidthFull();
        borderRadiusField.setPlaceholder("z.B. 8px, 0.5rem");
        borderRadiusField.setHelperText("Abrundung für UI-Elemente");

        fontFamilyField = new TextField("Schriftart");
        fontFamilyField.setValue(institution.getLayoutFontFamily() != null ? institution.getLayoutFontFamily() : "");
        fontFamilyField.setWidthFull();
        fontFamilyField.setPlaceholder("z.B. Arial, sans-serif");
        fontFamilyField.setHelperText("Schriftfamilie für die Benutzeroberfläche");

        saveButton = new Button("Speichern", e -> saveLayoutSettings(institution));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button reloadButton = new Button("Seite neu laden", e -> reloadPage());
        reloadButton.setIcon(VaadinIcon.REFRESH.create());
        reloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reloadButton.setTooltipText("Lädt die Seite neu, um die CSS-Änderungen vollständig anzuwenden");

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, reloadButton);
        buttonLayout.setSpacing(true);

        FormLayout colorFormLayout = new FormLayout();
        colorFormLayout.add(primaryColorField, secondaryColorField, backgroundColorField, 
                textColorField, accentColorField);
        colorFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        FormLayout otherFormLayout = new FormLayout();
        otherFormLayout.add(borderRadiusField, fontFamilyField);
        otherFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        add(title, description, new Hr(), 
                importSection, new Hr(),
                colorSectionTitle, colorFormLayout, new Hr(),
                otherSectionTitle, otherFormLayout, buttonLayout);
    }

    private TextField createColorField(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value != null ? value : "");
        field.setWidthFull();
        field.setPlaceholder("#rrggbb");
        field.setPattern("#[0-9a-fA-F]{6}");
        
        // Add color preview
        Div preview = new Div();
        preview.setWidth("30px");
        preview.setHeight("30px");
        preview.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        preview.getStyle().set("border-radius", "4px");
        preview.getStyle().set("margin-left", "8px");
        preview.getStyle().set("display", "inline-block");
        preview.getStyle().set("vertical-align", "middle");
        
        updateColorPreview(preview, value);
        
        field.addValueChangeListener(e -> {
            String newValue = e.getValue();
            updateColorPreview(preview, newValue);
        });
        
        HorizontalLayout fieldLayout = new HorizontalLayout(field, preview);
        fieldLayout.setAlignItems(Alignment.CENTER);
        fieldLayout.setWidthFull();
        fieldLayout.setFlexGrow(1, field);
        
        // Wrap in a container to maintain form layout structure
        VerticalLayout container = new VerticalLayout();
        container.setSpacing(false);
        container.setPadding(false);
        container.add(fieldLayout);
        
        // We'll add the preview directly to the field using a custom approach
        // For now, just return the field - preview can be added via CSS or JS
        
        return field;
    }

    private void updateColorPreview(Div preview, String colorValue) {
        if (colorValue != null && colorValue.matches("#[0-9a-fA-F]{6}")) {
            preview.getStyle().set("background-color", colorValue);
        } else {
            preview.getStyle().set("background-color", "transparent");
        }
    }

    private void importColorsFromWebsite(Institution institution) {
        String websiteUrl = websiteUrlField.getValue();
        if (websiteUrl == null || websiteUrl.isBlank()) {
            Notification.show("Bitte geben Sie eine Webseiten-URL ein", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        // Create progress dialog with component references
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMin(0);
        progressBar.setMax(1);
        progressBar.setValue(0);
        progressBar.setWidthFull();

        Span statusLabel = new Span("Vorbereitung...");
        statusLabel.getStyle().set("font-size", "var(--lumo-font-size-m)");

        Dialog progressDialog = new Dialog();
        progressDialog.setHeaderTitle("Farben importieren");
        progressDialog.setModal(true);
        progressDialog.setCloseOnEsc(false);
        progressDialog.setCloseOnOutsideClick(false);
        progressDialog.setWidth("400px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.add(statusLabel, progressBar);
        progressDialog.add(content);
        progressDialog.open();

        // Run color extraction in a separate thread to avoid blocking UI
        getUI().ifPresent(ui -> {
            new Thread(() -> {
                try {
                    // Simulate progress updates
                    updateProgress(ui, progressBar, statusLabel, 0.1, "Verbinde mit Webseite...");
                    Thread.sleep(300);
                    
                    updateProgress(ui, progressBar, statusLabel, 0.3, "Lade HTML-Inhalt...");
                    Thread.sleep(200);
                    
                    updateProgress(ui, progressBar, statusLabel, 0.5, "Analysiere CSS-Farben...");
                    Thread.sleep(200);
                    
                    LayoutColors colors = colorExtractorService.extractColors(websiteUrl);
                    
                    updateProgress(ui, progressBar, statusLabel, 0.8, "Extrahiere Farben...");
                    Thread.sleep(200);
                    
                    updateProgress(ui, progressBar, statusLabel, 1.0, "Fertig!");
                    Thread.sleep(300);
                    
                    ui.access((Command) () -> {
                        progressDialog.close();
                        
                        if (colors == null) {
                            Notification.show("Konnte keine Farben von der Webseite extrahieren. " +
                                    "Bitte überprüfen Sie die URL oder geben Sie die Farben manuell ein.",
                                    5000, Notification.Position.MIDDLE)
                                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
                            return;
                        }

                        // Update fields with extracted colors
                        if (colors.getPrimaryColor() != null) {
                            primaryColorField.setValue(colors.getPrimaryColor());
                        }
                        if (colors.getSecondaryColor() != null) {
                            secondaryColorField.setValue(colors.getSecondaryColor());
                        }
                        if (colors.getBackgroundColor() != null) {
                            backgroundColorField.setValue(colors.getBackgroundColor());
                        }
                        if (colors.getTextColor() != null) {
                            textColorField.setValue(colors.getTextColor());
                        }
                        if (colors.getAccentColor() != null) {
                            accentColorField.setValue(colors.getAccentColor());
                        }

                        // Also update website URL if it was successfully extracted
                        institution.setWebsiteUrl(websiteUrl);
                        institutionRepository.save(institution);

                        Notification.show("Farben erfolgreich von der Webseite importiert!", 3000,
                                Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    });
                } catch (Exception e) {
                    log.error("Error importing colors from website: {}", websiteUrl, e);
                    ui.access((Command) () -> {
                        progressDialog.close();
                        Notification.show("Fehler beim Importieren der Farben: " + e.getMessage(),
                                5000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    });
                }
            }).start();
        });
    }

    private void updateProgress(UI ui, ProgressBar progressBar, Span statusLabel, double value, String status) {
        ui.access((Command) () -> {
            progressBar.setValue(value);
            statusLabel.setText(status);
        });
    }

    private void reloadPage() {
        UI.getCurrent().getPage().reload();
    }

    private void saveLayoutSettings(Institution institution) {
        try {
            // Validate color formats
            if (!isValidColor(primaryColorField.getValue()) && !primaryColorField.getValue().isBlank()) {
                Notification.show("Ungültiges Format für Primärfarbe. Bitte verwenden Sie #rrggbb", 
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            institution.setLayoutPrimaryColor(primaryColorField.getValue());
            institution.setLayoutSecondaryColor(secondaryColorField.getValue());
            institution.setLayoutBackgroundColor(backgroundColorField.getValue());
            institution.setLayoutTextColor(textColorField.getValue());
            institution.setLayoutAccentColor(accentColorField.getValue());
            institution.setLayoutBorderRadius(borderRadiusField.getValue());
            institution.setLayoutFontFamily(fontFamilyField.getValue());

            institutionRepository.save(institution);

            // Apply settings immediately
            layoutService.applyLayoutSettings();

            Notification.show("Layout-Einstellungen wurden erfolgreich gespeichert!", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error saving layout settings: {}", e.getMessage(), e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean isValidColor(String color) {
        if (color == null || color.isBlank()) {
            return true; // Empty is valid (optional field)
        }
        return color.matches("#[0-9a-fA-F]{6}");
    }
}

