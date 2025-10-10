package de.bbajor.pvs.medication.ui;

import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.medication.controller.MedicationViewPresenter;
import de.bbajor.pvs.medication.model.Medication;

public class MedicationDetailDialog extends Dialog {

        private Binder<Medication> binder = new Binder<>();
        private final Consumer<Medication> onSave;

        public MedicationDetailDialog(MedicationViewPresenter presenter, Medication medication,
                        Consumer<Medication> onSave) {
                this.onSave = onSave;

                boolean isReadOnly = true;
                setWidth("1200px");
                setHeight("1000px");
                setCloseOnEsc(true);

                TextField nr = new TextField("Nr");
                nr.setReadOnly(isReadOnly);
                TextField eingangsnummer = new TextField("Eingangsnummer");
                eingangsnummer.setReadOnly(isReadOnly);
                TextField arzneimittelBezeichnung = new TextField("Arzneimittelbezeichnung");
                arzneimittelBezeichnung.setReadOnly(isReadOnly);
                TextField darreichungsForm = new TextField("Darreichungsform");
                darreichungsForm.setReadOnly(isReadOnly);
                TextField zielgruppe = new TextField("Zielgruppe");
                zielgruppe.setReadOnly(isReadOnly);
                TextField anwendungsArt = new TextField("Anwendungsart");
                anwendungsArt.setReadOnly(isReadOnly);
                TextArea anwendungsGebiete = new TextArea("Anwendungsgebiete");
                anwendungsGebiete.setHeight("80px");
                anwendungsGebiete.setMinWidth("200px");
                anwendungsGebiete.setReadOnly(isReadOnly);
                TextField indikationAtc = new TextField("Indikation/ATC");
                indikationAtc.setReadOnly(isReadOnly);
                TextField bescheidDatumZulassung = new TextField("Bescheiddatum der Zulassung");
                bescheidDatumZulassung.setReadOnly(isReadOnly);
                TextField zulassungsStatus = new TextField("Zulassungsstatus");
                zulassungsStatus.setReadOnly(isReadOnly);
                TextField zulassungsRegNrOderKennziffer = new TextField(
                                "Zulassungs-/Reg.-Nr. (AMG 1976), Register-Nr. (AMG 1961) oder Kennziffer");
                zulassungsRegNrOderKennziffer.setReadOnly(isReadOnly);
                TextField verkehrsFaehigkeit = new TextField("Verkehrsfähigkeit");
                verkehrsFaehigkeit.setReadOnly(isReadOnly);
                TextField parallelImportInformationen = new TextField("Parallelimportinformationen");
                parallelImportInformationen.setReadOnly(isReadOnly);
                TextField euVerfahrensNummer = new TextField("EU-Verfahrensnummer");
                euVerfahrensNummer.setReadOnly(isReadOnly);
                TextField zulassungsInhaber = new TextField("Zulassungsinhaber");
                zulassungsInhaber.setReadOnly(isReadOnly);
                TextField herstellerEndFreigabe = new TextField("Hersteller/Endfreigabe");
                herstellerEndFreigabe.setReadOnly(isReadOnly);
                TextField vertreiber = new TextField("Vertreiber");
                vertreiber.setReadOnly(isReadOnly);
                TextField oertlicherVertreter = new TextField("Örtlicher Vertreter");
                oertlicherVertreter.setReadOnly(isReadOnly);
                TextField wirkstoffe = new TextField("Wirkstoffe");
                wirkstoffe.setReadOnly(isReadOnly);
                TextArea packungsGroessenGruppeVerkaufsabgrenzung = new TextArea(
                                "Packungsgrößen-Gruppe/Verkaufsabgrenzung");
                packungsGroessenGruppeVerkaufsabgrenzung.setHeight("80px");
                packungsGroessenGruppeVerkaufsabgrenzung.setReadOnly(isReadOnly);
                TextArea amKlassifikationen = new TextArea("AM-Klassifikationen");
                amKlassifikationen.setHeight("80px");
                amKlassifikationen.setReadOnly(isReadOnly);
                Checkbox favourite = new Checkbox("Favorit");

                FormLayout detailLayout = new FormLayout();
                detailLayout.setMinColumns(3);
                detailLayout.setColumnSpacing("5em");
                detailLayout.setExpandColumns(true);

                detailLayout.add(nr, eingangsnummer, zielgruppe, anwendungsArt, arzneimittelBezeichnung);
                detailLayout.add(amKlassifikationen, 3);
                detailLayout.add(anwendungsGebiete, 3);
                detailLayout.add(darreichungsForm, indikationAtc, zulassungsRegNrOderKennziffer,
                                euVerfahrensNummer, bescheidDatumZulassung, zulassungsStatus,
                                verkehrsFaehigkeit, parallelImportInformationen,
                                zulassungsInhaber, herstellerEndFreigabe, vertreiber, oertlicherVertreter,
                                wirkstoffe, packungsGroessenGruppeVerkaufsabgrenzung);
                detailLayout.add(amKlassifikationen, 3);
                detailLayout.setSizeFull();

                detailLayout.setResponsiveSteps(
                                // Use one column by default
                                new ResponsiveStep("0", 1),
                                // Use two columns, if the layout's width exceeds 320px
                                new ResponsiveStep("320px", 2),
                                // Use three columns, if the layout's width exceeds 500px
                                new ResponsiveStep("500px", 3));

                Scroller scroller = new Scroller(detailLayout);
                add(scroller);

                Button saveButton = new Button("Ok");
                saveButton.addClickListener(event -> {
                        Medication saved = presenter.save(binder.getBean());
                        binder.setBean(saved);
                        if (onSave != null) {
                                onSave.accept(saved);
                        }
                        close();
                });
                Button cancelButton = new Button("Abbrechen");
                cancelButton.addClickListener(event -> {
                        close();
                });

                getFooter().add(favourite, saveButton, cancelButton);

                binder.forField(nr).bind(Medication::getZulassungsNr, Medication::setZulassungsNr);
                binder.forField(eingangsnummer).bind(Medication::getEingangsnummer,
                                Medication::setEingangsnummer);
                binder.forField(arzneimittelBezeichnung).bind(Medication::getArzneimittelbezeichnung,
                                Medication::setArzneimittelbezeichnung);
                binder.forField(darreichungsForm).bind(Medication::getDarreichungsform,
                                Medication::setDarreichungsform);
                binder.forField(zielgruppe).bind(Medication::getZielgruppe, Medication::setZielgruppe);
                binder.forField(anwendungsArt).bind(Medication::getAnwendungsart, Medication::setAnwendungsart);
                binder.forField(anwendungsGebiete).bind(Medication::getAnwendungsgebiete,
                                Medication::setAnwendungsgebiete);
                binder.forField(indikationAtc).bind(Medication::getIndikationAtc, Medication::setIndikationAtc);
                binder.forField(bescheidDatumZulassung).bind(Medication::getBescheiddatumZulassung,
                                Medication::setBescheiddatumZulassung);
                binder.forField(zulassungsStatus).bind(Medication::getZulassungsstatus,
                                Medication::setZulassungsstatus);
                binder.forField(zulassungsRegNrOderKennziffer).bind(Medication::getZulassungsRegNrOderKennziffer,
                                Medication::setZulassungsRegNrOderKennziffer);
                binder.forField(verkehrsFaehigkeit).bind(Medication::getVerkehrsfaehigkeit,
                                Medication::setVerkehrsfaehigkeit);
                binder.forField(parallelImportInformationen).bind(Medication::getParallelimportinformationen,
                                Medication::setParallelimportinformationen);
                binder.forField(euVerfahrensNummer).bind(Medication::getEuVerfahrensnummer,
                                Medication::setEuVerfahrensnummer);
                binder.forField(zulassungsInhaber).bind(Medication::getZulassungsinhaber,
                                Medication::setZulassungsinhaber);
                binder.forField(herstellerEndFreigabe).bind(Medication::getHerstellerEndfreigabe,
                                Medication::setHerstellerEndfreigabe);
                binder.forField(vertreiber).bind(Medication::getVertreiber, Medication::setVertreiber);
                binder.forField(oertlicherVertreter).bind(Medication::getOertlicherVertreter,
                                Medication::setOertlicherVertreter);
                binder.forField(wirkstoffe).bind(Medication::getWirkstoffe, Medication::setWirkstoffe);
                binder.forField(packungsGroessenGruppeVerkaufsabgrenzung).bind(Medication::getPackungsgroessenGruppe,
                                Medication::setPackungsgroessenGruppe);
                binder.forField(amKlassifikationen).bind(Medication::getAmKlassifikationen,
                                Medication::setAmKlassifikationen);
                binder.forField(favourite).bind(Medication::isFavourite, Medication::setFavourite);

                binder.setBean(medication);
        }

}
