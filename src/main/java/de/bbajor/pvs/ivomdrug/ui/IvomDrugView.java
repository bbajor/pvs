package de.bbajor.pvs.ivomdrug.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.ivomdrug.controller.IvomDrugViewPresenter;
import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import jakarta.annotation.security.PermitAll;

@Route("ivom-drugs")
@PageTitle("Medikamente")
@Menu(order = 3, icon = "vaadin:drop", title = "Medikamentendatenbank")
@PermitAll
public class IvomDrugView extends Main {

    private final TreeGrid<IvomDrugNode> grid = new TreeGrid<>(IvomDrugNode.class, false);
    // Member-Variable für den TreeDataProvider
    private TreeDataProvider<IvomDrugNode> dataProvider = new TreeDataProvider<>(new TreeData<>());

    private UI myUi; // wird in onAttach gesetzt
    private IvomDrugViewPresenter ivomDrugViewPresenter;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.myUi = attachEvent.getUI();
    }

    public IvomDrugView(IvomDrugViewPresenter ivomDrugViewPresenter) {
        this.ivomDrugViewPresenter = ivomDrugViewPresenter;

        // Info für den Anwender
        Paragraph info = new Paragraph("Bitte laden Sie die Arzneimitteldaten als CSV von folgender Seite herunter:");
        Anchor link = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        link.setTarget("_blank");
        Div infoBox = new Div(info, link);
        infoBox.getStyle().set("margin-bottom", "20px");

        // Spalten
        grid.addHierarchyColumn(IvomDrugNode::getLabel)
                .setHeader("Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer");
        grid.addColumn(IvomDrugNode::getZulassungsinhaber).setHeader("Zulassungsinhaber");
        grid.addColumn(IvomDrugNode::getEingangsnummer).setHeader("Eingangsnummer");
        grid.addColumn(IvomDrugNode::isFavourite).setHeader("Favorit");
        grid.setSizeFull();
        grid.addItemDoubleClickListener(event -> {
            IvomDrugNode ivomDrugNode = event.getItem();
            if (ivomDrugNode.getDto() != null) {
                IvomDrugDetailDialog detailDialog = new IvomDrugDetailDialog(ivomDrugViewPresenter,
                        ivomDrugNode.getDto());
                detailDialog.open();
            }
        });
        grid.setDataProvider(dataProvider);

        reloadTree(ivomDrugViewPresenter.getAll());

        // Filter oben
        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter...");
        filterField.setWidthFull();
        filterField.setClearButtonVisible(true);
        filterField.addKeyPressListener(e -> dataProvider.setFilter(drug -> {
            return filterField.getValue() != null ? drug.isContainsSearchTerm(filterField.getValue().toLowerCase())
                    : true;
        }));

        add(new ViewToolbar("Medikamentenkatalog", ViewToolbar.group(infoBox, filterField)));
        if (isTechUser()) {
            add(initUpload());
        }
        add(grid);
        setSizeFull();
    }

    private Upload initUpload() {
        // InMemory handler — liefert byte[] mit den Daten
        UploadHandler inMemory = UploadHandler.inMemory((metadata, bytes) -> {
            // Läuft im Request/Handler-Thread — UI.getCurrent() kann hier null sein.
            try {
                int importedCount;
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    importedCount = ivomDrugViewPresenter.importCsv(new InputStreamReader(in));
                }

                // UI-Update: sichere Ausführung im UI-Thread
                if (myUi != null) {
                    myUi.access(() -> {
                        reloadTree(ivomDrugViewPresenter.getAll()); // setzt neuen provider
                        Notification.show(importedCount + " Medikamente importiert",
                                3000, Notification.Position.MIDDLE);
                    });
                } else if (VaadinSession.getCurrent() != null) {
                    // Fallback: alle UIs in der Session updaten
                    VaadinSession.getCurrent().getUIs().forEach(ui -> ui.access(() -> {
                        reloadTree(ivomDrugViewPresenter.getAll());
                        Notification.show(importedCount + " Medikamente importiert",
                                3000, Notification.Position.MIDDLE);
                    }));
                } else {
                    // letzter Rückfall: nur loggen (damit du siehst, dass Handler lief)
                    System.out.println("Upload finished, but no UI available. imported=" + importedCount);
                }
            } catch (Exception ex) {
                // unbedingt loggen, sonst merkst du nichts
                ex.printStackTrace();
            }
        });

        Upload upload = new Upload(inMemory);
        upload.setMaxFiles(1);
        // optional: Benutzer-Friendly-Listener für Rejected/Failed
        upload.addFileRejectedListener(
                ev -> Notification.show(ev.getErrorMessage(), 4000, Notification.Position.MIDDLE));
        // add upload to UI
        return upload;
    }

    private boolean isTechUser() {
        return true;
        // return
        // SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        // .anyMatch(a -> a.getAuthority().equals(AppRoles.TECH_USER) ||
        // a.getAuthority().equals(AppRoles.ADMIN));
    }

    private TreeData<IvomDrugNode> buildTree(List<IvomDrugDto> drugs) {
        TreeData<IvomDrugNode> newTreeData = new TreeData<>();
        if (drugs != null && !drugs.isEmpty()) {
            for (IvomDrugDto dto : drugs) {

                String zulassungsRegNrOderKennzifferLabel = Optional.ofNullable(dto.getZulassungsRegNrOderKennziffer())
                        .orElse("").trim();
                String arzneimittelBezeichnungLabel = Optional.ofNullable(dto.getArzneimittelbezeichnung()).orElse("")
                        .trim();
                String wirkstoffeLabel = Optional.ofNullable(dto.getWirkstoffe()).orElse("").trim();

                IvomDrugNode zulassungsRegNrOderKennzifferNode = new IvomDrugNode(zulassungsRegNrOderKennzifferLabel,
                        null);
                if (!newTreeData.contains(zulassungsRegNrOderKennzifferNode)) {
                    newTreeData.addItem(null, zulassungsRegNrOderKennzifferNode);
                }

                IvomDrugNode arzneimittelBezeichnungNode = new IvomDrugNode(arzneimittelBezeichnungLabel, null);
                if (!newTreeData.contains(arzneimittelBezeichnungNode)) {
                    newTreeData.addItem(zulassungsRegNrOderKennzifferNode, arzneimittelBezeichnungNode);
                }

                IvomDrugNode wirkstoffeNode = new IvomDrugNode(wirkstoffeLabel, dto);
                newTreeData.addItem(arzneimittelBezeichnungNode, wirkstoffeNode);
            }
        }
        return newTreeData;
    }

    private void reloadTree(List<IvomDrugDto> drugs) {
        TreeData<IvomDrugNode> newTreeData = buildTree(drugs);
        TreeDataProvider<IvomDrugNode> newProvider = new TreeDataProvider<>(newTreeData);
        grid.setDataProvider(newProvider);
        dataProvider = newProvider; // Referenz aktualisieren
    }

}
