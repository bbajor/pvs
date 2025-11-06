package de.bbajor.pvs.institution.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to apply institution-specific layout settings (colors, fonts, etc.)
 * to the UI by injecting CSS variables.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionLayoutService {

    private final InstitutionRepository institutionRepository;

    /**
     * Applies layout settings from the current institution to the UI.
     * Should be called when the UI is initialized or when settings change.
     */
    public void applyLayoutSettings(UI ui) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return;
        }

        Institution institution = institutionRepository.findById(institutionId).orElse(null);
        if (institution == null) {
            return;
        }

            ui.access(() -> {
                Page page = ui.getPage();
                StringBuilder css = new StringBuilder();

                // Override Lumo CSS variables directly to ensure they take precedence
                if (institution.getLayoutPrimaryColor() != null && !institution.getLayoutPrimaryColor().isBlank()) {
                    css.append("--lumo-primary-color: ").append(institution.getLayoutPrimaryColor()).append("; ");
                    // Also set related Lumo primary color variants
                    css.append("--lumo-primary-color-50pct: ").append(institution.getLayoutPrimaryColor()).append("80; ");
                    css.append("--lumo-primary-color-10pct: ").append(institution.getLayoutPrimaryColor()).append("1a; ");
                }
                if (institution.getLayoutSecondaryColor() != null && !institution.getLayoutSecondaryColor().isBlank()) {
                    css.append("--lumo-contrast-10pct: ").append(institution.getLayoutSecondaryColor()).append("; ");
                }
                if (institution.getLayoutBackgroundColor() != null && !institution.getLayoutBackgroundColor().isBlank()) {
                    css.append("--lumo-base-color: ").append(institution.getLayoutBackgroundColor()).append("; ");
                }
                if (institution.getLayoutTextColor() != null && !institution.getLayoutTextColor().isBlank()) {
                    css.append("--lumo-body-text-color: ").append(institution.getLayoutTextColor()).append("; ");
                    css.append("--lumo-primary-text-color: ").append(institution.getLayoutTextColor()).append("; ");
                }
                if (institution.getLayoutAccentColor() != null && !institution.getLayoutAccentColor().isBlank()) {
                    css.append("--lumo-success-color: ").append(institution.getLayoutAccentColor()).append("; ");
                }
                if (institution.getLayoutBorderRadius() != null && !institution.getLayoutBorderRadius().isBlank()) {
                    css.append("--lumo-border-radius-m: ").append(institution.getLayoutBorderRadius()).append("; ");
                    css.append("--lumo-border-radius: ").append(institution.getLayoutBorderRadius()).append("; ");
                }
                if (institution.getLayoutFontFamily() != null && !institution.getLayoutFontFamily().isBlank()) {
                    css.append("--lumo-font-family: ").append(institution.getLayoutFontFamily()).append("; ");
                }

                if (css.length() > 0) {
                    // Inject CSS directly into :root to override Lumo variables
                    String style = ":root { " + css.toString() + " }";
                    page.executeJs(
                        "const style = document.createElement('style'); " +
                        "style.id = 'institution-layout-override'; " +
                        "style.textContent = $0; " +
                        "document.head.appendChild(style);",
                        style
                    );
                    log.debug("Applied layout settings for institution {}: {}", institutionId, css.toString());
                }
            });
    }

    /**
     * Applies layout settings to the current UI.
     */
    public void applyLayoutSettings() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            applyLayoutSettings(ui);
        }
    }
}

