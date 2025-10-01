package de.bbajor.pvs.medication.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormRow;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.medication.controller.MedicationViewPresenter;
import de.bbajor.pvs.medication.dto.MedicationDto;

public class MedicationDetailDialog extends Dialog {

        private Binder<MedicationDto> binder = new Binder<>();

        public MedicationDetailDialog(MedicationViewPresenter presenter, MedicationDto medication) {

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
                packungsGroessenGruppeVerkaufsabgrenzung.setHeight("160px");
                packungsGroessenGruppeVerkaufsabgrenzung.setReadOnly(isReadOnly);
                TextArea amKlassifikationen = new TextArea("AM-Klassifikationen");
                amKlassifikationen.setHeight("160px");
                amKlassifikationen.setReadOnly(isReadOnly);
                Checkbox favourite = new Checkbox("Favorit");
                FormLayout detailLayout = new FormLayout();
                FormRow firstRow = new FormRow();
                firstRow.add(nr, eingangsnummer, zielgruppe, anwendungsArt);
                FormRow secondRow = new FormRow();
                secondRow.add(arzneimittelBezeichnung, 2);
                secondRow.add(anwendungsArt);
                secondRow.add(darreichungsForm);
                FormRow thirdRow = new FormRow();
                thirdRow.add(anwendungsGebiete, 5);
                FormRow fourthRow = new FormRow();
                fourthRow.add(indikationAtc, zulassungsRegNrOderKennziffer, euVerfahrensNummer);
                detailLayout.add(firstRow, secondRow, thirdRow, fourthRow, bescheidDatumZulassung, zulassungsStatus,
                                verkehrsFaehigkeit, parallelImportInformationen,
                                zulassungsInhaber, herstellerEndFreigabe, vertreiber, oertlicherVertreter,
                                wirkstoffe, packungsGroessenGruppeVerkaufsabgrenzung, amKlassifikationen, favourite);
                detailLayout.setSizeFull();
                Scroller scroller = new Scroller(detailLayout);
                add(scroller);

                Button saveButton = new Button("Ok");
                saveButton.addClickListener(event -> {
                        MedicationDto saved = presenter.save(binder.getBean());
                        binder.setBean(saved);
                        close();
                });
                Button cancelButton = new Button("Abbrechen");
                cancelButton.addClickListener(event -> {
                        close();
                });

                getFooter().add(saveButton, cancelButton);

                binder.forField(nr).bind(MedicationDto::getZulassungsNr, MedicationDto::setZulassungsNr);
                binder.forField(eingangsnummer).bind(MedicationDto::getEingangsnummer,
                                MedicationDto::setEingangsnummer);
                binder.forField(arzneimittelBezeichnung).bind(MedicationDto::getArzneimittelbezeichnung,
                                MedicationDto::setArzneimittelbezeichnung);
                binder.forField(darreichungsForm).bind(MedicationDto::getDarreichungsform,
                                MedicationDto::setDarreichungsform);
                binder.forField(zielgruppe).bind(MedicationDto::getZielgruppe, MedicationDto::setZielgruppe);
                binder.forField(anwendungsArt).bind(MedicationDto::getAnwendungsart, MedicationDto::setAnwendungsart);
                binder.forField(anwendungsGebiete).bind(MedicationDto::getAnwendungsgebiete,
                                MedicationDto::setAnwendungsgebiete);
                binder.forField(indikationAtc).bind(MedicationDto::getIndikationAtc, MedicationDto::setIndikationAtc);
                binder.forField(bescheidDatumZulassung).bind(MedicationDto::getBescheiddatumZulassung,
                                MedicationDto::setBescheiddatumZulassung);
                binder.forField(zulassungsStatus).bind(MedicationDto::getZulassungsstatus,
                                MedicationDto::setZulassungsstatus);
                binder.forField(zulassungsRegNrOderKennziffer).bind(MedicationDto::getZulassungsRegNrOderKennziffer,
                                MedicationDto::setZulassungsRegNrOderKennziffer);
                binder.forField(verkehrsFaehigkeit).bind(MedicationDto::getVerkehrsfaehigkeit,
                                MedicationDto::setVerkehrsfaehigkeit);
                binder.forField(parallelImportInformationen).bind(MedicationDto::getParallelimportinformationen,
                                MedicationDto::setParallelimportinformationen);
                binder.forField(euVerfahrensNummer).bind(MedicationDto::getEuVerfahrensnummer,
                                MedicationDto::setEuVerfahrensnummer);
                binder.forField(zulassungsInhaber).bind(MedicationDto::getZulassungsinhaber,
                                MedicationDto::setZulassungsinhaber);
                binder.forField(herstellerEndFreigabe).bind(MedicationDto::getHerstellerEndfreigabe,
                                MedicationDto::setHerstellerEndfreigabe);
                binder.forField(vertreiber).bind(MedicationDto::getVertreiber, MedicationDto::setVertreiber);
                binder.forField(oertlicherVertreter).bind(MedicationDto::getOertlicherVertreter,
                                MedicationDto::setOertlicherVertreter);
                binder.forField(wirkstoffe).bind(MedicationDto::getWirkstoffe, MedicationDto::setWirkstoffe);
                binder.forField(packungsGroessenGruppeVerkaufsabgrenzung).bind(MedicationDto::getPackungsgroessenGruppe,
                                MedicationDto::setPackungsgroessenGruppe);
                binder.forField(amKlassifikationen).bind(MedicationDto::getAmKlassifikationen,
                                MedicationDto::setAmKlassifikationen);
                binder.forField(favourite).bind(MedicationDto::isFavourite, MedicationDto::setFavourite);

                binder.setBean(medication);
        }

}
