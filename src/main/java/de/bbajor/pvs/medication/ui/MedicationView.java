package de.bbajor.pvs.medication.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.medication.controller.MedicationViewPresenter;
import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.PermitAll;

@Route("ivom-drugs")
@PageTitle("Medikamente")
@Menu(order = 3, icon = "vaadin:drop", title = "Medikamentendatenbank")
@PermitAll
public class MedicationView extends Main {

    private final TreeGrid<MedicationNode> grid = new TreeGrid<>();
    // Member-Variable für den TreeDataProvider
    private TreeDataProvider<MedicationNode> dataProvider = new TreeDataProvider<>(new TreeData<>());

    private UI myUi; // wird in onAttach gesetzt
    private MedicationViewPresenter medicationPresenter;
    private final AuthenticationContext authenticationContext;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.myUi = attachEvent.getUI();
    }

    public MedicationView(MedicationViewPresenter medicationPresenter, AuthenticationContext authenticationContext) {
        this.medicationPresenter = medicationPresenter;
        this.authenticationContext = authenticationContext;

        // Info für den Anwender
        Paragraph info = new Paragraph("Bitte laden Sie die Arzneimitteldaten als CSV von folgender Seite herunter:");
        Anchor link = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        link.setTarget("_blank");
        Div infoBox = new Div(info, link);
        infoBox.getStyle().set("margin-bottom", "20px");

        // Spalten
        // Hierarchie-Spalte → zeigt Label
        grid.addHierarchyColumn(MedicationNode::getLabel).setHeader("Bezeichnung").setResizable(true)
                .setWidth("250px");
        grid.addColumn(MedicationNode::getWirkstoffe).setHeader("Wirkstoffe").setResizable(true)
                .setWidth("250px");
        grid.addColumn(MedicationNode::getEingangsnummer).setHeader("Eingangsnummer").setResizable(true);
        grid.addColumn(MedicationNode::getZulassungsinhaber).setHeader("Zulassungsinhaber").setResizable(true);
        grid.addColumn(MedicationNode::getAnwendungsgebiete)
                .setHeader("Anwendungsgebiete")
                .setResizable(true);
        grid.addColumn(new ComponentRenderer<>(dto -> {
            if (Boolean.TRUE.equals(dto.isFavourite())) {
                Icon check = VaadinIcon.CHECK.create();
                check.setColor("green");
                return check;
            } else {
                Icon cross = VaadinIcon.CLOSE.create();
                cross.setColor("red");
                return cross;
            }
        })).setHeader("Favorit");
        grid.setSizeFull();
        grid.setDetailsVisibleOnClick(true);
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addItemDoubleClickListener(event -> {
            MedicationNode medicationNode = event.getItem();
            if (medicationNode.getDto() != null) {
                MedicationDetailDialog detailDialog = new MedicationDetailDialog(medicationPresenter,
                        medicationNode.getDto(), updatedDto -> {
                            MedicationNode nodeToRefresh = findById(grid.getTreeData(), null,
                                    medicationNode.getDto().getId());
                            if (nodeToRefresh != null) {
                                TreeData<MedicationNode> treeData = grid.getTreeData();
                                MedicationNode parent = treeData.getParent(medicationNode);
                                treeData.removeItem(medicationNode);
                                MedicationNode newNode = new MedicationNode(medicationNode.getLabel(), updatedDto);
                                treeData.addItem(parent, newNode);
                                grid.getDataProvider().refreshAll();
                            }

                        });
                detailDialog.open();
            }
        });
        grid.setDataProvider(dataProvider);

        reloadTree(medicationPresenter.getAll());

        // Filter oben
        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter...");
        filterField.setWidthFull();
        filterField.setClearButtonVisible(true);
        filterField.addKeyUpListener(e -> dataProvider.setFilter(drug -> {
            return filterField.getValue() != null ? drug.isContainsSearchTerm(filterField.getValue().toLowerCase())
                    : true;
        }));
        add(new ViewToolbar("Medikamentenkatalog"));
        add(infoBox);
        if (isTechUser()) {
            add(initUpload());
        }
        add(filterField);
        add(grid);

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
    }

    private Upload initUpload() {
        // InMemory handler — liefert byte[] mit den Daten
        UploadHandler inMemory = UploadHandler.inMemory((metadata, bytes) -> {
            // Läuft im Request/Handler-Thread — UI.getCurrent() kann hier null sein.
            try {
                int importedCount;
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    importedCount = medicationPresenter.importCsv(new InputStreamReader(in));
                }

                // UI-Update: sichere Ausführung im UI-Thread
                if (myUi != null) {
                    myUi.access(() -> {
                        reloadTree(medicationPresenter.getAll()); // setzt neuen provider
                        Notification.show(importedCount + " Medikamente importiert",
                                3000, Notification.Position.MIDDLE);
                    });
                } else if (VaadinSession.getCurrent() != null) {
                    // Fallback: alle UIs in der Session updaten
                    VaadinSession.getCurrent().getUIs().forEach(ui -> ui.access(() -> {
                        reloadTree(medicationPresenter.getAll());
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
        return authenticationContext.getGrantedRoles().stream()
                .anyMatch(role -> AppRoles.TECH_USER.equals(role) || AppRoles.ADMIN.equals(role));
    }

    private TreeData<MedicationNode> buildTree(List<MedicationDto> drugs) {
        TreeData<MedicationNode> newTreeData = new TreeData<>();

        Map<String, MedicationNode> bezeichnungNodes = new HashMap<>();

        for (MedicationDto dto : drugs) {

            // nur einmal anlegen
            MedicationNode parent = bezeichnungNodes.computeIfAbsent(dto.getArzneimittelbezeichnung(), key -> {
                MedicationNode node = new MedicationNode(key, null); // Zwischenknoten ohne DTO
                newTreeData.addItem(null, node); // als Root-Item
                return node;
            });

            // Ebene 2: Leaf
            MedicationNode leaf = new MedicationNode(dto.getWirkstoffe(), dto);
            if (!newTreeData.contains(leaf)) {
                newTreeData.addItem(parent, leaf);
            }
        }
        return newTreeData;

    }

    private void reloadTree(List<MedicationDto> drugs) {
        TreeData<MedicationNode> newTreeData = buildTree(new ArrayList<>(drugs));
        TreeDataProvider<MedicationNode> newProvider = new TreeDataProvider<>(newTreeData);
        grid.setDataProvider(newProvider);
        dataProvider = newProvider; // Referenz aktualisieren
    }

    private MedicationNode findById(TreeData<MedicationNode> treeData, MedicationNode parent, Long id) {
        for (MedicationNode child : treeData.getChildren(parent)) {
            if (child.getDto() != null && id.equals(child.getDto().getId())) {
                return child;
            }
            MedicationNode found = findById(treeData, child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
