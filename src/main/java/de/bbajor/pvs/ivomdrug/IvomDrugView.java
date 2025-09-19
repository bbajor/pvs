package de.bbajor.pvs.ivomdrug;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.ivomdrug.controller.IvomDrugViewPresenter;
import de.bbajor.pvs.ivomplan.dto.IvomDrugDto;
import jakarta.annotation.security.PermitAll;

@Route("ivom-drugs")
@PageTitle("Medikamente")
@Menu(order = 3, icon = "vaadin:drop", title = "Medikamentendatenbank")
@PermitAll
public class IvomDrugView extends Main {

    private final Grid<IvomDrugDto> grid = new Grid<>(IvomDrugDto.class, false);
    private ListDataProvider<IvomDrugDto> dataProvider;

    public IvomDrugView(IvomDrugViewPresenter ivomDrugViewPresenter) {

        // Info für den Anwender
        Paragraph info = new Paragraph("Bitte laden Sie die Arzneimitteldaten als CSV von folgender Seite herunter:");
        Anchor link = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        link.setTarget("_blank");
        Div infoBox = new Div(info, link);
        infoBox.getStyle().set("margin-bottom", "20px");
        add(infoBox);

        List<IvomDrugDto> drugs = ivomDrugViewPresenter.getAll();
        dataProvider = new ListDataProvider<>(drugs);
        grid.setDataProvider(dataProvider);

        // Spalten
        grid.addColumn(IvomDrugDto::getArzneimittelbezeichnung).setHeader("Arzneimittel");
        grid.addColumn(IvomDrugDto::getZulassungsNr).setHeader("Zulassungsnummer");
        grid.addColumn(IvomDrugDto::getZulassungsinhaber).setHeader("Zulassungsinhaber");
        grid.addColumn(IvomDrugDto::getWirkstoffe).setHeader("Wirkstoffe");
        grid.setSizeFull();

        // Filter oben
        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter...");
        filterField.setWidthFull();
        filterField.setClearButtonVisible(true);
        filterField.addValueChangeListener(e -> dataProvider.setFilter(drug -> {
            String term = e.getValue().toLowerCase();
            return drug.getArzneimittelbezeichnung().toLowerCase().contains(term)
                    || (drug.getVertreiber() != null && drug.getVertreiber().toLowerCase().contains(term))
                    || (drug.getWirkstoffe() != null && drug.getWirkstoffe().toLowerCase().contains(term))
                    || (drug.getDescription() != null && drug.getDescription().toLowerCase().contains(term))
                    || (drug.getZulassungsNr() != null && drug.getZulassungsNr().toLowerCase().contains(term));
        }));

        VerticalLayout layout = new VerticalLayout();
        if (isTechUser()) {
            Upload upload = new Upload();
            upload.setAcceptedFileTypes(".csv");
            upload.setMaxFiles(1);
            upload.setDropLabel(new NativeLabel("CSV hier ablegen oder Datei auswählen"));

            upload.setUploadHandler(event -> {
                try {
                    int importedCount = ivomDrugViewPresenter.importCsv(new InputStreamReader(event.getInputStream()));
                    UI ui = UI.getCurrent();
                    if (ui != null) {
                        ui.access(() -> {
                            if (importedCount > 0) {
                                // Create new ListDataProvider with fresh data
                                List<IvomDrugDto> updatedDrugs = ivomDrugViewPresenter.getAll();
                                ListDataProvider<IvomDrugDto> newDataProvider = new ListDataProvider<>(
                                        new ArrayList<>(updatedDrugs));

                                // Set the new DataProvider to the grid
                                grid.setDataProvider(newDataProvider);

                                // Update the reference
                                dataProvider = newDataProvider;

                                Notification.show(
                                        String.format("%d Medikamente erfolgreich importiert", importedCount),
                                        3000,
                                        Notification.Position.MIDDLE);
                            }
                        });
                    }
                } catch (Exception e) {
                    UI.getCurrent().access(() -> {
                        Notification.show(
                                "Fehler beim Import: " + e.getMessage(),
                                3000,
                                Notification.Position.MIDDLE);
                    });
                }
            });
            layout.add(upload);
        }
        layout.add(filterField);
        layout.add(grid);
        layout.setSizeFull();

        add(layout);
        setSizeFull();
    }

    private boolean isTechUser() {
        return true;
        // return
        // SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        // .anyMatch(a -> a.getAuthority().equals(AppRoles.TECH_USER) ||
        // a.getAuthority().equals(AppRoles.ADMIN));
    }
}
