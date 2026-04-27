package de.bbajor.pvs.system.update.ui;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.system.update.ApplicationUpdateService;
import de.bbajor.pvs.system.update.ApplicationUpdateStatus;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "admin/system-update", layout = MainLayout.class)
@PageTitle("System-Update")
@RolesAllowed(AppRoles.SUPER_ADMIN)
public class ApplicationUpdateView extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss 'UTC'", Locale.GERMANY);

    private final ApplicationUpdateService updateService;
    private final Paragraph currentVersion = new Paragraph();
    private final Paragraph latestVersion = new Paragraph();
    private final Paragraph updateStatus = new Paragraph();
    private final Paragraph lastCheck = new Paragraph();
    private final Button refreshButton = new Button("Erneut prüfen");
    private final Button updateButton = new Button("Update installieren");
    private boolean updateAvailable;

    public ApplicationUpdateView(ApplicationUpdateService updateService) {
        this.updateService = updateService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("900px");

        var title = new H2("System-Update");
        var description = new Paragraph(
                "Hier kannst Du prüfen, ob ein neues IVOMPlaner-Release verfügbar ist. "
                        + "Vor einem Update wird serverseitig ein Datenbank-Backup erstellt; Patientendaten bleiben erhalten.");

        refreshButton.addClickListener(event -> refreshStatus());
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.addClickListener(event -> openUpdateDialog());

        add(title, description, currentVersion, latestVersion, updateStatus, lastCheck,
                new HorizontalLayout(refreshButton, updateButton));

        refreshStatus();
    }

    private void refreshStatus() {
        refreshButton.setEnabled(false);
        updateButton.setEnabled(false);
        ApplicationUpdateStatus status = updateService.getStatus();
        renderStatus(status);
        refreshButton.setEnabled(true);
    }

    private void renderStatus(ApplicationUpdateStatus status) {
        currentVersion.setText("Installierte Version: " + valueOrUnknown(status.currentVersion()));
        latestVersion.setText("Verfügbare Version: " + valueOrUnknown(status.latestVersion()));
        lastCheck.setText("Letzte Prüfung: " + TIMESTAMP_FORMATTER.format(status.checkedAt()));

        if (!status.enabled()) {
            updateStatus.setText("App-Update ist deaktiviert: " + status.message());
            updateAvailable = false;
            updateButton.setEnabled(false);
            return;
        }

        if (status.updateAvailable()) {
            updateStatus.setText("Update verfügbar: " + status.message());
            updateAvailable = true;
            updateButton.setEnabled(true);
        } else {
            updateStatus.setText(status.message());
            updateAvailable = false;
            updateButton.setEnabled(false);
        }
    }

    private void openUpdateDialog() {
        var dialog = new Dialog();
        dialog.setHeaderTitle("Update installieren?");

        var content = new VerticalLayout();
        content.setPadding(false);
        content.add(
                new Paragraph("Bitte speichere offene Änderungen vor dem Update."),
                new Paragraph("Die Anwendung erstellt ein Backup, installiert das neue Release und startet danach neu."),
                new Paragraph("Während des Neustarts ist die Anwendung kurz nicht erreichbar."));

        var cancelButton = new Button("Abbrechen", event -> dialog.close());
        var updateNowButton = new Button("Gespeichert - Update starten", event -> {
            dialog.close();
            startUpdate();
        });
        updateNowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        dialog.add(content);
        dialog.getFooter().add(cancelButton, updateNowButton);
        dialog.open();
    }

    private void startUpdate() {
        setBusy(true);
        updateButton.setEnabled(false);
        Notification.show("Update wurde gestartet. Die Anwendung startet gleich neu.", 6000, Position.TOP_CENTER);

        try {
            updateService.startUpdate();
        } catch (IllegalStateException exception) {
            setBusy(false);
            Notification.show(exception.getMessage(), 6000, Position.TOP_CENTER);
            refreshStatus();
            return;
        }

        UI.getCurrent().getPage().executeJs(
                "setTimeout(() => { window.location.reload(); }, 45000);");
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
        updateButton.setEnabled(!busy && updateAvailable);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unbekannt" : value;
    }
}
